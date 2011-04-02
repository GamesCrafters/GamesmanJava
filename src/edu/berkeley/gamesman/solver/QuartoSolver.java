package edu.berkeley.gamesman.solver;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.util.Progressable;

/**
 * A specialized solver which handles the unique way in which child values are
 * combined in Quarto
 * 
 * @author dnspies
 */
public class QuartoSolver extends CacheTierSolver {
	/**
	 * @param conf
	 *            The configuration object
	 * @param db
	 *            The database to read/write from/to
	 */
	public QuartoSolver(Configuration conf, Database db) {
		super(conf, db);
	}

	/**
	 * @param conf
	 *            The configuration object
	 * @param db
	 *            The database to read/write from/to
	 * @param tier
	 *            The tier currently being solved
	 * @param firstHash
	 *            The first hash to solve in that tier
	 * @param numHashes
	 *            The number of hashes to solve
	 * @param progress
	 *            An object to report progress to in order to indicate the job
	 *            hasn't frozen
	 */
	public QuartoSolver(Configuration conf, Database db, int tier,
			long firstHash, long numHashes, Progressable progress) {
		super(conf, db, tier, firstHash, numHashes, progress);
	}

	/**
	 * A SolveTask object for solving Quarto
	 * 
	 * @author dnspies
	 */
	public class QuartoSolveTask extends CacheTierSolveTask {
		private final Record[] underChildren;

		/**
		 * @param firstRecordIndex
		 *            The index of the first record which will be solved by this
		 *            task.
		 * @param numRecords
		 *            The number of records which will be solved by this task.
		 */
		public QuartoSolveTask(long firstRecordIndex, long numRecords) {
			super(firstRecordIndex, numRecords);
			underChildren = myGame.newRecordArray(16);
		}

		@Override
		protected Record combineChildren(int numChildren) {
			int emptySize = 16 - currentTier;
			int start = 0;
			for (int i = 0; i < emptySize; i++) {
				underChildren[i] = myGame.combine(childRecords, start,
						emptySize);
				underChildren[i].previousPosition();
				start += emptySize;
			}
			assert start == numChildren;
			return myGame.combine(underChildren, 0, emptySize);
		}
	}

	@Override
	protected TierSolveTask getSolveTask(long firstRecordIndex, long numRecords) {
		return new QuartoSolveTask(firstRecordIndex, numRecords);
	}
}
