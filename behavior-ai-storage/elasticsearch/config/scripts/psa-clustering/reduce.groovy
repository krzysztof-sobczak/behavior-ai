// ------ PSA functions

def createPSA = { length1, length2 ->
    psaTable = []
    for (Integer x in (0..seq1len)) {
        psaTable[x] = []
        for (Integer y in (0..seq2len)) {
            if ((x == 0 || y == 0) && !semiglobalMode) {
                penalty = (x == 0) ? -y : -x;
            } else {
                penalty = 0
            }
            psaTable[x][y] = penalty
        }
    }
    return psaTable
}.memoize()

def isSemiglobal = { length1, length2 ->
    // check length difference
    if (length1 >= length2) {
        lengthSimilarityPercent = length2 / length1 * 100
    } else {
        lengthSimilarityPercent = length1 / length2 * 100
    }

    // check for semiglobal mode
    semiglobalMode = (lengthSimilarityPercent < 80)
    return semiglobalMode
}.memoize()

def calculate_move_value = { boolean sequenceMatching, Integer vertical, Integer horizontal, Integer diagonal, Integer x, Integer y, boolean semiglobalMode ->
    verticalGapPenalty = (semiglobalMode && x == (rowCount - 1)) ? 0 : -1
    verticalMove = (y > 0) ? vertical + verticalGapPenalty : -99999
    horizontalGapPenalty = (semiglobalMode && y == (colCount - 1)) ? 0 : -1
    horizontalMove = (x > 0) ? horizontal + horizontalGapPenalty : -99999
    if (x > 0 && y > 0 && sequenceMatching) {
        diagonalScore = 1
        diagonalMove = diagonal + diagonalScore
    } else {
        // should low enough to be ignored
        diagonalScore = -99999
        diagonalMove = diagonalScore
    }

    bestMoveValue = [verticalMove, horizontalMove, diagonalMove].max()

    return bestMoveValue
}.memoize()

def calculate_sequences_score_psa = { ArrayList sequence1, ArrayList sequence2 ->

    // calculate sequence lengths
    seq1len = sequence1.size()
    seq2len = sequence2.size()

//    semiglobalMode = isSemiglobal(seq1len, seq2len)
    semiglobalMode = true

    // create PSA table
    psaTable = createPSA(seq1len, seq2len);

    // shared counters
    colCount = seq1len + 1
    rowCount = seq2len + 1

    // create PSA table pointer
    pointer = [1, 1]
    for (Integer x in (0..seq1len)) {
        for (Integer y in (0..seq2len)) {
//            println("[" + x + "," + y + "]")
            if (x > 0 && y > 0) {
                boolean sequenceMatching = (sequence1[x - 1] == sequence2[y - 1]);
                Integer horizontal = psaTable[x - 1][y];
                Integer vertical = psaTable[x][y - 1];
                Integer diagonal = psaTable[x - 1][y - 1];
                psaTable[x][y] = calculate_move_value(sequenceMatching, vertical, horizontal, diagonal, x, y, semiglobalMode)
            }
        }
    }

    // get optimal move
    optimalMoveValue = psaTable[seq1len][seq2len]

    maxScore = Math.min(seq1len, seq2len)
    scaledScore = Math.round(optimalMoveValue / maxScore * 100)

    return scaledScore
}.memoize()

// ------ PSA functions end

// ------ Clustering functions start

def mergeClosestClusters = { HashMap<String, HashMap<String, String>> _clusters, HashMap<String, HashMap<String, Integer>> _distances ->
    merge = []
    mergeDistance = -99999
    for (c1 in _distances) {
        for (c2 in _distances) {
            distance = _distances[c1.key][c2.key]
            if (distance != 1 && distance > mergeDistance) {
                merge = [c1.key, c2.key]
                mergeDistance = distance
            }
        }
    }
//    println(merge)
//    println(mergeDistance)
    if (mergeDistance < -30) {
        return false
    }

    // perform cluster merging

    for (user in _clusters[merge[1]]) {
        _clusters[merge[0]].add(user)
    }
    _clusters.remove(merge[1])
//    println(_clusters)

    for (d in _distances[merge[0]]) {
//        println(d.key+": comparing "+d.value+" and "+_distances[merge[1]][d.key])
        max = Math.max(d.value, _distances[merge[1]][d.key])
        d.setValue(max)
        _distances[d.key][merge[0]] = max
    }
    for (d in _distances) {
        d.value.remove(merge[1])
    }
    _distances.remove(merge[1])
    return true
}

// ------ Clustering functions end

// ------ MOCKS START
if (!binding.variables.containsKey('_aggs')) {
    _aggs = [
            [
                    id  : "AS1",
                    path: [
                            [
                                    item     : "INBOX",
                                    timestamp: 1
                            ],
                            [
                                    item     : "API_ME",
                                    timestamp: 2
                            ],
                            [
                                    item     : "INBOX",
                                    timestamp: 8
                            ],
                            [
                                    item     : "INBOX",
                                    timestamp: 9
                            ],
                            [
                                    item     : "API_ME",
                                    timestamp: 10
                            ],
                            [
                                    item     : "API_ME",
                                    timestamp: 11
                            ]
                    ]
            ],
            [
                    id  : "AS1",
                    path: [
                            [
                                    item     : "PRODUCT",
                                    timestamp: 4
                            ],
                            [
                                    item     : "PRODUCT",
                                    timestamp: 5
                            ],
                            [
                                    item     : "PRODUCT",
                                    timestamp: 6
                            ]
                    ]
            ]
    ]
}
// ------ MOCKS END

users = [];
for (a in _aggs) {
    if (a != null) {
        users = users + a;
    }
}

// ------ INITIALIZATION START
// make one list of user sessions
mergedUsers = new HashMap<>();
for (u in users) {
    if (mergedUsers.containsKey(u.id)) {
        for (pathItem in u.path) {
            mergedUsers[u.id].path.add(pathItem);
        }
        mergedUsers[u.id].path.sort({ it.timestamp })
    } else {
        mergedUsers[u.id] = u;
    }
};
users = [];
for (u in mergedUsers) {
    path = [];
    for (pathItem in u.value.path) {
        path.add(pathItem.item)
    }
    user = u.value;
    user.path = path;
    users.add(user);
}
_aggs = null;
mergedUsers = null;
// ------ INITIALIZATION END

distanceCalculations = 0;
timePSA = 0;
startDistances = System.currentTimeMillis();
HashMap<String, HashMap<String, Integer>> distances = new HashMap<>()
for (user1 in users) {
    if (user1 != null) {
        if (!distances.containsKey(user1.id)) {
            distances[user1.id] = new HashMap<>()
        }
        for (user2 in users) {
            if (user2 != null) {
                if (!distances[user1.id].containsKey(user2.id)) {
                    if (user1.id == user2.id) {
                        distance = 1
                    } else {
                        startPSA = System.currentTimeMillis();
                        distance = calculate_sequences_score_psa(user1.path, user2.path) - 100
                        timePSA = timePSA + (System.currentTimeMillis() - startPSA);
                    }
                    distances[user1.id][user2.id] = distance
                    if (!distances.containsKey(user2.id)) {
                        distances[user2.id] = new HashMap<>()
                    }
                    distances[user2.id][user1.id] = distance
                    distanceCalculations = distanceCalculations + 1;
                }
            }
        }
    }
}
timeDistances = System.currentTimeMillis() - startDistances;

// perform hierarchical clustering using distance matrix
HashMap<String, HashMap<String, String>> clusters = new HashMap<>()
for (u in users) {
    if (u != null) {
        clusters[u.id] = [u]
    }
}

// keep clustering until merge is possible
while (mergeClosestClusters(clusters, distances)) {
//    println(clusters)
//    println(distances)
}

// find cluster representants
// representant is a centroid of cluster
// in distance matrix of cluster we sum-up each row
// and find user with minimal value

result = [clusters: [], clusters_count: clusters.size(), clusters_users_count: 0, times: [distances: timeDistances, psa: timePSA], counters: [usersSize: users.size(), distanceCalculations: distanceCalculations], debug: []]
for (cluster in clusters) {
    clusterSize = cluster.value.size()
    cluster.value[0]['pathHash'] = cluster.value[0].path.join().bytes.encodeBase64().toString()
    result.clusters.add([size: clusterSize, representants: [
            cluster.value[0]
    ]]);
    result.clusters_users_count += clusterSize
}

//println(result)

return result;