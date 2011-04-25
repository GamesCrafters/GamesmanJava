#!/usr/bin/env python2.6

from __future__ import with_statement
import sys
import os
import time

sys.path.append(os.path.join(os.getcwd(),os.path.pardir,os.path.pardir,
                             'GamesmanWeb','PythonPuzzles'))
from DatabaseHelper import *

def makeLEInt(num):
    s = ''
    for i in range(4):
        s = chr(num&0xff) + s
        num >>= 8;
    return s

readFile = sys.argv[1]
readDB = OpenDB(readFile)
puzz = readDB.getPuzzle(**readDB.header['options'])
print >>sys.stderr, readDB.header

readDB.fp.seek(readDB.startpos)
rawData = bytearray(readDB.fp.read(-1))

# Modify rawData
numBytesPer = (sum(x[1] for x in readDB.header["fields"])+7)/8
if numBytesPer != 1:
    print >>sys.stderr, "Warning: Input uses more than one byte per record."
if len(readDB.header["fields"]) == 1:
    print >>sys.stderr, len(rawData)/numBytesPer
    s = int(time.time())
    for x in xrange(0, len(rawData), numBytesPer):
        sy = int(time.time())
        if sy > s:
            s = sy
            print >>sys.stderr, x/numBytesPer
        if rawData[x] == 0xff:
            rawData[x] = 0
        else:
            rawData[x/numBytesPer] = rawData[x] + 1
elif len(readDB.header["fields"]) == 2:
    print >>sys.stderr, readDB.header["fields"]
    remoteBits = readDB.header["fields"][0][1]
    assert readDB.header["fields"][0][0] == "remoteness"
    assert readDB.header["fields"][1][0] == "score"
    maxRemoteness = puzz.maxRemoteness
    print >>sys.stderr, len(rawData)/numBytesPer
    s = int(time.time())
    for x in xrange(0, puzz.maxhash() * numBytesPer, numBytesPer):
        sy = int(time.time())
        if sy > s:
            s = sy
            print >>sys.stderr, "Time", x/numBytesPer
        instance = puzz.unhash(x/numBytesPer)
        data = int(rawData[x])
        for i in range(1, numBytesPer):
            data = data * 256 + rawData[x + i]
        #print >>sys.stderr, data
        # Hack for triangular peg solitaire's starting position
        #if hash(instance) == hash(puzz) and rawData[hash(puzz._generate_start())] != 0:
        #    rawData[x] = rawData[hash(puzz._generate_start())]

        if data == 0xff or (data == 0 and
                                  not (instance.is_a_solution() or instance.is_deadend())):
            #print >>sys.stderr, "ignore", instance.board
            rawData[x/numBytesPer] = 0
        else:
            remote = int(data) & ((1<<remoteBits)-1)
            #score = int(data) >> remoteBits
            score = sum(sum(r) for r in instance.board) - remote
            #print >>sys.stderr, remote, score, instance.board,
            newData = remote + score * maxRemoteness + 1
            #print >>sys.stderr, newData
            #if score != 1:
            #    newData *= 2
            rawData[x/numBytesPer] = newData
else:
    raise NotImplementedError("More than two fields used in database!")

sys.stdout.write('\x00' * 12)
sys.stdout.write(makeLEInt(len(rawData)/numBytesPer))
sys.stdout.write('\xff\x00')
configLines = ["gamesman.game.%s=%s"%(a,b) for (a,b) in readDB.header['options'].iteritems()]
configLines.append("record.fields=VALUE," + ",".join(x[0].upper() for x in readDB.header["fields"]))
configLines.append("gamesman.game=PythonPuzzle.py")
configLines.append("pythonpuzzle=" + readDB.header["puzzle"])
configLines.append("gamesman.database=edu.berkeley.gamesman.database.FileDatabase")
configLines.append("gamesman.hasher=edu.berkeley.gamesman.hasher.NullHasher")

timeStr = time.strftime("%a %b %d %H:%M:%S %Z %Y", time.localtime(os.stat(readFile)[8]))
headerString="J#\n#" + timeStr + "\n" + "\n".join(configLines) + "\n"
sys.stdout.write(makeLEInt(len(headerString)))
sys.stdout.write(headerString)
toWrite = str(rawData)
if len(rawData) != puzz.maxhash():
    print >>sys.stderr, "Truncating to "+str(puzz.maxhash())+" instead of "+str(len(rawData))
    toWrite = toWrite[:puzz.maxhash()]
sys.stdout.write(toWrite)
#print >>sys.stderr, repr(rawData)

