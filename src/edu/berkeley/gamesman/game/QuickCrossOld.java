package edu.berkeley.gamesman.game;

import java.sql.Array;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.util.Pair;


//The pieces are chars, ' ', '-', and '|'
//public class QuickCross extends LoopyMutaGame {
public class QuickCrossOld{
	private static int width = 4;
	private static int height = 4;
	private int boardSize;
	private static int piecesToWin = 4;
	private static char[][] Board = new char[width][height];
	private static String whoseMove;
	private static ArrayList<String> undoMoveHistory = new ArrayList<String>();

	
	
	//Constructor
	public QuickCrossOld(Configuration conf) {
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
	
/*below is loopymutagame.java
 * 
 * opposite of makemove, aka making a move but from bottom to top.
 * public abstract int unmakeMove();

	public abstract boolean changeUnmakeMove();

	public abstract void remakeMove();
 */
/* below is topdownmutagame.java
/*
 * @Override
	public String displayState(HashState pos) {
		setToHash(pos.hash);
		return displayState();
	}

	/**
	 * "Pretty-print" the current State for display to the user
	 * 
	 * @return a pretty-printed string
	 */
/*
	public abstract String displayState();

	@Override
	public void hashToState(long hash, HashState state) {
		state.hash = hash;
	}

	/**
	 * Sets the board position to the passed hash
	 * 
	 * @param hash
	 *            The hash to match
	 */
/*
	public abstract void setToHash(long hash);

	@Override
	public Value primitiveValue(HashState pos) {
		setToHash(pos.hash);
		return primitiveValue();
	}

	/**
	 * @return The primitive value of the current position
	 */
/*
	public abstract Value primitiveValue();

	@Override
	public long stateToHash(HashState pos) {
		return pos.hash;
	}

	/**
	 * @return The hash of the current position
	 */
/*
	public abstract long getHash();

	@Override
	public String stateToString(HashState pos) {
		setToHash(pos.hash);
		return toString();
	}

	@Override
	public HashState stringToState(String pos) {
		setFromString(pos);
		return newState(getHash());
	}

	/**
	 * Sets the board to the position passed in string form
	 * 
	 * @param pos
	 *            The position to set to
	 */
/*
	public abstract void setFromString(String pos);

	/**
	 * Makes a move on the board. The possible moves are ordered such that this
	 * will always be the move made when makeMove() is called
	 * 
	 * @return The number of available moves
	 */
/*
	public abstract int makeMove();

	/**
	 * Changes the last move made to the next possible move in the list
	 * 
	 * @return If there are any more moves to be tried
	 */
/*
	public abstract boolean changeMove();

	/**
	 * Undoes the last move made
	 */
/*
	public abstract void undoMove();

	@Override
	public Collection<Pair<String, HashState>> validMoves(HashState pos) {
		setToHash(pos.hash);
		return validMoves();
	}

	private Collection<Pair<String, HashState>> validMoves() {
		Collection<String> moveStrings = moveNames();
		HashState[] states = new HashState[moveStrings.size()];
		for (int i = 0; i < states.length; i++) {
			states[i] = newState();
		}
		validMoves(states);
		ArrayList<Pair<String, HashState>> validMoves = new ArrayList<Pair<String, HashState>>(
				moveStrings.size());
		int i = 0;
		for (String move : moveStrings) {
			validMoves.add(new Pair<String, HashState>(move, states[i++]));
		}
		return validMoves;
	}

	public abstract Collection<String> moveNames();

	@Override
	public Collection<HashState> startingPositions() {
		int numStartingPositions = numStartingPositions();
		ArrayList<HashState> startingPositions = new ArrayList<HashState>(
				numStartingPositions);
		for (int i = 0; i < numStartingPositions; i++) {
			setStartingPosition(i);
			HashState thisState = newState(getHash());
			startingPositions.add(thisState);
		}
		return startingPositions;
	}

	public abstract int numStartingPositions();

	public abstract void setStartingPosition(int i);

	@Override
	public int validMoves(HashState pos, HashState[] children) {
		setToHash(pos.hash);
		return validMoves(children);
	}

	private int validMoves(HashState[] children) {
		int numChildren = makeMove();
		for (int child = 0; child < numChildren; child++) {
			children[child].hash = getHash();
			changeMove();
		}
		undoMove();
		return numChildren;
	}

	@Override
	public HashState newState() {
		return new HashState();
	}

	private HashState newState(long hash) {
		return new HashState(hash);
	}

	@Override
	public void longToRecord(HashState recordState, long record, Record toStore) {
		setToHash(recordState.hash);
		longToRecord(record, toStore);
	}

	public abstract void longToRecord(long record, Record toStore);

	@Override
	public long recordToLong(HashState recordState, Record fromRecord) {
		setToHash(recordState.hash);
		return recordToLong(fromRecord);
	}

	public abstract long recordToLong(Record fromRecord);
}

 */













