package edu.berkeley.gamesman.mapreduce;

public interface LoopyGame {
	public Iterable<Long> getSuccessors(long hash);
	public boolean isPrimitive(long hash);
	public int evalPrimitive(long hash);
	public Iterable<Long> getStartingPositions();
}
