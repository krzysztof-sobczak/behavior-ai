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

    def calculate_move_value = { ArrayList seq1, ArrayList seq2, ArrayList<ArrayList<Integer>> psaTable, ArrayList<Integer> pointer, boolean semiglobalMode ->
        pointerX = pointer[0]
        pointerY = pointer[1]
        colCount = psaTable[0].size()
        rowCount = psaTable.size()
        verticalGapPenalty = (semiglobalMode && pointerX == (rowCount - 1)) ? 0 : -1
        verticalMove = (pointerY > 0) ? psaTable[pointerX][pointerY - 1] + verticalGapPenalty : -99999
        horizontalGapPenalty = (semiglobalMode && pointerY == (colCount - 1)) ? 0 : -1
        horizontalMove = (pointerX > 0) ? psaTable[pointerX - 1][pointerY] + horizontalGapPenalty : -99999
        if(pointerX > 0 && pointerY > 0 && seq1[pointerX - 1] == seq2[pointerY - 1]) {
            diagonalScore = 1
            diagonalMove = psaTable[pointerX - 1][pointerY - 1] + diagonalScore
        }
        else {
            // should low enough to be ignored
            diagonalScore = -99999
            diagonalMove = diagonalScore
        }

        // [value, step]
        bestMoveValue =[verticalMove, horizontalMove, diagonalMove].max()
        if(verticalMove == bestMoveValue) {
            bestMoveType = [0, -1]
        }
        else {
            if(horizontalMove == bestMoveValue) {
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

_scorePsa = calculate_sequences_score_psa(sequence1, sequence2)