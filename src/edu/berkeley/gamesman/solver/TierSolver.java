package edu.berkeley.gamesman.solver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import edu.berkeley.gamesman.core.*;
import edu.berkeley.gamesman.database.TierZippedDatabase;
import edu.berkeley.gamesman.game.TieredGame;
import edu.berkeley.gamesman.hasher.TieredHasher;
import edu.berkeley.gamesman.util.*;

/**
 * TierSolver documentation stub
 * 
 * @author Steven Schlansker
 * @param <T>
 *            The state type for the game
 */
public class TierSolver<T extends State> extends Solver {

	protected static final double SAFETY_MARGIN = 2.0;

	private boolean strictSafety;

	protected boolean strainingMemory;

	private int splits;

	private long maxMem;

	private int count;

	private int numThreads;

	private int minSplit;

	private long[] starts;

	private File minSolvedFile = null;

	boolean parallelSolving;

	protected double prevToCurFraction;

	protected double marginVarSum = 0;

	protected long timesUsed = 0;

	@Override
	public WorkUnit prepareSolve(Configuration inconf) {
		String msf = inconf.getProperty("gamesman.minSolvedFile", null);
		strictSafety = inconf.getBoolean("gamesman.solver.strictMemory", false);
		if (msf == null)
			tier = Util.<TieredGame<T>, Game<?>> checkedCast(inconf.getGame())
					.numberOfTiers();
		else {
			minSolvedFile = new File(msf);
			if (minSolvedFile.exists()) {
				try {
					Scanner scan = new Scanner(minSolvedFile);
					tier = scan.nextInt();
					scan.close();
				} catch (FileNotFoundException e) {
					Util.fatalError("This should never happen", e);
				}
			} else {
				tier = Util.<TieredGame<T>, Game<?>> checkedCast(
						inconf.getGame()).numberOfTiers();
				try {
					minSolvedFile.createNewFile();
					FileWriter fw = new FileWriter(minSolvedFile);
					fw.write(Integer.toString(tier));
					fw.close();
				} catch (IOException e) {
					Util.fatalError("IO Error", e);
				}
			}
		}
		updater = new TierSolverUpdater();
		parallelSolving = false;
		flusher.run();
		needs2Reset = false;
		return new TierSolverWorkUnit(inconf);
	}

	protected void solvePartialTier(Configuration conf, long start,
			long hashes, TierSolverUpdater t, Database inRead, Database inWrite) {
		long current = start;
		TieredGame<T> game = Util.checkedCast(conf.getGame());
		for (long i = 0L; i < hashes; i++) {
			if (current % STEP_SIZE == 0)
				t.calculated(STEP_SIZE);

			T state = game.hashToState(current);

			PrimitiveValue pv = game.primitiveValue(state);

			if (pv.equals(PrimitiveValue.UNDECIDED)) {
				assert Util.debug(DebugFacility.SOLVER,
						"Primitive value for state " + current
								+ " is undecided");
				Collection<Pair<String, T>> children = game.validMoves(state);
				ArrayList<Record> vals = new ArrayList<Record>(children.size());
				for (Pair<String, T> child : children) {
					vals.add(inRead.getRecord(game.stateToHash(child.cdr)));
				}
				Record[] theVals = new Record[vals.size()];
				Record newVal = game.combine(vals.toArray(theVals), 0,
						theVals.length);
				inWrite.putRecord(current, newVal);
			} else {
				Record prim = game.newRecord();
				prim.value = pv;
				assert Util.debug(DebugFacility.SOLVER,
						"Primitive value for state " + current + " is " + prim);
				inWrite.putRecord(current, prim);
			}
			++current;
		}
		assert Util.debug(DebugFacility.THREADING,
				"Reached end of partial tier at " + (start + hashes));
	}

	protected int nextIndex = 0;

	protected TierSolverUpdater updater;

	protected CyclicBarrier barr;

	private final Runnable flusher = new Runnable() {
		public void run() {
			if (writeDb != null) {
				writeDb.flush();
			}
			if (minSolvedFile != null) {
				try {
					FileWriter fw;
					fw = new FileWriter(minSolvedFile);
					fw.write(Integer.toString(tier));
					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			--tier;
			needs2Sync = false;
			if (tier < 0) {
				updater.complete();
				if (minSolvedFile != null)
					minSolvedFile.delete();
				if (timesUsed > 0)
					assert Util.debug(DebugFacility.SOLVER,
							"Standard Deviation = "
									+ Math.sqrt(marginVarSum / timesUsed));
			} else {
				if (writeDb instanceof TierZippedDatabase) {
					((TierZippedDatabase) writeDb).setTier(tier);
				}
				if (barr != null)
					needs2Reset = true;
				TieredGame<T> game = Util.checkedCast(conf.getGame());
				long fullStart = game.hashOffsetForTier(tier);
				TieredHasher<T> h = Util.checkedCast(conf.getHasher());
				long fullSize = h.numHashesForTier(tier);
				long neededMem = memNeededForTier(conf);
				TieredHasher<T> hasher = Util.checkedCast(conf.getHasher());
				prevToCurFraction = (tier >= game.numberOfTiers() - 1) ? 0
						: ((double) hasher.numHashesForTier(tier + 1) / hasher
								.numHashesForTier(tier));
				splits = Math.max(minSplit,
						(int) (neededMem * numThreads / maxMem));
				strainingMemory = strictSafety && splits > minSplit;
				starts = Util.groupAlignedTasks(splits, fullStart, fullSize,
						conf.recordsPerGroup);
			}
		}
	};

	protected int tier;

	private volatile boolean needs2Sync = false;

	private volatile boolean needs2Reset = false;

	protected Pair<Long, Long> nextSlice(Configuration conf) {
		while (true) {
			if (needs2Sync) {
				if (parallelSolving) {
					updater.complete();
					return null;
				}
				if (barr == null)
					flusher.run();
				else {
					assert Util.debug(DebugFacility.THREADING,
							"Thread waiting to tier-sync");
					try {
						barr.await();
						synchronized (this) {
							if (needs2Reset) {
								needs2Reset = false;
								barr.reset();
							}
						}
					} catch (InterruptedException e) {
						Util
								.fatalError(
										"TierSolver thread was interrupted while waiting!",
										e);
					} catch (BrokenBarrierException e) {
						Util.fatalError("Barrier Broken", e);
					}
				}
			}
			synchronized (this) {
				if (!needs2Sync) {
					if (tier < 0) {
						return null;
					}
					Pair<Long, Long> slice = new Pair<Long, Long>(
							starts[count], starts[count + 1] - starts[count]);
					assert Util.debug(DebugFacility.THREADING, "Solving "
							+ (count + 1) + "/" + splits + " in tier " + tier
							+ "; " + starts[count] + "-" + starts[count + 1]);
					if (count < starts.length - 2) {
						++count;
					} else {
						count = 0;
						needs2Sync = true;
					}
					return slice;
				}
			}
		}
	}

	private final class TierSolverWorkUnit implements WorkUnit {

		private int index;

		Configuration conf;

		Pair<Long, Long> thisSlice;

		TierSolverWorkUnit(Configuration conf) {
			this.conf = conf;
			this.index = nextIndex++;
		}

		public void conquer() {
			assert Util.debug(DebugFacility.SOLVER, "Started the solver... ("
					+ index + ")");
			Thread.currentThread().setName(
					"Solver (" + index + "): " + conf.getGame().toString());
			Pair<Long, Long> slice;
			while ((slice = nextSlice(conf)) != null) {
				thisSlice = slice;
				if (parallelSolving) {
					try {
						Database myWrite = writeDb.beginWrite(tier, slice.car,
								slice.car + slice.cdr);
						solvePartialTier(conf, slice.car, slice.cdr, updater,
								readDb, myWrite);
						writeDb.endWrite(tier, myWrite, slice.car, slice.car
								+ slice.cdr);
					} catch (Util.FatalError e) {
						e.printStackTrace(System.out);
						throw e;
					} catch (Throwable e) {
						e.printStackTrace(System.out);
						throw new RuntimeException(e);
					}
				} else
					solvePartialTier(conf, slice.car, slice.cdr, updater,
							readDb, writeDb);
			}
			if (barr != null)
				try {
					barr.await();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (BrokenBarrierException e) {
					e.printStackTrace();
				}
		}

		public List<WorkUnit> divide(int num) {
			ArrayList<WorkUnit> arr = new ArrayList<WorkUnit>(num);
			arr.add(this);
			for (int i = 1; i < num; i++)
				arr.add(new TierSolverWorkUnit(conf.cloneAll()));
			if (parallelSolving || numThreads == 1)
				barr = null;
			else
				barr = new CyclicBarrier(numThreads, flusher);
			return arr;
		}

		@Override
		public String toString() {
			String str = "WorkUnit " + index + "; slice is ";
			if (thisSlice != null) {
				str += "[" + thisSlice.car + "-" + thisSlice.cdr + "]";
			} else {
				str += "null";
			}
			return str;
		}
	}

	final class TierSolverUpdater {

		private long total = 0;

		private Task t;

		TierSolverUpdater() {
			this(conf.getGame().numHashes());
		}

		TierSolverUpdater(long totalProgress) {
			TieredGame<T> myGame = Util.checkedCast(conf.getGame());
			t = Task.beginTask("Tier solving \"" + myGame.describe() + "\"");
			t.setTotal(totalProgress);
		}

		synchronized void calculated(int howMuch) {
			total += howMuch;
			if (t != null) {
				t.setProgress(total);
			}
		}

		public void complete() {
			if (t != null)
				t.complete();
			t = null;
		}
	}

	@Override
	public void initialize(Configuration conf) {
		super.initialize(conf);
		maxMem = conf.getLong("gamesman.memory", Integer.MAX_VALUE);
		numThreads = conf.getInteger("gamesman.threads", 1);
		minSplit = conf.getInteger("gamesman.split", numThreads);
	}

	/**
	 * @param conf
	 *            The configuration object
	 * @param tier
	 *            The tier
	 * @param startHash
	 *            The tier to solve
	 * @param endHash
	 *            The range in the given tier to solve
	 * @return A WorkUnit for solving solveSpace
	 */
	public WorkUnit prepareSolve(Configuration conf, int tier, long startHash,
			long endHash) {
		strictSafety = conf.getBoolean("gamesman.solver.strictMemory", false);
		this.tier = tier;
		updater = new TierSolverUpdater(endHash - startHash);
		parallelSolving = true;
		TieredGame<T> game = Util.checkedCast(conf.getGame());
		long fullSize = endHash - startHash;
		long neededMem = memNeededForRange(conf, fullSize);
		TieredHasher<T> hasher = Util.checkedCast(conf.getHasher());
		prevToCurFraction = (tier >= game.numberOfTiers() - 1) ? 0
				: ((double) hasher.numHashesForTier(tier + 1) / hasher
						.numHashesForTier(tier));
		splits = Math.max(minSplit, (int) (neededMem * numThreads / maxMem));
		strainingMemory = strictSafety && splits > minSplit;
		starts = Util.groupAlignedTasks(splits, startHash, fullSize,
				conf.recordsPerGroup);
		return new TierSolverWorkUnit(conf);
	}

	private long memNeededForRange(Configuration conf, long fullSize) {
		TieredHasher<T> hasher = Util.checkedCast(conf.getHasher());
		TieredGame<T> game = Util.checkedCast(conf.getGame());
		long tierHashes = hasher.numHashesForTier(tier);
		return (long) ((tierHashes + game.maxChildren()
				* (tier == game.numberOfTiers() - 1 ? 0 : hasher
						.numHashesForTier(tier + 1)) * SAFETY_MARGIN)
				/ conf.recordsPerGroup * conf.recordGroupByteLength * (fullSize / (double) tierHashes));
	}

	private long memNeededForTier(Configuration conf) {
		TieredHasher<T> hasher = Util.checkedCast(conf.getHasher());
		TieredGame<T> game = Util.checkedCast(conf.getGame());
		return (long) ((hasher.numHashesForTier(tier) + game.maxChildren()
				* (tier == game.numberOfTiers() - 1 ? 0 : hasher
						.numHashesForTier(tier + 1)) * SAFETY_MARGIN)
				* conf.recordGroupByteLength / conf.recordsPerGroup);
	}
}
