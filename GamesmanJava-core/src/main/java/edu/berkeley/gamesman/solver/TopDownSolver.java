package edu.berkeley.gamesman.solver;

import java.io.IOException;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.game.TopDownGame;
import edu.berkeley.gamesman.game.TopDownMutaGame;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Util;

/**
 * A solver for {@link TopDownMutaGame}
 *
 * <p>Often times the games don't keep internal states, and they are auto
 * wrapped by {@link TopDownGame} before solving.
 *
 * @author dnspies
 */
public class TopDownSolver<S extends State<S>> extends Solver {

	/**
	 * The top down solver only has one job
	 */
	private boolean askedJob = false;

	private final boolean debugSolver;

	/**
	 * The default constructor
	 *
	 * @param conf The configuration object
	 */
	public TopDownSolver(final Configuration conf, Database db) {
		super(conf, db);
		debugSolver = Util.debug(DebugFacility.SOLVER);
	}

	public class TopDownSolveTask implements Runnable {

		/**
		 * Task to solve the game
		 */
		public void run() {
			// Instantiate a TopDownMutaGame
			Game<S> g = conf.getCheckedGame();
			TopDownMutaGame game;
			if (conf.getGame() instanceof TopDownMutaGame) {
				game = (TopDownMutaGame) conf.getGame();
			} else {
				game = wrapGame(g);
			}

			// Initialize database with default records
			Record defaultRecord = game.newRecord();
			defaultRecord.value = Value.UNDECIDED;
			DatabaseHandle readHandle = db.getHandle(true);
			DatabaseHandle writeHandle = db.getHandle(false);
			try {
				db.fill(writeHandle, game.recordToLong(defaultRecord));
			} catch (IOException e) {
				throw new Error(e);
			}

			// Solve from each starting position
			for (int startNum = 0; startNum < game.numStartingPositions(); startNum++) {
				// Init starting position
				game.setStartingPosition(startNum);

				// Time the solver
				long currentTimeMillis = System.currentTimeMillis();

				// Solve the game
				solve(game, game.newRecord(), 0, readHandle, writeHandle);

				System.out.println(Util.millisToETA(System.currentTimeMillis()
						- currentTimeMillis) + " time to complete");
			}
		}

		/**
		 * Wrap the input game as a {@link TopDownMutaGame}
		 *
		 * @param g Game to be wrapped
		 * @return A TopDownMutaGame
		 */
		private TopDownMutaGame wrapGame(Game<S> g) {
			return new TopDownGame<S>(g);
		}

		/**
		 * Top down game solver
		 *
		 * @param game    Instance of top down muta game
		 * @param record  Record instance to receive solution for the starting position
		 * @param depth   The depth of the solver
		 * @param readDh  Read database handle
		 * @param writeDh Write database handle
		 */
		private void solve(TopDownMutaGame game, Record record, int depth,
						   DatabaseHandle readDh, DatabaseHandle writeDh) {

			// Debug info
			if (debugSolver) {
				System.out.println(game.displayState());
			}

			// Get the current position of the mutable game
			// Load the corresponding record from database
			long hash = game.getHash();
			try {
				game.longToRecord(db.readRecord(readDh, hash), record);
			} catch (IOException e) {
				throw new Error(e);
			}

			// Skip record if already decided
			if (record.value != Value.UNDECIDED) {
				return;
			}

			// Check if position is primitive
			Value pv = game.primitiveValue();
			switch (pv) {
				default:
					record.value = pv;
					record.remoteness = 0;
					break;

				case UNDECIDED:

					// Acquire a temporary record instance
					Record bestRecord = game.getPoolRecord();
					bestRecord.value = Value.UNDECIDED;

					int numChildren = game.makeMove();
					for (int child = 0; child < numChildren; child++) {
						// Recursively solve from the child position
						solve(game, record, depth + 1, readDh, writeDh);

						// Get value of the current position based on the next position value
						record.previousPosition();
						// Save as best record if better
						if (bestRecord.value == Value.UNDECIDED
								|| record.compareTo(bestRecord) > 0)
							bestRecord.set(record);

						// Change to a next move
						game.changeMove();
					}
					// If entered the for loop, time to pop back up to the previous position
					if (numChildren > 0)
						game.undoMove();

					// Update record with solution
					record.set(bestRecord);

					// Release the temporary record instance
					game.release(bestRecord);

					break;

				case IMPOSSIBLE:
					throw new Error(
							"Top-down solve should not reach impossible positions");
			}

			// Save record to database
			try {
				db.writeRecord(writeDh, hash, game.recordToLong(record));
			} catch (IOException e) {
				throw new Error(e);
			}
		}
	}

	/**
	 * There's only one job for the TopDownSolver
	 *
	 * @return Runnable
	 */
	@Override
	public Runnable nextAvailableJob() {
		// If job already requested, then should be none available
		if (askedJob) {
			return null;
		} else {
			askedJob = true;
			return new TopDownSolveTask();
		}
	}
}
