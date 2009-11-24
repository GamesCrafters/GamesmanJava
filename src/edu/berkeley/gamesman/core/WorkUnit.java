package edu.berkeley.gamesman.core;

import java.util.List;

/**
 * A WorkUnit represents enough information that a Solver, given an appropriate
 * WorkUnit, can complete the work represented by this WorkUnit.
 * 
 * @author Steven Schlansker
 */
public interface WorkUnit {

	/**
	 * Divide a WorkUnit into pieces
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
