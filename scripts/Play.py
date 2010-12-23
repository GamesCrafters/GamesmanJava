from edu.berkeley.gamesman.core import Configuration
from edu.berkeley.gamesman.core import Record
from edu.berkeley.gamesman.util import Util
from java.util import Properties
import sys,os

"""
To use this file, do the following:
    import Play
    state = Play.play("jobs/something.job")
    print state
Then you can play with state by simply adding a move string to it
"""
class GameState:
    """Nice wrapper for games and puzzles"""
    
    def __init__(self, game, state, db):
        self.game = game
        self.state = state
        self.db = db
        
    def __str__(self):
        buf = str(self.hash())
        buf += "( " + self.game.stateToString(self.state) + " )"
        buf += self.game.displayState(self.state)
        if self.db is not None:
            rec = self.db.getRecord(self.game.stateToHash(self.state))
            buf += rec.toString()
        buf += "\n"
        return buf

    def hash(self):
        return self.game.stateToHash(self.state)
    
    def unhash(self, hash):
        return GameState(self.game, self.game.hashToState(hash), self.db)
    
    def moves(self):
        s = []
        for c in self.game.validMoves(self.state):
            s += [(c.car,GameState(self.game, c.cdr, self.db))]
        return s
    
    def printMoves(self):
        i = 0
        for c in self.moves():
            print '+'+str(i)+': "'+str(c[0])+'"',
            if self.db is not None:
                rec = self.db.getRecord(self.game.stateToHash(c[1].state))
                print rec.toString(),
                #v = rec.get()
                #remoteness = rec.get(RecordFields.REMOTENESS)
                #if v is not None:
                #    print v.previousMovesValue().toString(), "(", remoteness, ")",
            print ' =>',
            print c[1]
            i += 1
    
    def __add__(self, move):
        if type(move) == str and move[0]=='+':
            move = int(move[1:])
        if type(move) == int:
            mvs = self.moves()
            if move >= 0 and move < len(mvs):
                return mvs[move][1]
            else:
                print "Invalid move number: " + str(move)
                return self
        nextState = self.game.doMove(self.state, move)
        if nextState == None:
            print "Invalid move: " + move
            return self
        gs = GameState(self.game, nextState, self.db)
        #print gs
        return gs

    def __getitem__(self, index):
        return self.moves()[index]

def play(jobFile):
    for possible_path in sys.path:
        p = possible_path + "/" + jobFile + ".job"
        if os.path.exists(p):
            jobFile = p
            break
    conf = Configuration(jobFile)
    
    response = raw_input("Would you like to load the database? (Y/n): ")
    if response.lower() != "n":
        db = Util.typedInstantiate("edu.berkeley.gamesman.database." + conf.getProperty("gamesman.database"))
        db.initialize(conf.getProperty("gamesman.db.uri"),None)
        conf = db.getConfiguration()
        game = conf.getGame()
        
    else:
        db = None
        gameClass = Util.typedForName("edu.berkeley.gamesman.game." + conf.getProperty("gamesman.game"))
        #java's newInstance() expects an array
        game = gameClass.getConstructors()[0].newInstance([conf])
    

    gs = GameState(game, game.startingPositions()[0], db)
    
    global globalState
    globalState = gs
    
    print gs
    return gs

def load(state):
    global globalState
    globalState.state = globalState.game.stringToState(state)
    print globalState
    return globalState

def m(mv):
    move(mv)

def move(m):
    global globalState
    globalState += m
    print globalState

def moves(s = None):
    if s is None:
        global globalState
        s = globalState
    s.printMoves()

def curState():
    global globalState
    return globalState.state

def getConf():
    global conf
    return conf

# Pass in an array of length 1 to allow assignment
def moveloop(stateRef=None):
    if not stateRef:
        global globalState
        stateRef = [globalState]
    if type(stateRef) is not list:
        stateRef = [stateRef]
    from edu.berkeley.gamesman.core import PrimitiveValue
    while stateRef[0].game.primitiveValue(stateRef[0].state) is PrimitiveValue.UNDECIDED:
        print "State is now", stateRef[0]
        print "Avaliable moves:"
        stateRef[0].printMoves()
        m = raw_input("Which move to take? ")
        if len(m) == 0:
            print "Interrupt!"
            print "Current state is: ", stateRef[0]
            return
        stateRef[0] += m
    print "Game is over! Final state is:", stateRef[0]

def playFromState(state):
    arr = [state]
    moveloop(arr)
    return arr[0]

