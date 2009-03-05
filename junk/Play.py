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
    
    def __init__(self, game, state):
        self.game = game
        self.state = state
        
    def __str__(self):
        return self.game.displayState(self.state)
    
    def __add__(self, move):
        nextState = self.game.doMove(self.state, move)
        if nextState == None:
            print "Invalid move: " + move
            return self
        gs = GameState(self.game, nextState)
        print gs
        return gs

def play(jobFile):
    props = Properties()
    conf = Configuration(props)
    conf.addProperties(jobFile)
    gameClass = Util.typedForName("edu.berkeley.gamesman.game." + conf.getProperty("gamesman.game"))
    #java's newInstance() expects an array
    game = gameClass.getConstructors()[0].newInstance([conf])
    gs = GameState(game, game.startingPositions()[0])
    print gs
    return gs