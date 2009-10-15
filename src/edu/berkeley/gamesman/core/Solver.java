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

	protected Database db;

	/**
	 * Set the Database to use for this solver
	 * 
	 * @param conf
	 *            The configuration object
	 */
	public void initialize(Configuration conf) {
		this.db = conf.db;
	}

	/**
	 * Prepare to solve a game and encapsulate all relevant information into a
	 * WorkUnit for later execution by a Master
	 * 
	 * @param config
	 *            The configuration used for this solve
	 * @param game
	 *            The Game to solve
	 * @return a WorkUnit that has enough information to solve the game in its
	 *         entirety.
	 */
	public abstract WorkUnit prepareSolve(Configuration config,
			Game<Object> game);

}
