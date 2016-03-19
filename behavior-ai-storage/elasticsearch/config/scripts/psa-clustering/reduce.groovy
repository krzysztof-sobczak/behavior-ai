if (!binding.variables.containsKey('_aggs')) {
    _aggs = [
            [
                    id  : "AS1",
                    path: ["INBOX", "API_ME", "INBOX", "INBOX", "API_ME", "API_ME"]
            ],
            [
                    id  : "AS3",
                    path: ["PRODUCT", "API_ME", "PRODUCT", "INBOX", "PRODUCT", "API_ME", "PRODUCT", "INBOX"]
            ],
            [
                    id  : "AS4",
                    path: ["INBOX", "API_ME", "API_ME", "INBOX", "API_ME", "API_ME"]
            ],
            [
                    id  : "AS2",
                    path: ["INBOX", "API_ME", "INBOX", "API_ME", "PRODUCT", "INBOX"]
            ],
            [
                    id  : "AS5",
                    path: ["INBOX", "API_ME", "PRODUCT", "INBOX", "PRODUCT", "INBOX", "PRODUCT", "INBOX"]
            ],
            [
                    id  : "AS6",
                    path: ["INBOX", "API_ME", "INBOX", "API_ME"]
            ],
            [
                    id  : "AS7",
                    path: ["PRODUCT", "API_ME", "INBOX", "INBOX"]
            ]
    ]
}

// make one list of user sessions
users = []; for (a in _aggs) {
    users = users + a
};

// create distance matrix
// using PSA algorithm on user.path

def calculate_sequences_score_psa = { ArrayList sequence1, ArrayList sequence2 ->

    // calculate sequence lengths
    seq1len = sequence1.size()
    seq2len = sequence2.size()

    // check length difference
    if (seq1len >= seq2len) {
        lengthSimilarityPercent = seq2len / seq1len * 100
    } else {
        lengthSimilarityPercent = seq1len / seq2len * 100
    }

    // check for semiglobal mode
    semiglobalMode = (lengthSimilarityPercent < 80)
    if (semiglobalMode) {
//        println("Performing semiglobal alignment")
    }

    // create PSA table
    psaTable = []
    (seq1len + 1).times { y ->
        psaTable[y] = []
        (seq2len + 1).times { x ->
            if ((y == 0 || x == 0) && !semiglobalMode) {
                penalty = (y == 0) ? -x : -y;
            } else {
                penalty = 0
            }
            psaTable[y][x] = penalty
        }
    }
//    println(psaTable)

    def calculate_move_value = { ArrayList seq1, ArrayList seq2, ArrayList<ArrayList<Integer>> psaTable, ArrayList<Integer> pointer, Boolean semiglobalMode ->
        pointerX = pointer[0]
        pointerY = pointer[1]
        colCount = psaTable[0].size()
        rowCount = psaTable.size()
        verticalGapPenalty = (semiglobalMode && pointerX == (rowCount - 1)) ? 0 : -1
        verticalMove = (pointerY > 0) ? psaTable[pointerX][pointerY - 1] + verticalGapPenalty : -99999
        horizontalGapPenalty = (semiglobalMode && pointerY == (colCount - 1)) ? 0 : -1
        horizontalMove = (pointerX > 0) ? psaTable[pointerX - 1][pointerY] + horizontalGapPenalty : -99999
        if (pointerX > 0 && pointerY > 0 && seq1[pointerX - 1] == seq2[pointerY - 1]) {
            diagonalScore = 1
            diagonalMove = psaTable[pointerX - 1][pointerY - 1] + diagonalScore
        } else {
            // should low enough to be ignored
            diagonalScore = -99999
            diagonalMove = diagonalScore
        }

        // [value, step]
        bestMoveValue = [verticalMove, horizontalMove, diagonalMove].max()
        if (verticalMove == bestMoveValue) {
            bestMoveType = [0, -1]
        } else {
            if (horizontalMove == bestMoveValue) {
                bestMoveType = [-1, 0]
            } else {
                bestMoveType = [-1, -1]
            }
        }

        return [bestMoveValue, bestMoveType]
    }

    // create PSA table pointer
    pointer = [1, 1]
    (seq1len + 1).times { x ->
        (seq2len + 1).times { y ->
//            println("[" + x + "," + y + "]")
            if (x > 0 && y > 0) {
                pointer = [x, y]
                bestMove = calculate_move_value(sequence1, sequence2, psaTable, pointer, semiglobalMode)
                bestMoveValue = bestMove[0]
                psaTable[pointer[0]][pointer[1]] = bestMoveValue
            }
        }
    }

//    println(psaTable)

    // get optimal move
    optimalMoveValue = psaTable[seq1len][seq2len]

    maxScore = Math.max(seq1len, seq2len)
    scaledScore = Math.round(optimalMoveValue / maxScore * 100)

    return scaledScore
}


HashMap<String, HashMap<String, Integer>> distances = new HashMap<>()
for (user1 in users) {
    distances[user1.id] = new HashMap<>()
    for (user2 in users) {
        distance = calculate_sequences_score_psa(user1.path, user2.path) - 100
        distance = (user1.id == user2.id) ? 1 : distance
        distances[user1.id][user2.id] = distance
    }
}

// perform hierarchical clustering using distance matrix
HashMap<String, HashMap<String, String>> clusters = new HashMap<>()
for (u in users) {
    clusters[u.id] = [u]
}

def mergeClosestClusters = { HashMap<String, HashMap<String, String>> _clusters, HashMap<String, HashMap<String, Integer>> _distances ->
    merge = []
    mergeDistance = -99999
    for (c1 in distances) {
        for (c2 in distances) {
            distance = distances[c1.key][c2.key]
            if (distance != 1 && distance > mergeDistance) {
                merge = [c1.key, c2.key]
                mergeDistance = distance
            }
        }
    }
//    println(merge)
//    println(mergeDistance)
    if (mergeDistance < -50) {
        return false
    }

    // perform cluster merging

    for (user in clusters[merge[1]]) {
        clusters[merge[0]].add(user)
    }
    clusters.remove(merge[1])
//    println(clusters)

    for (d in distances[merge[0]]) {
//        println(d.key+": comparing "+d.value+" and "+distances[merge[1]][d.key])
        max = Math.max(d.value, distances[merge[1]][d.key])
        d.setValue(max)
        distances[d.key][merge[0]] = max
    }
    for (d in distances) {
        d.value.remove(merge[1])
    }
    distances.remove(merge[1])
    return true
}

while (mergeClosestClusters(clusters, distances)) {
//    println(clusters)
//    println(distances)
}

// find cluster representants
// representant is a centroid of cluster
// in distance matrix of cluster we sum-up each row
// and find user with minimal value

result = [clusters: [], clusters_count: clusters.size(), clusters_users_count: 0]
for (cluster in clusters) {
    clusterSize = cluster.value.size()
    result.clusters.add([size: clusterSize, representants: [
            cluster.value[0]]
    ]);
    result.clusters_users_count += clusterSize
}

//println(result)

return result;