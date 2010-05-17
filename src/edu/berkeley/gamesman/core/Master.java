package edu.berkeley.gamesman.core;

import edu.berkeley.gamesman.database.Database;


/**
 * A Master is responsible for coordinating the interaction between the Game, Solver, Hasher, and Database while solving a game.
 * @author Steven Schlansker
 */
public interface Master {

	/**
	 * Set up the Master and prepare to solve
	 * @param conf The Game to solve
	 * @param solver The Solver to use to solve it
	 * @param database The Database to store information to
	 * @param cached Whether to cache the database using DatabaseCache
	 */
	public void initialize(Configuration conf, Class<? extends Solver> solver, Class<? extends Database> database, boolean cached);
	/**
	 * Execute the solve
	 */
	public void run();
	
	/**
	 * Execute the solve
	 * Currently used by the JythonInterface to allow interaction with the database afterwards.
	 * @param close  If false, leaves database open for reading.
	 */
	public void run(boolean close);
}
