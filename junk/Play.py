from edu.berkeley.gamesman.core import Configuration
from edu.berkeley.gamesman.util import Util
from java.util import Properties

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
            buf += self.db.getRecord(self.game.stateToHash(self.state)).toString()
        return buf

    def hash(self):
        return self.game.stateToHash(self.state)
    
    def unhash(self, hash):
        return GameState(self.game, self.game.hashToState(hash))
    
    def __add__(self, move):
        nextState = self.game.doMove(self.state, move)
        if nextState == None:
            print "Invalid move: " + move
            return self
        gs = GameState(self.game, nextState, self.db)
        print gs
        return gs

def play(jobFile):
    props = Properties()
    conf = Configuration(props)
    conf.addProperties(jobFile)
    
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
    print gs
    return gs
