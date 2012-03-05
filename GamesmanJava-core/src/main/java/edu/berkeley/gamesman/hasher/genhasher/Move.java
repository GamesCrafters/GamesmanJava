package edu.berkeley.gamesman.hasher.genhasher;

/**
 * Should also implement hashCode and equals. You may call the hashCode and
 * equals functions in edu.berkeley.gamesman.hasher.genhasher.Moves
 * 
 * Move consists of a set of triplets (place, from, to) which specify what
 * changes when this move is made, what it changes from, and what it changes to.
 * 
 * These changes must be ordered by place from smallest to largest.
 * 
 * @author dnspies
 * 
 */
public interface Move {

	/**
	 * @return The number of changes made to the sequence by this move
	 */
	public int numChanges();

	/**
	 * @param i
	 *            Which change
	 * @return The place in the sequence at which the ith change occurs
	 */
	public int getChangePlace(int i);

	/**
	 * @param i
	 *            Which change
	 * @return The initial value of the piece at the place where the ith change
	 *         occurs
	 */
	public int getChangeFrom(int i);

	/**
	 * @param i
	 *            Which change
	 * @return The final value of the piece at the place where the ith change
	 *         occurs after the move is made.
	 */
	public int getChangeTo(int i);

}
