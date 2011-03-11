package edu.berkeley.gamesman;

import java.util.Properties;

/**
 * This is the abstract class that all classes that want to
 * be runnable from the command line should extend.
 * @see Gamesman
 * @author Jeremy Fleischman
 *
 */
public abstract class GamesmanApplication {
	/**
	 * A constructor with no arguments. A GamesmanConfigurableApplication
	 * must have this.
	 */
	public GamesmanApplication() {}
	/**
	 * Causes the current GamesmanApplication to start running.
	 * @param props The properties, probably from a job file
	 * @return Exit status
	 */
	public abstract int run(Properties props);
}
