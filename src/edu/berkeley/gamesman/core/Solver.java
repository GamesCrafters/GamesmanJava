package edu.berkeley.gamesman.core;

/**
 * A Solver is responsible for solving a Game and storing the result to a
 * Database
 * 
 * @author Steven Schlansker
 */
public abstract class Solver {

	/**
	 * The number of positions to go through between each update/reset
	 */
	public static final int STEP_SIZE = 10000000;

	protected Database readDb;

	protected Database writeDb;

	/**
	 * Set the Database to use for this solver
	 * 
	 * @param conf
	 *            The configuration object
	 */
	public void initialize(Configuration conf) {
		readDb = conf.db;
		writeDb = conf.db;
	}

	/**
	 * Prepare to solve a game and encapsulate all relevant information into a
	 * WorkUnit for later execution by a Master
	 * 
	 * @param config
	 *            The configuration used for this solve
	 * @return a WorkUnit that has enough information to solve the game in its
	 *         entirety.
	 *
	 * @note   This function also modifies the state of the solver.
	 * The WorkUnit allows you to parallelize the solve within one process;
	 * however, the WorkUnit returned does not have all the information
	 * to solve the game.
	 */
	public abstract WorkUnit prepareSolve(Configuration config);

	/**
	 * Prepare to solve a game and encapsulate all relevant information into a
	 * WorkUnit for later execution by a Master
	 * 
	 * @param config
	 *            The configuration used for this solve.
	 *            Uses config.getGame() to determine which game to solve.
	 * @param thisTier
	 *            Which tier in the solve process. Defaults to Game.numberOfTiers() - 1.
	 * @param startTask
	 *            Which task in the sequence to start solving. May not be negative.
	 * @param endTask
	 *            1 + the last task number to solve. If -1, solves all tiers until
	 *            finished. If equal to numberOfTasksForTier(thisTier), solves only
	 *            this single tier.
	 *
	 * @return See prepareSolve(Configuration, Game).
	 *
	 * @note: A task can be used to resume the solver state from a given point,
	 * or to solve only part of the database in the case of a parallel solve.
	 * Do not 
	 */
	@SuppressWarnings("unchecked")
	public WorkUnit prepareSolve(Configuration config,
			int thistier,
			long startTask,
			long endTask) {
		// Throws an exception if config.getGame() is not a subclass of Object
		return this.prepareSolve(config);
	}

	public long numberOfTasksForTier(
			Configuration conf,
			int tierNum) {
		return 1;
	}
}
