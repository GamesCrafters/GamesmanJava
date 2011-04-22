package edu.berkeley.gamesman.loopyhadoop;

/**
 * @author Eric keeps a couple bools so i can pass them by reference
 */
public class ChangeTracker {
	/**
	 * were changes made in the DB?
	 */
	public boolean changesMadeDB = false;
	/**
	 * were changes made in the num children?
	 */
	public boolean changesMadeNumChildren = false;
}
