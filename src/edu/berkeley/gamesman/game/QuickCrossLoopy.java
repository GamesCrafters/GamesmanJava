package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.core.Value;
//import edu.berkeley.gamesman.hasher.DartboardHasher;
import edu.berkeley.gamesman.util.Pair;

/**
 * Top down Tic Tac Toe game by David modified for QuickCros
 */
public final class QuickCrossLoopy extends Game<QuickCrossState> implements LoopyGame<QuickCrossState> {
	private final int width;
	private final int height;
	private final int boardSize;
	private final int piecesToWin;
	//private final long[] tierOffsets;
	//private final DartboardHasher dh;

	/**
	 * Default Constructor
	 * 
	 * @param conf
	 *            The Configuration object
	 */
	public QuickCrossLoopy(Configuration conf) {
		super(conf);
		width = conf.getInteger("gamesman.game.width", 4);
		height = conf.getInteger("gamesman.game.height", 4);
		boardSize = width * height;
		piecesToWin = conf.getInteger("gamesman.game.pieces", 4);
		//no longer needed
		/*
		tierOffsets = new long[boardSize + 2];
		dh = new DartboardHasher(boardSize, ' ', 'O', 'X');
		long total = 0;
		for (int i = 0; i <= boardSize; i++) {
			tierOffsets[i] = total;
			dh.setNums(boardSize - i, i / 2, (i + 1) / 2);
			total += dh.numHashes();
		}
		tierOffsets[boardSize + 1] = total;
		*/
	}

	@Override
	public Collection<QuickCrossState> startingPositions() {
		ArrayList<QuickCrossState> returnList = new ArrayList<QuickCrossState>(1);
		QuickCrossState returnState = newState();
		returnList.add(returnState);
		return returnList;
	}

	@Override
		//given state, returns groupings of names and child states
	public Collection<Pair<String, QuickCrossState>> validMoves(
			QuickCrossState pos) {
		

		
		//Below is for loopy game
		
		ArrayList<Pair<String, QuickCrossState>> moves = new ArrayList<Pair<String, QuickCrossState>>(
				pos.numPieces + 2*(boardSize - pos.numPieces));
		QuickCrossState[] children = new QuickCrossState[pos.numPieces + 2 * (boardSize
				- pos.numPieces)];
		
		String[] childNames = new String[children.length];
		
		for (int i = 0; i < children.length; i++) {
			children[i] = newState();
		}
		
		//this fills up the children array
		validMoves(pos, children);
		int moveCount = 0;
		
		
		//below is for loopy
		
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				//in this case 2 possible moves
				if (pos.getPiece(row, col) == ' ') {
					childNames[moveCount++] = String
							.valueOf('H' + (char) ('A' + col))
							+ Integer.toString(row + 1);
					childNames[moveCount++] = String
							.valueOf('V' + (char) ('A' + col))
							+ Integer.toString(row + 1);
				}
				if (pos.getPiece(row, col) == '-' || pos.getPiece(row, col) == '|'){
					childNames[moveCount++] = String
							.valueOf('F' + (char) ('A' + col))
							+ Integer.toString(row + 1);
				}
			}
		}
		for (int i = 0; i < children.length; i++) {
			moves.add(new Pair<String, QuickCrossState>(childNames[i], children[i]));
		}
		return moves;
		
	}

	@Override
	public int maxChildren() {
		//loopy
		return boardSize*2;
	}

	@Override
	public String stateToString(QuickCrossState pos) {
		return pos.toString();
	}

	@Override
	public String displayState(QuickCrossState pos) {
		StringBuilder sb = new StringBuilder((width + 1) * 2 * (height + 1));
		for (int row = height - 1; row >= 0; row--) {
			sb.append(row + 1);
			for (int col = 0; col < width; col++) {
				sb.append(" ");
				char piece = pos.getPiece(row, col);
				if (piece == ' ')
					sb.append(' ');
				else if (piece == '-' || piece == '|')
					sb.append(piece);
				else
					throw new Error(piece + " is not a valid piece");
			}
			sb.append("\n");
		}
		sb.append(" ");
		for (int col = 0; col < width; col++) {
			sb.append(" ");
			sb.append((char) ('A' + col));
		}
		sb.append("\n");
		return sb.toString();
	}

	@Override
	public QuickCrossState stringToState(String pos) {
		return new QuickCrossState(width, pos.toCharArray());
	}

	@Override
	public String describe() {
		return width + "x" + height + " QuickCross with " + piecesToWin
				+ " pieces";
	}

	@Override
	public QuickCrossState newState() {
		return new QuickCrossState(width, height);
	}

	@Override
	public int validMoves(QuickCrossState pos, QuickCrossState[] children) {

		int numMoves = 0;
		//loopy
		
		for (int i = 0; i < (pos.numPieces + 2 * (boardSize - pos.numPieces)); i++){
			if (pos.getPiece(i) == ' '){
				children[numMoves].set(pos);
				children[numMoves].setPiece(i, '-');
				numMoves++;
				children[numMoves].set(pos);
				children[numMoves].setPiece(i, '|');
				numMoves++;
			}
			else if (pos.getPiece(i) == '-'){
				children[numMoves].set(pos);
				children[numMoves].setPiece(i, '|');
				numMoves++;
			}
			else if (pos.getPiece(i) == '|'){
				children[numMoves].set(pos);
				children[numMoves].setPiece(i, '-');
				numMoves++;
			}
			else throw new Error("cannot generate valid moves from given pos");
		}
		return numMoves;
		
	}

	@Override
	public Value primitiveValue(QuickCrossState pos) {
		//char lastTurn = pos.numPieces % 2 == 0 ? 'O' : 'X';

		//if last move was 1st player and currently even num moves have happened, 4 in a row is a win for me (the 2nd player)
		Value WinorLose = (pos.lastMoveOne == pos.evenNumMoves ? Value.WIN : Value.LOSE);
		
		char currPiece = '-';
		//try both pieces
		for (int i = 0; i<2; i++){
			//checks for a vertical win
			for (int row = 0; row < height; row++) {
				int piecesInRow = 0;
				for (int col = 0; col < width; col++) {
					if (pos.getPiece(row, col) == currPiece) {
						piecesInRow++;
						if (piecesInRow == piecesToWin)
							return WinorLose;
					}
					else
						piecesInRow = 0;
				}
			}
			
			//checks for a horizontal win
			for (int col = 0; col < width; col++) {
				int piecesInCol = 0;
				for (int row = 0; row < height; row++) {
					if (pos.getPiece(row, col) == currPiece) {
						piecesInCol++;
						if (piecesInCol == piecesToWin)
							return WinorLose;
					} else
						piecesInCol = 0;
				}
			}
			//checks for diagonal win /
			for (int row = 0; row <= height - piecesToWin; row++) {
				for (int col = 0; col <= width - piecesToWin; col++) {
					int pieces;
					for (pieces = 0; pieces < piecesToWin; pieces++) {
						if (pos.getPiece(row + pieces, col + pieces) != currPiece)
							break;
					}
					if (pieces == piecesToWin)
						return WinorLose;
				}
			}
			//checks for diagonal win \
			for (int row = 0; row <= height - piecesToWin; row++) {
				for (int col = piecesToWin - 1; col < width; col++) {
					int pieces;
					for (pieces = 0; pieces < piecesToWin; pieces++) {
						if (pos.getPiece(row + pieces, col - pieces) != currPiece)
							break;
					}
					if (pieces == piecesToWin)
						return WinorLose;
				}
			}
			currPiece = '|';
		}
		return Value.UNDECIDED;
	}

	@Override
    // trinary hash
	public long stateToHash(QuickCrossState pos) {
		long retHash = 0;
		
		int index = 0;
		for(int x = 0; x < width; x++){
			for(int y = 0; y < height; y++){
				if (pos.getPiece(x,y) == ' '){
					//no change
				}
				else if(pos.getPiece(x,y) == '-'){
					retHash += Math.pow(3, index);
				}
				else if(pos.getPiece(x,y) == '|'){
					retHash += Math.pow(3, index) * 2;
				}
				else throw new Error("Error when hashing, bad piece");
				index++;
			}
		}
		//retHash = retHash << 1;
		//if (pos.evenNumMoves){
		//	retHash = retHash + 1;
		//}
		return retHash;
	}

	@Override
	public long numHashes() {
		return (long)Math.pow(3, boardSize) << 1;
	}

	@Override
	public long recordStates() {
		return boardSize + 3;
	}

	
	@Override
	public void hashToState(long hash, QuickCrossState s) {
		/*int tier = Arrays.binarySearch(tierOffsets, hash);
		if (tier < 0)
			tier = -tier - 2;
		hash -= tierOffsets[tier];
		dh.setNums(boardSize - tier, tier / 2, (tier + 1) / 2);
		dh.unhash(hash);
		dh.getCharArray(s.board);
		s.numPieces = tier;
		*/
		
		s.numPieces = 0;
		long hashLeft = hash;
		for (int index = width*height - 1; index >= 0; index--){
			int y = index / width;
			int x = index % width;
			double base = Math.pow(3,index);
			if (hashLeft < base){
				s.setPiece(x,y,' ');
			}
			else if(hashLeft < base * 2){
				s.setPiece(x,y,'-');
				s.numPieces++;
				hashLeft = (long) (hashLeft - base);
			}
			else if(hashLeft >= base*2){
				s.setPiece(x,y,'|');
				s.numPieces++;
				hashLeft = (long) (hashLeft - (base * 2));
			}
		}
	}

	@Override
	public void longToRecord(QuickCrossState recordState, long record,
			Record toStore) {
		if (record == boardSize + 1) {
			toStore.value = Value.TIE;
			toStore.remoteness = boardSize - recordState.numPieces;
		} else if (record == boardSize + 2)
			toStore.value = Value.UNDECIDED;
		else if (record >= 0 && record <= boardSize) {
			toStore.value = (record & 1) == 1 ? Value.WIN : Value.LOSE;
			toStore.remoteness = (int) record;
		}
	}

	@Override
	public long recordToLong(QuickCrossState recordState, Record fromRecord) {
		if (fromRecord.value == Value.WIN || fromRecord.value == Value.LOSE)
			return fromRecord.remoteness;
		else if (fromRecord.value == Value.TIE)
			return boardSize + 1;
		else if (fromRecord.value == Value.UNDECIDED)
			return boardSize + 2;
		else
			throw new Error("Invalid Value");
	}

	public int possibleParents(QuickCrossState pos, QuickCrossState[] children) {
		// TODO Auto-generated method stub
		return 0;
	}
}

//current state of the board
class QuickCrossLoopyState implements State {
	final char[] board;
	private final int width;
	int numPieces = 0;
	
	//previous move was made by first player
	boolean lastMoveOne = false;
	//even number of moves so far
	boolean evenNumMoves = true;

	public QuickCrossLoopyState(int width, int height) {
		this.width = width;
		board = new char[width * height];
		for (int i = 0; i < board.length; i++) {
			board[i] = ' ';
		}
	}

	public QuickCrossLoopyState(int width, char[] charArray) {
		this.width = width;
		board = charArray;
	}

	public void set(State s) {
		QuickCrossState qcs = (QuickCrossState) s;
		if (board.length != qcs.board.length)
			throw new Error("Different Length Boards");
		int boardLength = board.length;
		System.arraycopy(qcs.board, 0, board, 0, boardLength);
		numPieces = qcs.numPieces;
		lastMoveOne = qcs.lastMoveOne;
		evenNumMoves = qcs.evenNumMoves;
	}

	public void setPiece(int row, int col, char piece) {
		setPiece(row * width + col, piece);
	}

	public void setPiece(int index, char piece) {
		if (board[index] == ' '){
			board[index] = piece;
			numPieces++;
			lastMoveOne = !lastMoveOne;
			evenNumMoves = !evenNumMoves;
		}
		else if (board[index] == '-' || board[index] == '|'){
			board[index] = piece;
		}
		else throw new Error("Invalid board when setting piece");
	}
	

	public char getPiece(int row, int col) {
		return getPiece(row * width + col);
	}

	public char getPiece(int index) {
		return board[index];
	}

	public String toString() {
		return Arrays.toString(board);
	}
}

