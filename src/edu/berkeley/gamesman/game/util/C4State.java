package edu.berkeley.gamesman.game.util;

import edu.berkeley.gamesman.core.State;

/**
 * A dummy state for top down C4
 * 
 * @author dnspies
 */
public class C4State implements Cloneable, State {
	/**
	 * The number of pieces on the board
	 */
	public int numPieces;

	/**
	 * A hash for the column heights
	 */
	public long spaceArrangement;
	/**
	 * A hash for the ordering of colors
	 */
	public long pieceArrangement;

	/**
	 * @param numPieces
	 *            The number of pieces on the board
	 * @param spaceArrangement
	 *            The column height hash
	 * @param pieceArrangement
	 *            The color arrangement hash
	 */
	public C4State(int numPieces, long spaceArrangement, long pieceArrangement) {
		this.numPieces = numPieces;
		this.spaceArrangement = spaceArrangement;
		this.pieceArrangement = pieceArrangement;
	}

	public C4State clone() {
		return new C4State(numPieces, spaceArrangement, pieceArrangement);
	}

	/**
	 * @param s
	 *            Sets this state to be the same as s
	 */
	public void set(State s) {
		if (s instanceof C4State) {
			C4State pos = (C4State) s;
			numPieces = pos.numPieces;
			spaceArrangement = pos.spaceArrangement;
			pieceArrangement = pos.pieceArrangement;
		} else
			throw new RuntimeException("Type mismatch");
	}

	public String toString() {
		return "[" + numPieces + "," + spaceArrangement + ","
				+ pieceArrangement + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof C4State) {
			C4State other = (C4State) obj;
			return numPieces == other.numPieces
					&& spaceArrangement == other.spaceArrangement
					&& pieceArrangement == other.pieceArrangement;
		} else
			return false;
	}

}
