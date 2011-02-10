package edu.berkeley.gamesman.solver;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import edu.berkeley.gamesman.core.*;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.game.TierGame;
import edu.berkeley.gamesman.game.util.TierState;
import edu.berkeley.gamesman.util.*;

/**
 * @author DNSpies
 */
public class TierSolver extends Solver {

	/**
	 * The default constructor
	 * 
	 * @param conf
	 *            The configuration object
	 */
	public TierSolver(Configuration conf) {
		super(conf);
		maxMem = conf.getLong("gamesman.memory", 1 << 25);
		numThreads = conf.getInteger("gamesman.threads", 1);
		minSplits = conf.getInteger("gamesman.split", numThreads);
		minSplitSize = conf.getInteger("gamesman.minimum.split", 1);
	}

	protected int splits;

	private int count;

	protected final int numThreads;

	protected final int minSplits;

	protected final long maxMem;

	private long[] starts;

	private File minSolvedFile = null;

	boolean parallelSolving;

	protected long times[] = new long[7];

	private final int minSplitSize;

	protected void solvePartialTier(Configuration conf, long firstHash,
			long numHashes) {
		TierGame game = (TierGame) conf.getGame();
		game.setState(game.hashToState(firstHash));
		long currentHash = firstHash;
		Record currentRecord = game.newRecord();
		Record[] childRecords = new Record[game.maxChildren()];
		initialize(game, childRecords);
		TierState[] children = game.newStateArray(game.maxChildren());
		DatabaseHandle writeDh = writeDb.getHandle();
		DatabaseHandle readDh = null;
		if (readDb != null)
			readDh = readDb.getHandle();
		long stepNum = firstHash % STEP_SIZE;
		for (long i = 0; i < numHashes; i++) {
			if (stepNum == STEP_SIZE) {
				updater.calculated(STEP_SIZE);
				stepNum = 0;
			}
			Value v = game.primitiveValue();
			if (v == Value.UNDECIDED) {
				int numChildren = calculateAndFetchValues(game, children,
						childRecords, readDh);
				Record best = game.combine(childRecords, numChildren);
				store(game, currentHash, best, writeDh);
			} else {
				storePrimitive(game, currentHash, v, currentRecord, writeDh);
			}
			if (i < numHashes - 1)
				game.nextHashInTier();
			currentHash++;
			stepNum++;
		}
		writeDb.closeHandle(writeDh);
	}

	protected int calculateAndFetchValues(TierGame game, TierState[] children,
			Record[] childRecords, DatabaseHandle readDh) {
		int numChildren = game.validMoves(children);
		for (int i = 0; i < numChildren; i++) {
			long recordLong = readDb.getRecord(readDh,
					game.stateToHash(children[i]));
			game.longToRecord(children[i], recordLong, childRecords[i]);
			childRecords[i].previousPosition();
		}
		return numChildren;
	}

	private void storePrimitive(TierGame game, long currentHash, Value v,
			Record currentRecord, DatabaseHandle writeDh) {
		currentRecord.value = v;
		currentRecord.remoteness = 0;
		store(game, currentHash, currentRecord, writeDh);
	}

	private void store(TierGame game, long currentHash, Record r,
			DatabaseHandle writeDh) {
		writeDb.putRecord(writeDh, currentHash, game.recordToLong(r));
	}

	private void initialize(TierGame game, Record[] childRecords) {
		for (int i = 0; i < childRecords.length; i++)
			childRecords[i] = game.newRecord();
	}

	@Override
	public WorkUnit prepareSolve(Configuration inconf) {
		String msf = inconf.getProperty("gamesman.minSolvedFile", null);
		if (msf == null)
			tier = ((TierGame) inconf.getGame()).numberOfTiers();
		else {
			minSolvedFile = new File(msf);
			if (minSolvedFile.exists()) {
				try {
					Scanner scan = new Scanner(minSolvedFile);
					tier = scan.nextInt();
					scan.close();
				} catch (FileNotFoundException e) {
					throw new Error("This should never happen", e);
				}
			} else {
				tier = ((TierGame) inconf.getGame()).numberOfTiers();
				try {
					minSolvedFile.createNewFile();
					FileWriter fw = new FileWriter(minSolvedFile);
					fw.write(Integer.toString(tier));
					fw.close();
				} catch (IOException e) {
					throw new Error(e);
				}
			}
		}
		updater = new TierSolverUpdater(conf);
		parallelSolving = false;
		flusher.run();
		needs2Reset = false;
		return new TierSolverWorkUnit(inconf);
	}

	protected int nextIndex = 0;

	protected TierSolverUpdater updater;

	protected CyclicBarrier barr;

	private final Runnable flusher = new Runnable() {
		public void run() {
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
			} else {
				if (barr != null)
					needs2Reset = true;
				TierGame game = (TierGame) conf.getGame();
				long fullStart = game.hashOffsetForTier(tier);
				long fullSize = game.numHashesForTier(tier);
				splits = Math.max(minSplits, numSplits(tier, maxMem));
				starts = writeDb.splitRange(fullStart, fullSize, splits,
						minSplitSize);
			}
		}
	};

	protected int tier;

	private volatile boolean needs2Sync = false;

	private volatile boolean needs2Reset = false;

	protected int numSplits(int tier, long maxMem) {
		return numSplits(tier, maxMem,
				((TierGame) conf.getGame()).numHashesForTier(tier));
	}

	protected int numSplits(int tier, long maxMem, long numHashes) {
		return 1;
	}

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
						throw new Error(
								"TierSolver thread was interrupted while waiting!",
								e);
					} catch (BrokenBarrierException e) {
						throw new Error("Barrier Broken", e);
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
							+ (count + 1) + "/" + (starts.length - 1)
							+ " in tier " + tier + "; " + starts[count] + "-"
							+ starts[count + 1]);
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
					"Solver (" + index + "): " + conf.getGame().describe());
			Pair<Long, Long> slice;
			while ((slice = nextSlice(conf)) != null) {
				thisSlice = slice;
				solvePartialTier(conf, slice.car, slice.cdr);
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

		public ArrayList<WorkUnit> divide(int num) {
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

	/**
	 * @param conf
	 *            The configuration object
	 * @param tier
	 *            The tier
	 * @param startHash
	 *            The first hash in the range to solve
	 * @param numHashes
	 *            The number of hashes to solve
	 * @return A WorkUnit for solving solveSpace
	 */
	public WorkUnit prepareSolve(Configuration conf, int tier, long startHash,
			long numHashes) {
		this.tier = tier;
		this.updater = new TierSolverUpdater(conf, numHashes);
		parallelSolving = true;
		splits = Math.max(minSplits, numSplits(tier, maxMem, numHashes));
		starts = writeDb.splitRange(startHash, numHashes, splits, minSplitSize);
		return new TierSolverWorkUnit(conf);
	}
}