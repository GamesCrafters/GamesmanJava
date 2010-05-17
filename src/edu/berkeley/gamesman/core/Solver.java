package edu.berkeley.gamesman.core;

import edu.berkeley.gamesman.database.Database;

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

	protected Configuration conf;

	/**
	 * Set the Database to use for this solver
	 * 
	 * @param conf
	 *            The configuration object
	 */
	public void initialize(Configuration conf) {
		readDb = conf.db;
		writeDb = conf.db;
		this.conf = conf;
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
	 * @note This function also modifies the state of the solver. The WorkUnit
	 *       allows you to parallelize the solve within one process; however,
	 *       the WorkUnit returned does not have all the information to solve
	 *       the game.
	 */
	public abstract WorkUnit prepareSolve(Configuration config);

	/**
	 * @param db
	 *            The database for reading from
	 */
	public void setReadDb(Database db) {
		readDb = db;
	}

	/**
	 * @param db
	 *            The database for writing to
	 */
	public void setWriteDb(Database db) {
		writeDb = db;
	}
}
