from edu.berkeley.gamesman.core import Configuration
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
        buf = ""
        buf += "( " + self.game.stateToString(self.state) + " )"
        buf += "\n"
        buf += self.game.displayState(self.state)
        if self.db is not None:
            rec = self.db.getRecord(self.game.stateToHash(self.state))
            buf += rec.toString()
        return buf

    def hash(self):
        return self.game.stateToHash(self.state)
    
    def unhash(self, hash):
        return GameState(self.game, self.game.hashToState(hash))
    
    def moves(self):
        s = []
        for c in self.game.validMoves(self.state):
            s += [(c.car,GameState(self.game, c.cdr, self.db))]
        return s
    
    def printMoves(self):
        for c in self.moves():
            print '"'+c[0]+'"',
            if self.db is not None:
                rec = self.db.getRecord(self.game.stateToHash(c[1].state))
                v = rec.get()
                if v is not None:
                    print v.previousMovesValue().toString(),
            print ' =>'
            print c[1]
    
    def __add__(self, move):
        nextState = self.game.doMove(self.state, move)
        if nextState == None:
            print "Invalid move: " + move
            return self
        gs = GameState(self.game, nextState, self.db)
        #print gs
        return gs

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

def moveloop():
    global globalState
    from edu.berkeley.gamesman.core import PrimitiveValue
    while globalState.game.primitiveValue(globalState.state) is PrimitiveValue.UNDECIDED:
        print "State is now"
        print globalState
        print "Avaliable moves:"
        moves()
        m = raw_input("Which move to take? ")
        if len(m) == 0: return
        move(m)
    print "Game is over!"