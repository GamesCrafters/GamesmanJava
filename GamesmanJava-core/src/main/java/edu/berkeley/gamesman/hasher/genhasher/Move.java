package edu.berkeley.gamesman.hasher.genhasher;

public interface Move {

	public int numChanges();

	public int getChangePlace(int i);

	public int getChangeFrom(int i);

	public int getChangeTo(int i);

}
