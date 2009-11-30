package edu.berkeley.gamesman.game.util;

public class C4State implements Cloneable {
	public int numPieces;

	public long spaceArrangement, pieceArrangement;

	public C4State(int numPieces, long spaceArrangement, long pieceArrangement) {
		this.numPieces = numPieces;
		this.spaceArrangement = spaceArrangement;
		this.pieceArrangement = pieceArrangement;
	}

	public C4State clone() {
		return new C4State(numPieces, spaceArrangement, pieceArrangement);
	}

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
