from edu.berkeley.gamesman.core import Game
from edu.berkeley.gamesman.core import PrimitiveValue
from edu.berkeley.gamesman.util import Pair
from java.math import BigInteger

class OneTwo10(Game):
  def startingPositions(self):
    return [BigInteger.ZERO]
  
  def validMoves(self,state):
    return [Pair("",BigInteger.valueOf(x)) for x in range(state.intValue()+1,min(state.intValue()+3,10))]

  def primitiveValue(self,state):
    if state.intValue() == 9:
      return PrimitiveValue.LOSE
    return PrimitiveValue.UNDECIDED

  def initialize(self,conf):
    Game.initialize(self,conf)

  def hashToState(self,h):
    return h

  def stateToHash(self,s):
    return s

  def lastHash(self):
    return 9

  def stateToString(self,s):
    return str(s)

  def stringToState(self,s):
    return BigInteger.valueOf(s)

  def getDefaultBoardWidth(self):
    return 10

  def getDefaultBoardHeight(self):
    return 1

  def describe(self):
    return "OneTwo10_scs"
