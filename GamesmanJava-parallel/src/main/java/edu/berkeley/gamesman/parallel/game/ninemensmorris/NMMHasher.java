package edu.berkeley.gamesman.parallel.game.ninemensmorris;

import edu.berkeley.gamesman.hasher.invhasher.OptimizingInvariantHasher;

public class NMMHasher extends OptimizingInvariantHasher<NMMState> {
	/*
	int blackPiecesPlaced, whitePiecesPlaced;
	int blackPiecesCaptured, whitePiecesCaptured;
	/* 9 - blackPiecesPlaced = black Pieces that haven't been placed
	 * blackPiecesPlaced-blackPiecesCaptured = black pieces on the board.
	 */

	public NMMHasher(int[] digitBase) {
		super(digitBase);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected NMMState genHasherNewState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected long getInvariant(NMMState state) {
		// TODO Auto-generated method stub
		return 0;
	}

	
	//Let's just write a valid method within state and call it on itself
	@Override
	protected boolean valid(NMMState state) {
		// TODO Auto-generated method stub
		return false;
	}	
}
