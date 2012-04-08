package edu.berkeley.gamesman.parallel.game.ninemensmorris;

import edu.berkeley.gamesman.game.type.GameValue;
import edu.berkeley.gamesman.hasher.counting.CountingState;
import edu.berkeley.gamesman.hasher.genhasher.GenHasher;

public class NMMState extends CountingState {
	


	public NMMState(GenHasher<? extends CountingState> myHasher, int countTo) {
		super(myHasher, countTo);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public String toString() {
		return null;
	}
	
	//this method returns whether the given state is valid.
	public boolean valid() {
		/* invariants of being valid
		 * the blackPiecesPlaced + blackPiecesCaptured <= 9
		 * the whitePiecesPlaced + whitePiecesCaptured <= 9
		 */
		return false;
	}
	
	//WRITE LATER
	public char getXPiecesToBePlacedAsChar() {
		return ' ';
	}
	
	//WRITE LATER
	public char getOPiecesToBePlacedAsChar() {
		return ' ';
	}

	//WRITE LATER
	public GameValue getValue() {
		return null;
	}
}
