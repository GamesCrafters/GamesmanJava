from edu.berkeley.gamesman.core import Game,PrimitiveValue,Record,RecordFields,Configuration
from edu.berkeley.gamesman.database import BlockDatabase
from edu.berkeley.gamesman.master import LocalMaster
from edu.berkeley.gamesman.solver import TopDownSolver, BreadthFirstSolver
from edu.berkeley.gamesman.util import Util, Pair, DebugFacility
from edu.berkeley.gamesman.hasher import NullHasher
from java.util import Properties,EnumSet

import Play

class ConfigWrapper:
	def __init__(self, config, prefix='gamesman.game.'):
		self.realconfig = config
		self.prefix = prefix

	def __setitem__(self, key, val):
		self.realconfig.setProperty(self.prefix+key, val)

	def __getitem__(self, key):
		return self.realconfig[self.prefix+key]

	def __contains__(self, key):
		return (self.prefix+key) in self.realconfig

	def __delitem__(self, key):
		del self.realconfig[self.prefix+key]

class PythonPuzzle(Game):
	def __init__(self, config):
		puzzlename = config['pythonpuzzle']
		self.gameclass = getattr(__import__(puzzlename), puzzlename)
		self.initialize(ConfigWrapper(config))
		Game.__init__(self, config)

	def initialize(self, config):
		defaults = self.gameclass.default_options
		self.config = config
		for key in defaults:
			if key not in self.config:
				self.config[key] = str(defaults[key])
		self.gameinst = self.gameclass.unserialize(self.config)

	def getPlayerCount(self):
		return 0

	def find_solutions(self):
		puzzleQueue = []
		solutions = []
		puzzle = self.gameinst
		puzzleQueue.extend([hash(puzzle)])
		visited = {}
		while puzzleQueue:
			h_currPuzzle = puzzleQueue.pop()
			currPuzzle = puzzle.unhash(h_currPuzzle)
			if currPuzzle.is_illegal() or h_currPuzzle in visited:
				continue
			else:
				visited[h_currPuzzle] = True # Visit myself

			if currPuzzle.is_a_solution():
				solutions.append(currPuzzle)

			h_currChildren = []
			for move in currPuzzle.generate_moves():
				h_currChildren.append(hash(currPuzzle + move))

			puzzleQueue.extend(h_currChildren)
		return solutions

	def startingPositions(self):
		sols = self.gameinst.generate_solutions()
		if not sols:
			sols = self.find_solutions()
		return sols

	def validMoves(self,state):
		return [Pair(str(x), state.do_move(x)) for x in state.generate_moves()]

	def primitiveScore(self,state):
		return state.primitive_score()

	def primitiveValue(self,state):
		if state.is_a_solution():
			return PrimitiveValue.WIN
		elif state.is_a_deadend():
			return PrimitiveValue.LOSE
		elif state.is_illegal():
			return PrimitiveValue.TIE # Not really correct, but...
		return PrimitiveValue.UNDECIDED

	def hashToState(self,h):
		# Note: We need to convert from BigInteger to PyLong
		# for operators such as __mod__ to work.
		if type(h) != long and type(h) != int:
			try:
				h = long(h)
			except:
				h = h.longValue()
		return self.gameinst.unhash(h)

	def stateToHash(self,s):
		return long(hash(s))

	def lastHash(self):
		max = long(self.gameinst.maxhash())
		if not max:
			return 0L
		return max - 1L

	def stateToString(self,s):
		return s.serialize()

	def stringToState(self,string):
		return self.gameclass.unserialize(self.config, string)

	def getDefaultBoardWidth(self):
		raise NotImplementedError()

	def getDefaultBoardHeight(self):
		raise NotImplementedError()

	def displayState(self, s):
		return str(s)

	def describe(self):
		return self.gameclass.__name__

class Solver:
	def __init__(self, options={}, solverclass=None, hasherclass=None):
		if not hasherclass:
			hasherclass = NullHasher
		if not solverclass:
			solverclass = BreadthFirstSolver
		puzzle = options['pythonpuzzle']

		debugOpts = EnumSet.noneOf(DebugFacility)
		debugOpts.add(DebugFacility.CORE)
		# debugOpts.add(DebugFacility.SOLVER)
		Util.enableDebuging(debugOpts)

		props = Properties()
		props.setProperty("gamesman.db.uri","file:///tmp/python.db")
		if "gamesman.hasher" not in props:
			props.setProperty("gamesman.hasher",hasherclass.getName())
		props.setProperty("gamesman.game","PythonPuzzle.py")
		props.setProperty("gamesman.debug.SOLVER","true")
		props.setProperty("gamesman.fields","VALUE,REMOTENESS")
		for opt in options:
			props.setProperty(str(opt), str(options[opt]))
		self.dbfile = props.getProperty("gamesman.db.uri")

		self.conf = Configuration(props, True)
		self.gm = PythonPuzzle(self.conf)
		self.hasher = hasherclass(self.conf)
		self.solverclass = solverclass
		self.conf.initialize(self.gm, self.hasher)
		self.master = None

	def initializeDB(self):
		self.master = LocalMaster()
		self.master.initialize(self.conf,self.solverclass,BlockDatabase)
	def solve(self):
		self.delete()
		try:
			self.initializeDB()
			self.master.run(False)
		except:
			self.delete()
			raise

	def delete(self):
		import os
		dbfile = self.dbfile
		if dbfile[:7] == "file://":
			dbfile = dbfile[7:]
		try:
			os.unlink(dbfile)
		except OSError, e:
			print e

	def getDatabase(self):
		if not self.master:
			self.initializeDB()
		return self.master.database

	def __getitem__(self, state):
		if type(state) == str:
			st = self.gm.stringToState(state)
		elif type(state) == int or type(state) == long:
			st = self.gm.hashToState(long(state))
		else:
			raise ValueError(str(type(state))+" is not str or long")
		return Play.GameState(self.gm, st, self.getDatabase())

	def game(self):
		return self.gm.gameinst

	def gameclass(self):
		return self.gm.gameclass

