package edu.berkeley.gamesman.core;

import java.util.List;

/**
 * A WorkUnit is used by a single thread of a solver when doing a multi-threaded
 * solve.
 * 
 * @author Steven Schlansker
 */
public interface WorkUnit {

	/**
	 * Divide a WorkUnit into pieces. For single-threaded solves, do not assume
	 * this method will be called (even once)
	 * 
	 * @param num
	 *            The number of pieces to divide into
	 * @return a List of the new WorkUnits
	 */
	public List<WorkUnit> divide(int num);

	// According to Steven, this should've be a Solver or Master method... Too
	// late now

	/**
	 * Synchronously complete the work represented by this WorkUnit
	 */
	public void conquer();

}
