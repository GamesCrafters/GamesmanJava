package edu.berkeley.gamesman.game.util;

/**
 * A dummy state for top down C4
 * 
 * @author dnspies
 */
public class C4State implements Cloneable {
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
	 * @param pos
	 *            Sets this state to be the same as pos
	 */
	public void set(C4State pos) {
		numPieces = pos.numPieces;
		spaceArrangement = pos.spaceArrangement;
		pieceArrangement = pos.pieceArrangement;
	}

	public String toString() {
		return "[" + numPieces + "," + spaceArrangement + ","
				+ pieceArrangement + "]";
	}

}
