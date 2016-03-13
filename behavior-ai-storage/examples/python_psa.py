#!/usr/bin/env python
# -*- coding: utf-8 -*

__revision__ = "$Id$"
__docformat__ = 'restructuredtext en'

import os, sys, logging, optparse

#: Glogbal logger instance
LOG = logging.getLogger(__name__)
#: Debug level names as a string
LOG_HELP = ','.join(["%d=%s" % (4-x, logging.getLevelName((x+1)*10)) for x in xrange(5)])
#: Console LOG format
# LOG_FORMAT_CONS = '%(asctime)s %(name)-12s %(levelname)8st%(message)s'
LOG_FORMAT_CONS = '%(message)s'
#: File LOG format
LOG_FORMAT_FILE = '%(asctime)s %(name)s[%(process)d] %(levelname)10s %(message)s'
#: Levels of logging translation (count of -v, log level)
LOGLEVEL_DICT = { 1 : 50, 2:40, 3:20, 4:10, 5:1 }

DEFAULT_VERBOSITY = 0

def sequence_alignment(seq1, seq2):
    # seq1 = "ACACT"
    # seq2 = "ACT"
    LOG.info("Analyzed sequences:")
    LOG.info("  " + seq1)
    LOG.info("  " + seq2)

    # calculate sequence lengths
    seq1len = len(seq1)
    seq2len = len(seq2)

    # check length difference
    if seq1len >= seq2len:
        lengthSimilarityPercent = (float(seq2len) / float(seq1len)) * 100
    else:
        lengthSimilarityPercent = (float(seq1len) / float(seq2len)) * 100

    #LOG.info("Length similarity in percent: %d %%" % lengthSimilarityPercent)

    # check for semiglobal mode
    semiglobalMode = bool(lengthSimilarityPercent < 60)
    if semiglobalMode:
        LOG.info("Performing semiglobal alignment")

    # create PSA table
    psaTable = [[0 for x in range(seq2len+1)] for x in range(seq1len+1)]
    psaTable[0][0] = 0
    for x in range(seq2len):
        y = x + 1
        penalty = 0 if semiglobalMode else -y
        psaTable[0][y] = penalty
    for x in range(seq1len):
        y = x + 1
        penalty = 0 if semiglobalMode else -y
        psaTable[y][0] = penalty
    #LOG.info(psaTable)

    # create PSA table pointer
    pointer = [1, 1]
    for x in range(seq1len+1):
        for y in range(seq2len+1):
            # print [x,y]
            if(x > 0 and y > 0):
                pointer = [x, y]
                bestMove = calculate_move_value(seq1, seq2, psaTable, pointer, semiglobalMode)
                bestMoveValue = bestMove[0]
                psaTable[pointer[0]][pointer[1]] = bestMoveValue
    # LOG.info(psaTable)

    # get optimal move
    optimalMoveValue = psaTable[seq1len][seq2len]
    # LOG.info("Optimal score: %d" % optimalMoveValue)

    pointerX = seq1len
    pointerY = seq2len
    pointer = [pointerX, pointerY]
    alignmentPath = []

    while pointerX != 0 or pointerY != 0:
        bestMove = calculate_move_value(seq1, seq2, psaTable, pointer, semiglobalMode)
        bestMoveType = bestMove[1]
        alignmentPath.append(bestMoveType)
        LOG.info(pointer)
        # LOG.info(bestMoveType)
        pointerX = pointerX + bestMoveType[0]
        pointerY = pointerY + bestMoveType[1]
        pointer = [pointerX, pointerY]
        # LOG.info(pointer)
    # LOG.info(alignmentPath)

    # construct alignment

    # alignment moves were calculated from the end so we have to reverse the order to reproduce steps
    alignmentPath = reversed(alignmentPath)
    seq1Aligned = ''
    seq2Aligned = ''
    gapSymbol = '-'
    seq1Index = 0
    seq2Index = 0
    for move in alignmentPath:
        # diagonal - no gap
        if (move[0] == -1 and move[1] == -1):
            seq1Aligned += seq1[seq1Index]
            seq2Aligned += seq2[seq2Index]
            seq1Index += 1
            seq2Index += 1
        # horizontal - gap in left sequence (second)
        if (move[0] == -1 and move[1] == 0):
            seq1Aligned += seq1[seq1Index]
            seq2Aligned += gapSymbol
            seq1Index += 1
        # vertical - gap in top sequence (first)
        if (move[0] == 0 and move[1] == -1):
            seq1Aligned += gapSymbol
            seq2Aligned += seq2[seq2Index]
            seq2Index += 1
    LOG.info("Sequence aligned with PSA:")
    LOG.info("  " + seq1Aligned)
    LOG.info("  " + seq2Aligned)
    LOG.info("Score: %d" % optimalMoveValue)

def calculate_move_value(seq1, seq2, psaTable, pointer, semiglobalMode):
    pointerX = pointer[0]
    pointerY = pointer[1]
    colCount = len(psaTable[0])
    rowCount = len(psaTable)
    verticalGapPenalty = 0 if semiglobalMode and pointerX == (rowCount - 1) else -1
    verticalMove = psaTable[pointerX][pointerY - 1] + verticalGapPenalty if pointerY > 0 else -99999
    horizontalGapPenalty = 0 if semiglobalMode and pointerY == (colCount - 1) else -1
    horizontalMove = psaTable[pointerX - 1][pointerY] + horizontalGapPenalty if pointerX > 0 else -99999
    if pointerX > 0 and pointerY > 0 and seq1[pointerX - 1] == seq2[pointerY - 1]:
        diagonalScore = 1
        diagonalMove = psaTable[pointerX - 1][pointerY - 1] + diagonalScore
    else:
        # should low enough to be ignored
        diagonalScore = -99999
        diagonalMove = diagonalScore

    # [value, step]
    bestMoveValue = max(verticalMove, horizontalMove, diagonalMove)
    if verticalMove == bestMoveValue:
        bestMoveType = [0, -1]
    elif horizontalMove == bestMoveValue:
        bestMoveType = [-1, 0]
    else:
        bestMoveType = [-1, -1]

    return [bestMoveValue, bestMoveType]

def default_action():
    LOG.info("Usage: app.py sequence_alignment [seq1] [seq2]")

def main():
    """ Main function - parses args and runs action """
    parser = optparse.OptionParser(usage="%prog or type %prog -h (--help) for help", description=__doc__, version="%prog" + __revision__)
    parser.add_option("-v", action="count", dest="verbosity", default = DEFAULT_VERBOSITY, help = "Verbosity. Add more -v to be more verbose (%s) [default: %%default]" % LOG_HELP)
    parser.add_option("-l", "--logfile", dest="logfile",    default = None, help = "Log to file instead off console [default: %default]" )

    (options, args) = parser.parse_args()

    verbosity = LOGLEVEL_DICT.get(int(options.verbosity), DEFAULT_VERBOSITY)

    # Set up logging
    if options.logfile is None:
        logging.basicConfig(level=verbosity, format=LOG_FORMAT_CONS)
    else:
        logfilename = os.path.normpath(options.logfile)
        logging.basicConfig(level=verbosity, format=LOG_FORMAT_FILE, filename=logfilename, filemode='a')
        print >> sys.stderr, "Logging to %s" % logfilename

    # Run actions
    # LOG.info("Starting %s, rev %s from %s using verbosity %s/%s as PID %d", __name__, __revision__, os.path.abspath(__file__), options.verbosity, verbosity, os.getpid())

    if(len(args) < 3):
        default_action()
        return

    action = args[0]
    if action != 'sequence_alignment':
        default_action()
        return


    sequence_alignment(args[1], args[2])

    # LOG.info("Exited %s, rev %s from %s using verbosity %s/%s  as PID %d", __name__, __revision__, os.path.abspath(__file__), options.verbosity, verbosity, os.getpid())

if __name__ == "__main__":
    main()