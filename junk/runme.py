from edu.berkeley.gamesman.core import PrimitiveValue,Record,RecordFields,Configuration
from edu.berkeley.gamesman.database import FileDatabase
from edu.berkeley.gamesman.master import LocalMaster
from edu.berkeley.gamesman.solver import TopDownSolver
from edu.berkeley.gamesman.util import Util
from edu.berkeley.gamesman.hasher import NullHasher
from java.util import Properties,EnumSet

import game_test

props = Properties()
props.setProperty("gamesman.game.width","4")
props.setProperty("gamesman.game.height","4")
props.setProperty("gamesman.db.uri","file:///tmp/python.db")
props.setProperty("gamesman.debug.Solver","true")


conf = Configuration(props)
Util.debugInit(conf)
gm = game_test.OneTwo10(conf)
conf.setGame(gm)
conf.setHasher(NullHasher(conf))

conf.setStoredFields(EnumSet.of(RecordFields.Value))

master = LocalMaster()
master.initialize(conf,TopDownSolver,FileDatabase)

master.run()