package edu.berkeley.gamesman.game;

import java.sql.Array;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Value;

public class QuickCross extends LoopyMutaGame {

	public static void main(String args[]) {
		initializeQuickCross();
		askUserForMove();
		
	}
	
	static String[][] Board = new String[4][4]; //Double Array with values "   ", "|  ", or "__ "
	static String whoseMove;
	
	private static void initializeQuickCross() {
		for (int i = 0; i < Board.length; i++) {
			for (int j = 0; j < Board[i].length; j++) {
				Board[i][j] = "   ";
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
		System.out.print("Possible moves: ");
		for (int i = 0; i < possibleMoves.size(); i++) {
			System.out.println(possibleMoves.get(i) + ", ");
		}
		System.out.print("\n> ");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		char[] cbuf = new char[4];
		try {
			br.read(cbuf);
		} catch (IOException e) {
			System.err.println("ERROR.");
			System.exit(1);
		}
		while(true) {
			if (cbuf[3] == '\n') makeMoveDisplay(cbuf);
			break;
		}
		changeTurn();
		if (isPrimitivePosition()) {
			//End the game, say who wins. 
		};
		
		
	}
	
	 private static void makeMoveDisplay(char[] cbuf) {
		// TODO Auto-generated method stub
		
	}

	static Boolean isPrimitivePosition() {
		return false;
		// TODO Auto-generated method stub
		
	}

	private static ArrayList<String> generateMoves() {
		ArrayList<String> possibleMoves = new ArrayList<String>();
		return possibleMoves;
		
	}
	
	private static String changeTurn() {
		// TODO Auto-generated method stub
		if (whoseMove == "Player 1") whoseMove = "Player 2";
		else whoseMove = "Player 1";
		return whoseMove;
	}
	

	/** Prints the board. */
	public static void printBoard() {
		System.out.print("4  ");
		for (int i = 0; i < Board.length; i++) {
			System.out.print(Board[i][3]);
		}
		System.out.println();
		
		System.out.print("3  ");
		for (int i = 0; i < Board.length; i++) {
			System.out.print(Board[i][2]);
		}
		System.out.println();
		
		System.out.print("2  ");
		for (int i = 0; i < Board.length; i++) {
			System.out.print(Board[i][1]);
		}
		System.out.println();
		
		System.out.print("1  ");
		for (int i = 0; i < Board.length; i++) {
			System.out.print(Board[i][0]);
		}
		System.out.println();
		
		System.out.println("   a  b  c  d\n");		
	}
	
	
	
	public QuickCross(Configuration conf) {
		super(conf);
		// TODO Auto-generated constructor stub
	}

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
	public boolean unmakeMove() {
		// TODO Auto-generated method stub
		return false;
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
	public boolean makeMove() {
		// TODO Auto-generated method stub
		return false;
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
