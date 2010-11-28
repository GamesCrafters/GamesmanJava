#!/usr/bin/env python2.6
from __future__ import with_statement

template="""gamesman.game = PythonPuzzle.py
gamesman.game.binArt = {b}
gamesman.game.circle = {c}
gamesman.game.dots = {d}
gamesman.game.exactSol = {e}
gamesman.solver = BreadthFirstSolver
gamesman.hasher = NullHasher
gamesman.db.uri = tcross_binArt={b}_circle={c}_dots={d}_exactSol={e}.db
gamesman.database = MemoryDatabase:FileDatabase
gamesman.debug.SOLVER = true
gamesman.threads = 1
record.fields = VALUE,REMOTENESS
pythonpuzzle = TCross
"""

def gen(**args):
	name = ''.join(sorted(i[0] for i in args.items() if i[1]))
	with open('TCross_'+name+".job",'w') as f:
		f.write(template.format(**args))

for b in range(2):
	for c in range(2):
		for d in range(2):
			for e in range(2):
				if b or c or d:
					gen(b=b,c=c,d=d,e=e)

