package edu.berkeley.gamesman.game;

import java.sql.Array;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Value;


//The pieces are chars, ' ', '-', and '|'
//public class QuickCross extends LoopyMutaGame {
public class QuickCross{
	private static int width = 4;
	private static int height = 4;
	private int boardSize;
	private static int piecesToWin = 4;
	private static char[][] Board = new char[width][height];
	private static String whoseMove;
	private static ArrayList<String> undoMoveHistory = new ArrayList<String>();

	
	
	//Constructor
	public QuickCross(Configuration conf) {
		//super(conf);
		width = conf.getInteger("gamesman.game.width", 4);
		height = conf.getInteger("gamesman.game.height", 4);
		boardSize = width * height;
		piecesToWin = conf.getInteger("gamesman.game.pieces", 4);
		
	}
	public static void main(String args[]) {
		initializeQuickCross();
		while(true){
			askUserForMove();
		}
	}
	
	private static void initializeQuickCross() {
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				Board[i][j] = ' ';
			}
		}
		
		whoseMove = "Player 1";
		System.out.println("Welcome to QuickCross!\n");
		
	}
	
	/** Prints the board, lists possible moves, waits for user's move
	 *  then makes the move.
	 */


	public static void askUserForMove() {
		printBoard();
		System.out.printf("It is %s's turn.\n", whoseMove);
		ArrayList<String> possibleMoves = generateMoves();
		System.out.println("Enter H for horizontal, V for vertical, F to flip. Example: Ha1");
		System.out.print("> ");
		Scanner sc = new Scanner(System.in); 
		String move;
		while (true)
		{
			move = sc.nextLine();
			if (possibleMoves.contains(move)|| (move.equals("undo") && undoMoveHistory.size()>0))
				break;
			else
			{
				System.out.print(move + " is not a valid move\n> ");
			}
		}
		if (move.equals("undo")){
			undoMove();
		}
		else{
			makeMoveDisplay(move);
			undoMoveHistory.add(move);
			if (isPrimitivePosition()) {
				printBoard();
				//End the game, say who wins
				System.out.printf("%s wins.", whoseMove);
				System.exit(0); //Nonzero means it terminates w/ error
			}
		}
		changeTurn();
		
	}
	
	//parses move entered by user.
	private static void makeMoveDisplay(String move) {
		char p;
		int x = letterToNum(move.charAt(1))-1;
		int y = Integer.decode("" + move.charAt(2))-1;
		if (move.charAt(0) == 'F'){
			if (Board[x][y] == '-'){
				p = '|';
			}
			else if(Board[x][y] == '|'){
				p = '-';
			}
			else throw new Error("Invalid piece on board when making new move");
		}
		
		else if (move.charAt(0) == 'H') p = '-';
		else if (move.charAt(0) == 'V') p = '|';
		else if (move.charAt(0) == ' ') p = ' ';
		else throw new Error("invalid move!");
		Board[x][y] = p;
	}
	
	public static void undoMove(){
		String move = undoMoveHistory.get(undoMoveHistory.size()-1);
		undoMoveHistory.remove(undoMoveHistory.size()-1);
		System.out.println("Undoing move: " + move);
		if (move.charAt(0)=='F'){
			makeMoveDisplay(move);
		}
		else{
			String location = move.substring(1);
			if (move.charAt(0)=='H' || move.charAt(0)=='V'){
				makeMoveDisplay(' '+location);
			}
			else throw new Error("could not undo!");
		}
	}
	 
	static char getPiece(int a, int b) {
		return Board[a][b];
	}
	
	static char getPiece(String loc) {
		int a, b;
		a = letterToNum(loc.charAt(0))-1;
		b = Integer.decode("" + loc.charAt(1))-1;
		return getPiece(a,b);
	}

	static boolean isPrimitivePosition() {
		char currPiece = '-';
		
		for (int i = 0; i<2; i++){
			//checks for a vertical win
			for (int row = 0; row < height; row++) {
				int piecesInRow = 0;
				for (int col = 0; col < width; col++) {
					if (getPiece(row, col) == currPiece) {
						piecesInRow++;
						if (piecesInRow == piecesToWin)
							return true;
					}
					else
						piecesInRow = 0;
				}
			}
			
			//checks for a horizontal win
			for (int col = 0; col < width; col++) {
				int piecesInCol = 0;
				for (int row = 0; row < height; row++) {
					if (getPiece(row, col) == currPiece) {
						piecesInCol++;
						if (piecesInCol == piecesToWin)
							return true;
					} else
						piecesInCol = 0;
				}
			}
			//checks for diagonal win /
			for (int row = 0; row <= height - piecesToWin; row++) {
				for (int col = 0; col <= width - piecesToWin; col++) {
					int pieces;
					for (pieces = 0; pieces < piecesToWin; pieces++) {
						if (getPiece(row + pieces, col + pieces) != currPiece)
							break;
					}
					if (pieces == piecesToWin)
						return true;
				}
			}
			//checks for diagonal win \
			for (int row = 0; row <= height - piecesToWin; row++) {
				for (int col = piecesToWin - 1; col < width; col++) {
					int pieces;
					for (pieces = 0; pieces < piecesToWin; pieces++) {
						if (getPiece(row + pieces, col - pieces) != currPiece)
							break;
					}
					if (pieces == piecesToWin)
						return true;
				}
			}
			currPiece = '|';
		}
		return false;
	}
	
	private static ArrayList<String> generateMoves() {
		ArrayList<String> possibleMoves = new ArrayList<String>();
		for (int h = height-1; h >=0; h--){
			for (int w = 0; w < width; w++){
				if (getPiece(w, h)==' '){
					possibleMoves.add("H" + numToLetter(w+1) + (h+1));
					possibleMoves.add("V" + numToLetter(w+1) + (h+1));
				}
				else if (getPiece(w, h)=='|' || getPiece(w, h) =='-'){
					possibleMoves.add("F" + numToLetter(w+1) + (h+1));
				}
				else{
					throw new Error("piece at " + w + h + "is illegal, cannot generate moves");
				}
			}
		}
		return possibleMoves;
		
	}
	
	
	// a = 1, b=2, etc.
	private static char numToLetter(int num){
		return (char)(num + 96);
	}
	
	private static int letterToNum(char c){
		return (int)(c - 96);
	}
	
	private static String changeTurn() {
		// TODO Auto-generated method stub
		if (whoseMove == "Player 1") whoseMove = "Player 2";
		else whoseMove = "Player 1";
		return whoseMove;
	}
	

	/** Prints the board. */

	public static void printBoard() {
		System.out.print("4 ");
		for (int i = 0; i < Board.length; i++) {
			System.out.print(" " + Board[i][3] + " ");
		}
		System.out.println();
		
		System.out.print("3 ");
		for (int i = 0; i < Board.length; i++) {
			System.out.print(" " + Board[i][2] + " ");
		}
		System.out.println();
		
		System.out.print("2 ");
		for (int i = 0; i < Board.length; i++) {
			System.out.print(" " + Board[i][1] + " ");
		}
		System.out.println();
		
		System.out.print("1 ");
		for (int i = 0; i < Board.length; i++) {
			System.out.print(" " + Board[i][0] + " ");
		}
		System.out.println();
		
		System.out.println("   a  b  c  d\n");		
	}
	
}
	/*

	@Override
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

	@Override
	public boolean changeMove() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public String displayState() {
		
		return null;
	}

	@Override
	public long getHash() {
		// TODO Auto-generated method stub
		return 0;
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
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub

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




//BELOW IS CODE FROM TOP DOWN SOLVER
/*
package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.hasher.DartboardHasher;
import edu.berkeley.gamesman.util.Pair;

/**
 * Tic Tac Toe game by David modified for QuickCross
 * 
 * Still need to do:
 * 
 * -Hashing (I think we should just go with trinary hash) Only impossible positions are 2 or more
 * distinct wins. (it's ok to have 2 wins that share a piece)
 * For 4x4 this comes out to be:
 * 6 impossible positions for 2 horizontal wins at once
 * 6 impossible positions for 2 vertical wins at once
 * 1 impossible position for both diagonals
 * 4 impossible positions for 3 horizontal wins
 * 4 impossible positions for 3 vertical wins
 * = 21 impossible positions in 4x4
 * Compared to 3^16= 43046721, it seems not very worth it to find a perfect hash. I feel like
 * it doesn't change much for 5x5 and bigger boards either
 * 
 * -Remoteness (boardsize - numpieces clearly doesn't work anymore) Also I think games that
 * drag on to infinity are very likely to happen.
 * 
 * -On your turn you can either put down an X or an O. Currently the piece you put down depends
 * on whose turn it is. (Instead of saying that we change a piece on the board, it may be easier
 * to just say we can put a piece anywhere, including on top of an existing piece; it just
 * must be the opposite piece. This means we only have to worry about one type of move, as
 * opposed to two.)
 */
/*
public final class QuickCross extends Game<QuickCrossState> {
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
/*
	public QuickCross(Configuration conf) {
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
/*
	}
	@Override
	public Collection<QuickCrossState> startingPositions() {
		ArrayList<QuickCrossState> returnList = new ArrayList<QuickCrossState>(1);
		QuickCrossState returnState = newState();
		returnList.add(returnState);
		return returnList;
	}

	@Override
	
	public Collection<Pair<String, QuickCrossState>> validMoves(
			QuickCrossState pos) {
		ArrayList<Pair<String, QuickCrossState>> moves = new ArrayList<Pair<String, QuickCrossState>>(
				pos.numPieces + 2*(boardSize - pos.numPieces));
		QuickCrossState[] children = new QuickCrossState[pos.numPieces + 2 * (boardSize
				- pos.numPieces)];
		String[] childNames = new String[children.length];
		for (int i = 0; i < children.length; i++) {
			children[i] = newState();
		}
		validMoves(pos, children);
		int moveCount = 0;
		//all moves are now valid
		/*
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				if (pos.getPiece(row, col) == ' ')
					childNames[moveCount++] = String
							.valueOf((char) ('A' + col))
							+ Integer.toString(row + 1);
			}
		}
		for (int i = 0; i < children.length; i++) {
			moves.add(new Pair<String, QuickCrossState>(childNames[i],
					children[i]));
		}
		return moves;
		*//*
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				childNames[moveCount++] = String
							.valueOf((char) ('A' + col))
							+ Integer.toString(row + 1);
			}
		}
		return moves;
	}

	@Override
	public int maxChildren() {
		return boardSize;
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
					sb.append('-');
				else if (piece == 'X' || piece == 'O')
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
		// all moves are valid
		/*int numMoves = 0;
		char turn = pos.numPieces % 2 == 0 ? 'X' : 'O';
		for (int i = 0; i < boardSize; i++) {
			if (pos.getPiece(i) == ' ') {
				children[numMoves].set(pos);
				children[numMoves].setPiece(i, turn);
				numMoves++;
			}
		}
		return numMoves
		*/
		//char turn = pos.numPieces % 2 == 0 ? 'X' : 'O';
	/*
	for (int i = 0; i < pos.n; i++){
			children[i].set(pos);
			children[i].setPiece(i, turn);
		}
		return boardSize;
	}

	@Override
	public Value primitiveValue(QuickCrossState pos) {
		//char lastTurn = pos.numPieces % 2 == 0 ? 'O' : 'X';
		//we have to check both X's and O's now.
		
		//checks for a horizontal win
		for (int row = 0; row < height; row++) {
			int piecesInRow = 0;
			for (int col = 0; col < width; col++) {
				//if (pos.getPiece(row, col) == lastTurn) {
				if (pos.getPiece(row, col) != ' ') {
					piecesInRow++;
					if (piecesInRow == piecesToWin)
						return Value.LOSE;
				} else
					piecesInRow = 0;
			}
		}
		
		//checks for a vertical win
		for (int col = 0; col < width; col++) {
			int piecesInCol = 0;
			for (int row = 0; row < height; row++) {
				if (pos.getPiece(row, col) != ' ') {
					piecesInCol++;
					if (piecesInCol == piecesToWin)
						return Value.LOSE;
				} else
					piecesInCol = 0;
			}
		}
		//checks for diagonal win /
		for (int row = 0; row <= height - piecesToWin; row++) {
			for (int col = 0; col <= width - piecesToWin; col++) {
				int pieces;
				for (pieces = 0; pieces < piecesToWin; pieces++) {
					//if (pos.getPiece(row + pieces, col + pieces) != lastTurn)
					if (pos.getPiece(row + pieces, col + pieces) == ' ')
						break;
				}
				if (pieces == piecesToWin)
					return Value.LOSE;
			}
		}
		//checks for diagonal win \
		for (int row = 0; row <= height - piecesToWin; row++) {
			for (int col = piecesToWin - 1; col < width; col++) {
				int pieces;
				for (pieces = 0; pieces < piecesToWin; pieces++) {
					if (pos.getPiece(row + pieces, col - pieces) == ' ')
						break;
				}
				if (pieces == piecesToWin)
					return Value.LOSE;
			}
		}
		if (pos.numPieces == boardSize)
			return Value.TIE;
		else
			return Value.UNDECIDED;
	}

	@Override
    // Need to fix hashing. Random return values put in for now
	public long stateToHash(QuickCrossState pos) {
		//long offset = tierOffsets[pos.numPieces];
		//return offset + dh.setNumsAndHash(pos.board);
		return 0;
	}

	@Override
	public long numHashes() {
		//return tierOffsets[boardSize + 1];
		return 0;
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
		
	}*/
	//end hash stuff
/*
	@Override
	public void longToRecord(QuickCrossState recordState, long record,
			Record toStore) {
		if (record == boardSize + 1) {
			toStore.value = Value.TIE;
			//how to calculate remoteness?
			//toStore.remoteness = boardSize - recordState.numPieces;
		} else if (record == boardSize + 2)
			toStore.value = Value.UNDECIDED;
		else if (record >= 0 && record <= boardSize) {
			toStore.value = record % 2 == 0 ? Value.LOSE : Value.WIN;
			//toStore.remoteness = (int) record;
		}
	}

	@Override
	//stuff needs to be fixed here
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
}

//current state of the board
class QuickCrossState implements State {
	final char[] board;
	private final int width;
	//numpieces tells us available moves
	int numPieces = 0;

	public QuickCrossState(int width, int height) {
		this.width = width;
		board = new char[width * height];
		for (int i = 0; i < board.length; i++) {
			board[i] = ' ';
		}
	}

	public QuickCrossState(int width, char[] charArray) {
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
	}

	public void setPiece(int row, int col, char piece) {
		setPiece(row * width + col, piece);
	}

	public void setPiece(int index, char piece) {
		if (board[index] == 'X'){
			board[index] = 'O';
		}
		else if (board[index] == 'O'){
			board[index] = 'X';
		}
		
		else if (piece != 'X' && piece != 'O')
			throw new Error("Invalid Piece: " + piece);
		
		
		else if (board[index] == ' '){
			board[index] = piece;
			numPieces++;
		}
		else throw new Error("Problem with board, cannot set piece");
	}*/
	/*ASK DAVID ABOUT NUMPIECES
	public void setPiece(int index, char piece) {
		//fail case
		if (board[index] != ' '){
			numPieces--;
		}
		
		board[index] = piece;
		
		//good
		if (piece == 'X' || piece == 'O') {
			numPieces++;
		} 
		
		//bad
		else if (piece != ' ')
			throw new Error("Invalid piece: " + piece);
	}
	*/
	/*

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
*/