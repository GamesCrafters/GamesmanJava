package edu.berkeley.gamesman.verification;

public enum Connect4Move implements Move {
	ZERO, ONE, TWO, THREE, FOUR, FIVE, SIX;
	
	public int getColumnIndex() {
		return ordinal();
	}
}
