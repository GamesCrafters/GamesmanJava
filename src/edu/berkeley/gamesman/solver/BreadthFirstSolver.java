package edu.berkeley.gamesman.solver;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

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
import edu.berkeley.gamesman.util.Task;
import edu.berkeley.gamesman.util.Util;

/**
 * A solver designed with puzzles in mind that will do a breadth first
 * search from all of the starting positions until every reachable position
 * has been hit. It is assumed that every starting position is a WIN and that
 * the puzzle is reversible.
 * @author Patrick Horn (with Jeremy Fleischman watching)
 */
public class BreadthFirstSolver extends Solver {

	Configuration conf;
	BigInteger maxHash = null;
	/// = maxHash + 1
	BigInteger hashSpace = null;

	protected CyclicBarrier barr;
	int nextIndex = 0;

	volatile int lastTier = 0;
	
	@Override
	public WorkUnit prepareSolve(Configuration config, Game<Object> game) {
		conf = config;
		int maxRemoteness = conf.getInteger("gamesman.solver.maxRemoteness", Integer.MAX_VALUE);
		maxHash = game.lastHash();
		hashSpace = maxHash.add(BigInteger.ONE);
		System.out.println("Calling alloc "+hashSpace);
		db.alloc(hashSpace);
		Record defaultRecord = new Record(conf, PrimitiveValue.UNDECIDED);
		for (BigInteger index : Util.bigIntIterator(hashSpace)) {
			db.putRecord(index, defaultRecord);
		}
		return new BreadthFirstWorkUnit<Object>(game, db, maxRemoteness);
	}

	class BreadthFirstWorkUnit<T> implements WorkUnit {

		final private Game<T> game;
		final private Database database;
		final private int maxRemoteness;
		final private BigInteger firstHash;
		final private BigInteger lastHashPlusOne;
		
		// Constructs for the entire hash space.
		public BreadthFirstWorkUnit(Game<T> g, Database db, int maxRemoteness) {
			game = g;
			database = db;
			this.maxRemoteness = maxRemoteness;
			lastHashPlusOne = hashSpace;
			firstHash = BigInteger.ZERO;
			nextIndex = 1;
			barr = new CyclicBarrier(1);
		}
		// For divide()
		private BreadthFirstWorkUnit(BreadthFirstWorkUnit<T> copy, BigInteger firstHash, BigInteger lastHashPlusOne) {
			game = copy.game;
			database = copy.database;
			maxRemoteness = copy.maxRemoteness;
			this.firstHash = firstHash;
			this.lastHashPlusOne = lastHashPlusOne;
			nextIndex++;
			barr = new CyclicBarrier(nextIndex);
		}
		
		public void conquer() {
			BigInteger numPositionsInLevel = BigInteger.ZERO;
			BigInteger numPositionsSeen = numPositionsInLevel;
			int remoteness = lastTier;
			Task solveTask = Task.beginTask(String.format("BFS solving \"%s\"", game.describe()));
			solveTask.setTotal(lastHashPlusOne.subtract(firstHash));
			solveTask.setProgress(0);
			Record rec = new Record(conf);
			Record childrec = new Record(conf);
			while (lastTier >= remoteness && remoteness < maxRemoteness) {
				HashSet<BigInteger> setValues = new HashSet<BigInteger>();
				numPositionsInLevel = BigInteger.ZERO;
				for (BigInteger hash : Util.bigIntIterator(firstHash, lastHashPlusOne.subtract(BigInteger.ONE))) {
					rec = database.getRecord(hash, rec);
					if (rec.get() != PrimitiveValue.UNDECIDED &&
							rec.get(RecordFields.REMOTENESS) == remoteness) {
						//System.out.println("Found! "+hash+"="+rec);
						for (Pair<String,T> child : game.validMoves(game.hashToState(hash))) {
							BigInteger childhash = game.stateToHash(child.cdr);
							if (setValues.contains(childhash)) {
								continue;
							}
							childrec = database.getRecord(childhash, childrec);
							if (childrec.get() == PrimitiveValue.UNDECIDED) {
								childrec.set(RecordFields.VALUE, rec.get().value());
								childrec.set(RecordFields.REMOTENESS, remoteness + 1);
								//System.out.println("Setting child "+childhash+"="+childrec);
								database.putRecord(childhash, childrec);
								numPositionsInLevel = numPositionsInLevel.add(BigInteger.ONE);
								numPositionsSeen = numPositionsSeen.add(BigInteger.ONE);
								setValues.add(childhash);
								lastTier = remoteness + 1;
								if(numPositionsSeen.remainder(BigInteger.valueOf(100000)).equals(BigInteger.ZERO)) {
									solveTask.setProgress(numPositionsSeen);
								}
							} else {
								//System.out.println("Get child "+childhash+"="+childrec);
							}
						}
					}
				}
				remoteness += 1;
				Util.debug(DebugFacility.SOLVER, "Number of states at remoteness " + remoteness + ": " + numPositionsInLevel);
				try {
					db.flush();
					barr.await();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (BrokenBarrierException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			solveTask.complete();
			Util.debug(DebugFacility.SOLVER, "Solving finished!!! Max remoteness is "+(remoteness-1)+".");
		}

		public List<WorkUnit> divide(int num) {
			long numPositionsZero = 0;
			long numPositionsOne = 0;
			for (T s : game.startingPositions()) {
				BigInteger hash = game.stateToHash(s);
				Record rec = new Record(conf,game.primitiveValue(s));
				database.putRecord(hash, rec);
				for (Pair<String,T> child: game.validMoves(s)) {
					Record childrec = new Record(conf, PrimitiveValue.WIN);
					BigInteger childhash = game.stateToHash(child.cdr);
					childrec.set(RecordFields.REMOTENESS, 1);
					database.putRecord(childhash, childrec);
					numPositionsOne += 1;
				}
				numPositionsZero += 1;
			}
			Util.debug(DebugFacility.SOLVER, "Number of states at remoteness 0: " + numPositionsZero);
			Util.debug(DebugFacility.SOLVER, "Number of states at remoteness 1: " + numPositionsOne);
			lastTier = 1;
			database.flush();
			ArrayList<WorkUnit> arr = new ArrayList<WorkUnit>(num);
			//arr.add(this);
			BigInteger bignum = BigInteger.valueOf(num);
			BigInteger hashIncrement = hashSpace.divide(bignum);
			BigInteger currentHash = BigInteger.ZERO;
			
			nextIndex--; // this is not going to be included in the List<WorkUnit>.
			if (bignum.compareTo(hashSpace) < 0) {
				for (int i = 0; i < num-1; i++) {
					BigInteger endHash = currentHash.add(hashIncrement);
					arr.add(new BreadthFirstWorkUnit<T>(this, currentHash, endHash));
					currentHash = endHash;
				}
			}
			// add the last one separately in case of rounding errors.
			arr.add(new BreadthFirstWorkUnit<T>(this, currentHash, hashSpace));
			return arr;
		}
		
	}
	
}
