package edu.berkeley.gamesman.game;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.PrimitiveValue;

/**
 * @author DNSpies
 * Tic Tac Toe: 3 in a row
 */
public class TicTacToe extends DartboardGame {
	private final int piecesToWin;
	
	/**
	 * @param conf The configuration object
	 */
	public TicTacToe(Configuration conf) {
		super(conf);
		piecesToWin = Integer.parseInt(conf.getProperty("gamesman.game.pieces", "3"));
	}

	@Override
	public TicTacToe clone() {
		TicTacToe other = new TicTacToe(conf);
		other.setState(getState());
		return other;
	}
	
	@Override
	public PrimitiveValue primitiveValue() {
		char lastTurn = (numPieces % 2 == 1) ? 'X' : 'O';
		for (int row = 0; row < gameHeight; row++) {
			for (int col = 0; col < gameWidth; col++) {
				if(checkWinOnPiece(row,col,lastTurn))
					return PrimitiveValue.LOSE;
			}
		}
		if(numPieces == gameWidth*gameHeight)
			return PrimitiveValue.TIE;
		else
			return PrimitiveValue.UNDECIDED;
	}

	private boolean checkWinOnPiece(int row, int col, char lastTurn) {
		if(get(row,col)!=lastTurn)
			return false;
		int i;
		for(i=1;i<piecesToWin;i++){
			if(!(exists(row+i,col)&&get(row+i,col)==lastTurn))
				break;
		}
		if(i==piecesToWin)
			return true;
		for(i=1;i<piecesToWin;i++){
			if(!(exists(row,col+i)&&get(row,col+i)==lastTurn))
				break;
		}
		if(i==piecesToWin)
			return true;
		for(i=1;i<piecesToWin;i++){
			if(!(exists(row+i,col+i)&&get(row+i,col+i)==lastTurn))
				break;
		}
		if(i==piecesToWin)
			return true;
		for(i=1;i<piecesToWin;i++){
			if(!(exists(row+i,col-i)&&get(row+i,col-i)==lastTurn))
				break;
		}
		if(i==piecesToWin)
			return true;
		return false;
	}

	@Override
	public String describe() {
		return "Tic Tac Toe";
	}
}
