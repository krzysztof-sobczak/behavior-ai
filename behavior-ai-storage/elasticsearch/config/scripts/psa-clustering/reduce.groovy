// make one list of user sessions
//users = []; for (a in _aggs) {
//    users = users + a
//};

users = [
        [
                id  : "AS1",
                path: ["INBOX", "API_ME", "INBOX", "INBOX", "API_ME", "API_ME"]
        ],
        [
                id  : "AS2",
                path: ["INBOX", "API_ME", "INBOX", "API_ME", "PRODUCT", "INBOX"]
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

println(distances)

// perform hierarchical clustering using distance matrix

merge = []
mergeDistance = -99999
for(c1 in distances) {
    for (c2 in distances) {
        distance = distances[c1.key][c2.key]
        if(distance != 1 && distance > mergeDistance)
        {
            merge = [c1.key, c2.key]
            mergeDistance = distance
        }
    }
}
println(merge)
println(mergeDistance)
//TODO: perform cluster merging

// find cluster representants
// representant is a centroid of cluster
// in distance matrix of cluster we sum-up each row
// and find user with minimal value

clusters = [];
// mocked clusters
clusters.add([size: 20, representant: users[0]]);
clusters.add([size: 12, representant: users[1]]);
clusters.add([size: 7, representant: users[2]]);
return clusters;