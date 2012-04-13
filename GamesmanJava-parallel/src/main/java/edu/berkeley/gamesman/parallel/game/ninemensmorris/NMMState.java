package edu.berkeley.gamesman.parallel.game.ninemensmorris;

import edu.berkeley.gamesman.game.type.GameValue;
import edu.berkeley.gamesman.game.util.BitSetBoard;
import edu.berkeley.gamesman.hasher.counting.CountingState;
import edu.berkeley.gamesman.hasher.genhasher.GenHasher;
import edu.berkeley.gamesman.parallel.game.connect4.C4Hasher;

public class NMMState extends CountingState {
	private final int levelsOfBoxes, elementsInABox, boardSize;

	
	public NMMState(NMMHasher myHasher, int levels, int elements) {
		
		super(myHasher, levels * elements);
		this.levelsOfBoxes = levels;
		this.elementsInABox = elements;
		this.boardSize = levels * elements;			
		//changePlace = boardSize - 1;
		//this.myBoard = new BitSetBoard(elements, levels);
	}
	
	@Override //FOR DEBUGGING PURPOSES REWRITE THIS LATER
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
