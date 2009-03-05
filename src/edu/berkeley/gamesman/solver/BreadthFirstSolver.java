package edu.berkeley.gamesman.solver;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Game;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.RecordFields;
import edu.berkeley.gamesman.core.Solver;
import edu.berkeley.gamesman.core.WorkUnit;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

public class BreadthFirstSolver extends Solver {

	Configuration conf;

	@Override
	public WorkUnit prepareSolve(Configuration config, Game<Object> game) {
		// TODO Auto-generated method stub
		conf = config;
		int maxRemoteness = Integer.parseInt(conf.getProperty("gamesman.solver.maxRemoteness", "-1"));
		if (maxRemoteness <= 0) {
			maxRemoteness = Integer.MAX_VALUE;
		}
		return new BreadthFirstWorkUnit<Object>(game, db, maxRemoteness);
	}

	class BreadthFirstWorkUnit<T> implements WorkUnit {

		final private Game<T> game;
		final private Database database;
		final private int maxRemoteness;
		
		BreadthFirstWorkUnit (Game<T> g, Database db, int maxRemoteness) {
			game = g;
			database = db;
			this.maxRemoteness = maxRemoteness;
		}
		
		public void conquer() {
			HashSet<BigInteger> seen = new HashSet<BigInteger>();
			BigInteger maxHash = game.lastHash();
			BigInteger numPositionsInLevel = BigInteger.ZERO;
			BigInteger numPositionsSeen = BigInteger.ZERO;
			for (T s : game.startingPositions()) {
				BigInteger hash = game.stateToHash(s);
				seen.add(hash);
				database.putRecord(hash, new Record(conf,game.primitiveValue(s)));
				numPositionsInLevel = numPositionsInLevel.add(BigInteger.ONE);
			}
			int remoteness = 0;
			System.out.println(maxRemoteness);
			while (!numPositionsInLevel.equals(BigInteger.ZERO) && remoteness < maxRemoteness) {
				numPositionsSeen = numPositionsSeen.add(numPositionsInLevel); 
				double percentage = numPositionsSeen.multiply(BigInteger.valueOf(10000)).divide(maxHash).doubleValue()/100.;
				Util.debug(DebugFacility.Solver, "Solving remoteness "+remoteness+"; "+numPositionsSeen+"/"+maxHash+" ("+percentage+"%)");
				numPositionsInLevel = BigInteger.ZERO;
				for (BigInteger hash : Util.bigIntIterator(maxHash)) {
					if (seen.contains(hash)) {
						Record rec = database.getRecord(hash);
						if (rec.get(RecordFields.Remoteness) == remoteness) {
							for (Pair<String,T> child : game.validMoves(game.hashToState(hash))) {
								BigInteger childhash = game.stateToHash(child.cdr);
								if (!seen.contains(childhash)) {
									//System.out.println(child.car+": "+game.displayState(child.cdr));
									Record childrec = new Record(conf, PrimitiveValue.Win);
									childrec.set(RecordFields.Remoteness, remoteness + 1);
									database.putRecord(childhash, childrec);
									seen.add(childhash);
									numPositionsInLevel = numPositionsInLevel.add(BigInteger.ONE);
								}
							}
						}
					}
				}
				remoteness += 1;
			}
			Util.debug(DebugFacility.Solver, "Solving finished!!! Max remoteness is "+(remoteness-1)+" total positions seen = "+numPositionsSeen);
		}

		public List<WorkUnit> divide(int num) {
			WorkUnit wu = this;
			return Arrays.asList(wu);
		}
		
	}
	
}
