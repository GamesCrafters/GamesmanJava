from edu.berkeley.gamesman.core import PrimitiveValue,Record,RecordFields,Configuration
from edu.berkeley.gamesman.database import FileDatabase
from edu.berkeley.gamesman.master import LocalMaster
try:
  from edu.berkeley.gamesman.solver import TopDownSolver, BreadthFirstSolver
except:
  pass
from edu.berkeley.gamesman.util import Util
from edu.berkeley.gamesman.hasher import NullHasher
from java.util import Properties,EnumMap

import game_test

props = Properties()
props.setProperty("gamesman.game.width","4")
props.setProperty("gamesman.game.height","4")
props.setProperty("gamesman.db.uri","/tmp/python.db")
props.setProperty("gamesman.db.nWayAssociative","7")
props.setProperty("gamesman.db.pageSize","19173961")
props.setProperty("gamesman.db.cacheSize","134217728")
props.setProperty("gamesman.debug.Solver","true")
props.setProperty("record.fields","VALUE:3,REMOTENESS:25")
props.setProperty("record.compression","90")
props.setProperty("pythonpuzzle","Rubik")

conf = Configuration(props,True)

#gm = game_test.OneTwo10(conf)
import PythonPuzzle
gm = PythonPuzzle.PythonPuzzle(conf)

conf.initialize(gm,NullHasher(conf),True)

solver = TopDownSolver
solver = BreadthFirstSolver

master = LocalMaster()
master.initialize(conf,solver,FileDatabase,True) # Cached

master.run()
