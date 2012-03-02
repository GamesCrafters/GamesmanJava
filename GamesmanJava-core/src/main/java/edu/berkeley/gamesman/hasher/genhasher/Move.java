package edu.berkeley.gamesman.hasher.genhasher;

/**
 * Should also implement hashCode and equals. You may call the hashCode and
 * equals functions in edu.berkeley.gamesman.hasher.genhasher.Moves
 * 
 * @author dnspies
 * 
 */
public interface Move {

	public int numChanges();

	public int getChangePlace(int i);

	public int getChangeFrom(int i);

	public int getChangeTo(int i);

}
