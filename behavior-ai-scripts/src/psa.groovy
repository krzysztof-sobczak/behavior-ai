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
    return true
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

    semiglobalMode = false

    // create PSA table
    psaTable = createPSA(seq1len, seq2len);

    // shared counters
    colCount = seq1len + 1
    rowCount = seq2len + 1

    // create PSA table pointer
    pointer = [1, 1]
    for (Integer x in (0..seq1len)) {
        for (Integer y in (0..seq2len)) {
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
    normalize = seq1len + seq2len
    optimalMoveValue = optimalMoveValue + normalize
    maxScore = maxScore + normalize
    scaledScore = Math.round(optimalMoveValue / maxScore * 100)

    return scaledScore
}.memoize()

start = System.currentTimeMillis();
j = 1000
for (int i in (1..j)) {
    _scorePsa = calculate_sequences_score_psa(sequence1, sequence2)
}
time = System.currentTimeMillis() - start;
println(time)