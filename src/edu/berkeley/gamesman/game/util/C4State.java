package edu.berkeley.gamesman.game.util;

public class C4State {
	int numPieces;

	long spaceArrangement, pieceArrangement;

	public C4State(int numPieces, long spaceArrangement, long pieceArrangement) {
		this.numPieces = numPieces;
		this.spaceArrangement = spaceArrangement;
		this.pieceArrangement = pieceArrangement;
	}

}
