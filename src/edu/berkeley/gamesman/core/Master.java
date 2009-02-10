package edu.berkeley.gamesman.core;


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
	 */
	public void initialize(Configuration conf, Class<? extends Solver> solver, Class<? extends Database> database);
	/**
	 * Execute the solve
	 */
	public void run();
}
