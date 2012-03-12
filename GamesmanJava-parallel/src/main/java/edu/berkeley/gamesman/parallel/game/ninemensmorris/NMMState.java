package edu.berkeley.gamesman.parallel.game.ninemensmorris;

import edu.berkeley.gamesman.hasher.counting.CountingState;
import edu.berkeley.gamesman.hasher.genhasher.GenHasher;

public class NMMState extends CountingState {
	
	int blackPiecesPlaced, whitePiecesPlaced;
	int blackPiecesCaptured, whitePiecesCaptured;
	/* 9 - blackPiecesPlaced = black Pieces that haven't been placed
	 * blackPiecesPlaced-blackPiecesCaptured = black pieces on the board.
	 */

	public NMMState(GenHasher<? extends CountingState> myHasher, int countTo) {
		super(myHasher, countTo);
		// TODO Auto-generated constructor stub
	}
	
	

}
