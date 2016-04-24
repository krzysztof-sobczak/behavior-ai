import groovy.transform.Memoized
//import static groovyx.gpars.GParsPool.withPool

// ------ MOCKS START
if (!binding.variables.containsKey('path_limit')) {
    path_limit = 10
}
if (!binding.variables.containsKey('shard_size')) {
    shard_size = 30
}
if (!binding.variables.containsKey('treshold')) {
    treshold = 69
}
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
            ],
            [
                    id  : "AS2",
                    path: [
                            [
                                    item     : "INBOX",
                                    timestamp: 1
                            ],
                            [
                                    item     : "API_ME",
                                    timestamp: 2
                            ],
                    ]
            ],
            [
                    id  : "AS3",
                    path: [
                            [
                                    item     : "CATEGORY",
                                    timestamp: 1
                            ],
                            [
                                    item     : "CATEGORY",
                                    timestamp: 2
                            ],
                    ]
            ],
            [
                    id  : "AS4",
                    path: [
                            [
                                    item     : "INBOX",
                                    timestamp: 1
                            ],
                            [
                                    item     : "API_ME",
                                    timestamp: 2
                            ],
                    ]
            ],
            [
                    id  : "AS5",
                    path: [
                            [
                                    item     : "INBOX",
                                    timestamp: 1
                            ],
                            [
                                    item     : "API_ME",
                                    timestamp: 2
                            ],
                    ]
            ],
            [
                    id  : "AS6",
                    path: [
                            [
                                    item     : "INBOX",
                                    timestamp: 1
                            ],
                            [
                                    item     : "API_ME",
                                    timestamp: 2
                            ],
                    ]
            ],
            [
                    id  : "AS7",
                    path: [
                            [
                                    item     : "INBOX",
                                    timestamp: 1
                            ],
                            [
                                    item     : "API_ME",
                                    timestamp: 2
                            ],
                    ]
            ],
            [
                    id  : "AS8",
                    path: [
                            [
                                    item     : "INBOX",
                                    timestamp: 1
                            ],
                            [
                                    item     : "API_ME",
                                    timestamp: 2
                            ],
                    ]
            ],
            [
                    id  : "AS9",
                    path: [
                            [
                                    item     : "INBOX",
                                    timestamp: 1
                            ],
                            [
                                    item     : "API_ME",
                                    timestamp: 2
                            ],
                    ]
            ],
            [
                    id  : "AS10",
                    path: [
                            [
                                    item     : "INBOX",
                                    timestamp: 1
                            ],
                            [
                                    item     : "API_ME",
                                    timestamp: 2
                            ],
                    ]
            ],
            [
                    id  : "AS11",
                    path: [
                            [
                                    item     : "INBOX",
                                    timestamp: 1
                            ],
                            [
                                    item     : "API_ME",
                                    timestamp: 2
                            ],
                    ]
            ],
            [
                    id  : "AS12",
                    path: [
                            [
                                    item     : "INBOX",
                                    timestamp: 1
                            ],
                            [
                                    item     : "API_ME",
                                    timestamp: 2
                            ],
                    ]
            ],
            [
                    id  : "AS13",
                    path: [
                            [
                                    item     : "INBOX",
                                    timestamp: 1
                            ],
                            [
                                    item     : "API_ME",
                                    timestamp: 2
                            ],
                    ]
            ],
            [
                    id  : "AS14",
                    path: [
                            [
                                    item     : "INBOX",
                                    timestamp: 1
                            ],
                            [
                                    item     : "API_ME",
                                    timestamp: 2
                            ],
                    ]
            ],
            [
                    id  : "AS15",
                    path: [
                            [
                                    item     : "INBOX",
                                    timestamp: 1
                            ],
                            [
                                    item     : "API_ME",
                                    timestamp: 2
                            ],
                    ]
            ],
            [
                    id  : "AS16",
                    path: [
                            [
                                    item     : "INBOX",
                                    timestamp: 1
                            ],
                            [
                                    item     : "API_ME",
                                    timestamp: 2
                            ],
                    ]
            ]
    ]
}
// ------ MOCKS END

class PSA {

    @Memoized
    static Integer[][] createPSA(length1, length2, semiglobalMode) {
        def psaTable = []
        def penalty
        for (Integer x in (0..length1)) {
            psaTable[x] = []
            for (Integer y in (0..length2)) {
                if ((x == 0 || y == 0) && !semiglobalMode) {
                    penalty = (x == 0) ? -y : -x;
                } else {
                    penalty = 0
                }
                psaTable[x][y] = penalty
            }
        }
        return psaTable
    }

    @Memoized
    boolean isSemiglobal(length1, length2) {
        // check length difference
        if (length1 >= length2) {
            def lengthSimilarityPercent = length2 / length1 * 100
        } else {
            def lengthSimilarityPercent = length1 / length2 * 100
        }

        // check for semiglobal mode
        def semiglobalMode = (lengthSimilarityPercent < 80)
        return semiglobalMode
    }

    @Memoized
    static int calculateMoveValue(boolean sequenceMatching, Integer vertical, Integer horizontal, Integer diagonal, Integer x, Integer y, boolean lastRow, boolean lastColumn, boolean semiglobalMode) {
        def verticalGapPenalty = (semiglobalMode && lastRow) ? 0 : -1
        def verticalMove = (y > 0) ? vertical + verticalGapPenalty : -99999
        def horizontalGapPenalty = (semiglobalMode && lastColumn) ? 0 : -1
        def horizontalMove = (x > 0) ? horizontal + horizontalGapPenalty : -99999
        def diagonalScore
        def diagonalMove
        if (x > 0 && y > 0 && sequenceMatching) {
            diagonalScore = 1
            diagonalMove = diagonal + diagonalScore
        } else {
            // should low enough to be ignored
            diagonalScore = -99999
            diagonalMove = diagonalScore
        }

        def bestMoveValue = [verticalMove, horizontalMove, diagonalMove].max()

        return bestMoveValue
    }

    @Memoized
    static int calculatePsaScore(ArrayList sequence1, ArrayList sequence2) {

        // calculate sequence lengths
        def seq1len = sequence1.size()
        def seq2len = sequence2.size()

//    semiglobalMode = isSemiglobal(seq1len, seq2len)
        def semiglobalMode = true

        // create PSA table
        def psaTable = createPSA(seq1len, seq2len, semiglobalMode);

        // shared counters
        def colCount = seq1len + 1
        def rowCount = seq2len + 1

        // go through PSA table
        for (Integer x in (1..seq1len)) {
            for (Integer y in (1..seq2len)) {
//            println("[" + x + "," + y + "]")
                def boolean sequenceMatching = (sequence1[x - 1] == sequence2[y - 1]);
                def Integer horizontal = psaTable[x - 1][y];
                def Integer vertical = psaTable[x][y - 1];
                def Integer diagonal = psaTable[x - 1][y - 1];
                def boolean lastRow = (x == (rowCount - 1));
                def boolean lastColumn = (y == (colCount - 1));
                psaTable[x][y] = calculateMoveValue(sequenceMatching, vertical, horizontal, diagonal, x, y, lastRow, lastColumn, semiglobalMode)
            }
        }

        // get optimal move
        def optimalMoveValue = psaTable[seq1len][seq2len]

        def maxScore = Math.min(seq1len, seq2len)
        def scaledScore = Math.round(optimalMoveValue / maxScore * 100)

        return scaledScore
    }
}

class Cluster {
    def users
    def representant

    public Cluster(users = []) {
        this.users = users;
        this.updateRepresentant()
    }

    public void merge(Cluster cluster) {
        this.users = this.users + cluster.users;
        this.updateRepresentant()
    }

    public int getDistance(Cluster cluster) {
        return PSA.calculatePsaScore(this.representant.path, cluster.representant.path);
    }

    private void updateRepresentant() {
        def representant = this.users.first()
        def representantDistanceSum = Integer.MAX_VALUE;
        for (user1 in this.users) {
            def userDistanceSum = 0;
            for (user2 in this.users) {
                userDistanceSum = userDistanceSum + PSA.calculatePsaScore(user1.path, user2.path);
            }
            if (userDistanceSum > representantDistanceSum) {
                representant = user1;
                representantDistanceSum = userDistanceSum;
            }
        }
        this.representant = representant;
    }

}

// ------ INITIALIZATION START
initTime = System.currentTimeMillis()
users = [];
for (a in _aggs) {
    if (a != null) {
        users = users + a;
    }
}
_aggs = null;

// make one list of user sessions
mergedUserSessions = new HashMap<>();
for (u in users) {
    if (mergedUserSessions.containsKey(u.id)) {
        for (pathItem in u.path) {
            mergedUserSessions[u.id].path.add(pathItem);
        }
        mergedUserSessions[u.id].path.sort({ it.timestamp })
    } else {
        mergedUserSessions[u.id] = u;
    }
};
def clusters = [];
for (u in mergedUserSessions) {
    path = [];
    for (pathItem in u.value.path) {
        path.add(pathItem.item)
        if(path.size() >= path_limit) {
            break;
        }
    }
    user = u.value;
    user.path = path;
    clusters.add(new Cluster([user]));
}
mergedUserSessions = null;
initTime = System.currentTimeMillis() - initTime
mergingTime = 0;
def mergeClusters = { _clusters, _treshold ->
    def boolean mergePossible = true;
    while(mergePossible) {
        def int beforeMergingSize = _clusters.size();
        _clusters = _clusters.inject([]) { prev, next ->
            for (cluster in prev) {
                startMerging = System.currentTimeMillis();
                if (cluster.getDistance(next) > _treshold) {
                    cluster.merge(next);
                    return prev;
                }
                mergingTime = mergingTime + (System.currentTimeMillis() - startMerging)
            }
            prev.add(next);
            return prev;
        };
        def newSize = _clusters.size();
        mergePossible = (boolean)(newSize > 1 && newSize < beforeMergingSize)
    }
    return _clusters;
}
shardingTime = 0;
def mergeClustersWithSharding = { _clusters, _shardSize, _treshold ->
    def boolean mergePossible = true;
    while (mergePossible) {
        startSharding = System.currentTimeMillis();
        // make shards
        shards = _clusters.inject([[]]) { prev, next ->
            lastShard = prev.last();
            if (lastShard.size() < _shardSize) {
                lastShard.add(next);
            } else {
                prev.add([next]);
            }
            return prev;
        };
        shardingTime = shardingTime + (System.currentTimeMillis() - startSharding);
//        println(shards)
//        def results = withPool(4) {

        // reduce shards (possible parallel)
        shards = shards.collect { it = mergeClusters(it, _treshold) }
        startSharding = System.currentTimeMillis();
        // combine shards into clusters (possible parallel)
        def results = shards.inject { prev, next ->
            for (cluster in next) {
                prev.add(cluster);
            }
            return prev;
        }
        shardingTime = shardingTime + (System.currentTimeMillis() - startSharding);
//        println(results)
        mergePossible = (boolean) (results.size() < _clusters.size());
        _clusters = results;
    }
    return _clusters;
}

//println(clusters);
initialSize = clusters.size();
mergingWithShardingTime = System.currentTimeMillis()
clusters = mergeClustersWithSharding(clusters, shard_size, treshold);
mergingWithShardingTime = (System.currentTimeMillis() - mergingWithShardingTime)

result = [clusters: [], clusters_count: clusters.size(), clusters_users_count: 0, debug: [initial: initialSize, initTime: initTime, mergingWithShardingTime: mergingWithShardingTime, shardingTime: shardingTime, mergingTime: mergingTime]]
for (cluster in clusters) {
    clusterSize = cluster.users.size()
    cluster.representant['pathHash'] = cluster.representant.path.join().bytes.encodeBase64().toString()
    result.clusters.add([size: clusterSize, representants: [
            cluster.representant
    ]]);
    result.clusters_users_count += clusterSize
}
//println(result)
return result;