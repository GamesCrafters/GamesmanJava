package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.game.util.BitSetBoard;
//import edu.berkeley.gamesman.hasher.ChangedIterator;
//import edu.berkeley.gamesman.hasher.DartboardHasher;

public class LoopyQuickCross extends LoopyMutaGame {
	private final int width;
	private final int height;
	private final int boardSize;
	private final int piecesToWin;
	private char[][] Board;
	private String whoseMove;
	private ArrayList<String> undoMoveHistory = new ArrayList<String>();	
	
	public LoopyQuickCross(Configuration conf) {
		super(conf);
		width = conf.getInteger("gamesman.game.width", 4);
		height = conf.getInteger("gamesman.game.height", 4);
		boardSize = width * height;
		piecesToWin = conf.getInteger("gamesman.game.pieces", 4);
		Board = new char[width][height];
		whoseMove = "Player 1";
	}

	@Override
	/* Opposite of makeMove(), aka making a move but from bottom to top. */
	public boolean changeUnmakeMove() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void remakeMove() {
		// TODO Auto-generated method stub

	}

	@Override
	public int unmakeMove() {
		// TODO Auto-generated method stub
		return 0;
	}

	
	//may be better to consolidate make move, changemove, etc. into valid moves, like in alignment.java
	@Override
	public boolean changeMove() {
		// TODO Auto-generated method stub
		return false;
	}
	
	public String displayState(HashState pos) {
		setToHash(pos.hash);
		return displayState();
	}

	@Override
	public String displayState() {
		StringBuilder board = new StringBuilder(3 *(width + 1) * (height + 1));
		for (int row = 0; row < width; row ++) {
			board.append((height - row) + " ");
			for (int col = 0; col < height; col++) {
				board.append(" " + Board[col][height - row - 1] + " \n");
			}
		}
		//Line below will change for different board widths.
		//Let's take care of that later...
		board.append("   a  b  c  d\n");
		return board.toString();
	}

	@Override
	//' ' = 0, '-' = 1, '|' = 2
	public long getHash() {
		long retHash = 0;
		int index = 0;
		for(int x = 0; x < width; x++){
			for(int y = 0; y < height; y++){
				if (Board[x][y] == ' '){
					//no change
				}
				else if(Board[x][y] == '-'){
					retHash += Math.pow(3, index);
				}
				else if(Board[x][y] == '|'){
					retHash += Math.pow(3, index) * 2;
				}
				else throw new Error("Error when hashing, bad piece");
				index++;
			}
		}
		return retHash;
	}

	@Override
	public void longToRecord(long record, Record toStore) {
		// TODO Auto-generated method stub

	}

	@Override
	public int makeMove() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Collection<String> moveNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int numStartingPositions() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Value primitiveValue() {
		//currently just setting everything to a win until I know what to do
		char currPiece = '-';
		
		for (int i = 0; i<2; i++){
			//checks for a vertical win
			for (int row = 0; row < height; row++) {
				int piecesInRow = 0;
				for (int col = 0; col < width; col++) {
					if (Board[row][col] == currPiece) {
						piecesInRow++;
						if (piecesInRow == piecesToWin)
							return Value.WIN;
					}
					else
						piecesInRow = 0;
				}
			}
			
			//checks for a horizontal win
			for (int col = 0; col < width; col++) {
				int piecesInCol = 0;
				for (int row = 0; row < height; row++) {
					if (Board[row][col] == currPiece) {
						piecesInCol++;
						if (piecesInCol == piecesToWin)
							return Value.WIN;
					} else
						piecesInCol = 0;
				}
			}
			//checks for diagonal win /
			for (int row = 0; row <= height - piecesToWin; row++) {
				for (int col = 0; col <= width - piecesToWin; col++) {
					int pieces;
					for (pieces = 0; pieces < piecesToWin; pieces++) {
						if (Board[row + pieces][col + pieces] != currPiece)
							break;
					}
					if (pieces == piecesToWin)
						return Value.WIN;
				}
			}
			//checks for diagonal win \
			for (int row = 0; row <= height - piecesToWin; row++) {
				for (int col = piecesToWin - 1; col < width; col++) {
					int pieces;
					for (pieces = 0; pieces < piecesToWin; pieces++) {
						if (Board[row + pieces][col - pieces] != currPiece)
							break;
					}
					if (pieces == piecesToWin)
						return Value.WIN;
				}
			}
			currPiece = '|';
		}
		return Value.UNDECIDED;
	}

	@Override
	public long recordToLong(Record fromRecord) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setFromString(String pos) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setStartingPosition(int i) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setToHash(long hash) {
		long hashLeft = hash;
		for (int index = width*height - 1; index >= 0; index--){
			int y = index / 3;
			int x = index % 3;
			double base = Math.pow(3,index);
			if (base > hashLeft){
				Board[x][y] = ' ';
			}
			else if(base * 2 > hashLeft){
				Board[x][y] = '-';
				hashLeft = (long) (hashLeft - base);
			}
			else if(base * 2 <= hashLeft){
				Board[x][y] = '|';
				hashLeft = (long) (hashLeft - (base * 2));
			}
		}
	}

	@Override
	public void undoMove() {
		// TODO Auto-generated method stub

	}

	@Override
	public String describe() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int maxChildren() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long numHashes() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long recordStates() {
		// TODO Auto-generated method stub
		return 0;
	}


}
