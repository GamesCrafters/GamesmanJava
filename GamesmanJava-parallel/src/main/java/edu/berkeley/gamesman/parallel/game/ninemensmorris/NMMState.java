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
	
	public boolean valid() {
		
		int oToPlace = get(boardSize);
		int xToPlace = get(boardSize + 1);
		int oOnBoard = get(boardSize + 2);
		int xOnBoard = get(boardSize + 3);
		int actualXonBoard = 0;
		int actualOonBoard = 0;
		boolean rightNumberPieces, oOnBoardIsCorrect, xOnBoardIsCorrect, toPlaceCorrect;
		
		//count how many Xs and Os are on the board to check if it matches the prefix.
		for (int i=0; i<boardSize; i++) {
			if (get(i) == 1)
				actualXonBoard++;
			else if (get(i) == 2)
				actualOonBoard++;
		}
		oOnBoardIsCorrect = actualOonBoard == oOnBoard;
		xOnBoardIsCorrect = actualXonBoard == xOnBoard;
		toPlaceCorrect = (Math.abs(oToPlace - xToPlace)) <= 1;
		rightNumberPieces = (xOnBoard + xToPlace <= 9) && (oOnBoard + oToPlace <= 9);
		return oOnBoardIsCorrect && xOnBoardIsCorrect && toPlaceCorrect && rightNumberPieces;
		
	}

	public char getXPiecesToBePlacedAsChar() {
		return new Integer(get(boardSize+1)).toString().charAt(0);
	}
	

	public char getOPiecesToBePlacedAsChar() {
		return new Integer(get(boardSize)).toString().charAt(0);
	}

	
	public GameValue getValue(int lastTurn) {
		int thisTurn;
		int thisTurnPiecesLeft;
		int thisTurnPiecesToPlace;
		if (lastTurn == 1) {
			thisTurnPiecesLeft = get(boardSize+2);
			thisTurnPiecesToPlace = get(boardSize);
		} else {
			thisTurnPiecesLeft = get(boardSize+3);
			thisTurnPiecesToPlace = get(boardSize+1);
		}
		
		if (thisTurnPiecesToPlace == 0 && thisTurnPiecesLeft < 3)
			return GameValue.LOSE;
			
		return null;
	}
}
