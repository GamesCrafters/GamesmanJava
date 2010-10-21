package edu.berkeley.gamesman.game;

import java.io.*;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.game.util.TierState;
import edu.berkeley.gamesman.hasher.DartboardHasher;
import edu.berkeley.gamesman.util.Pair;

public class Reversi extends TierGame {
	private final int width;
	private final int height;
	private final int boardSize;
	private final Cell[][] board;
	private final char[] pieces;
	private int numPieces = 0;
	private final DartboardHasher dbh;
	private int turn; //1 for black, 0 for white.
	private final static int BLACK = 1;
	private final static int WHITE = 0;
	private TierState[] children;
	private boolean isChildrenValid;
	private long hash;
	private final long[][] offsetTable;
		//offsetTable is a table of offsets based on the tier, or number of pieces
		//and each element is an array of all possible ways to order the board with
		//increasing number of white vs black pieces(first element is ways to order
		//the board with 0 white pieces, second is with 1 white piece, etc...)
		// tier
		//  0   [ [1]
		//  1     [64,128]
		//  2     [2016,...]
		//  ...
		//  63    [64,...]    ]

	private class Cell {
		final int row, col;
		final int boardNum;

		public Cell(int row, int col, int boardNum) {
			this.row = row;
			this.col = col;
			this.boardNum = boardNum;
			pieces[boardNum] = ' ';
		}

		public char getPiece() {
			return pieces[boardNum];
		}

		public void setPiece(char p) {
			if (pieces[boardNum] != ' ' && p == ' ')
				numPieces--;
			else if (pieces[boardNum] == ' ' && p != ' ')
				numPieces++;
			if (!(p == ' ' || p == 'X' || p == 'O'))
				throw new Error("Bad piece: " + p);
			pieces[boardNum] = p;
		}
	}
	
	//number of ways to reorder white and black number of pieces
	private int combination(int n, int k) {
		if (n < k)
			return 0;
		if (n == k || n == 1 || k == 1)
			return 1;
		return combination(n-1,k-1) + combination(n-1,k);
	}

	public Reversi(Configuration conf) {
		super(conf);
		width = conf.getInteger("gamesman.game.width", 8);
		height = conf.getInteger("gamesman.game.height", 8);
		boardSize = width * height;
		pieces = new char[boardSize];
		board = new Cell[height][width];
		dbh = new DartboardHasher(boardSize, ' ', 'O', 'X');
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				board[row][col] = new Cell(row, col, row * width + col);
			}
		}
		board[height / 2 - 1][width / 2 - 1].setPiece('X');
		board[height / 2][width / 2 - 1].setPiece('O');
		board[height / 2 - 1][width / 2].setPiece('O');
		board[height / 2][width / 2].setPiece('X');
		
		turn = BLACK;
		isChildrenValid = false;
		children = new TierState[0];
		hash = 0L; //not right.
		
		offsetTable = new long[boardSize+1][];
		//initialize offset table
		for (int tier = 0;tier < boardSize;tier++) {
			//number of ways to put tier number of pieces on the board
			long combo = 1;
			for(int x = boardSize;x > boardSize - tier;x--) {
				combo *= x;
			}
			for(int x = tier;x > 0;x--) {
				combo /= x;
			}
						
			offsetTable[tier] = new long[tier+1];
			offsetTable[tier][0] = combo * combination(tier,1);
			for (int offset = 1;offset < tier + 1;offset++) {
				offsetTable[tier][offset] = offsetTable[tier][offset-1] + combo * combination(tier,offset);
			}
		}
	}

	@Override
	public void setState(TierState pos) {
		numPieces = pos.tier;
		//not done. remember to set hash.
	}

	@Override
	public Value primitiveValue() {
		Value value = Value.UNDECIDED;
		if (!(isChildrenValid))
			getChildren();
		if ((isChildrenValid && children.length == 0)) {
			int black = 0;
			int white = 0;
			for (int boardNumber = 0;boardNumber < boardSize;boardNumber++) {
				if (pieces[boardNumber] == 'O')
					white++;
				if (pieces[boardNumber] == 'X')
					black++;
			}
			if (black == white) {
				value = Value.TIE;
			} else if ((turn == BLACK && black > white) || (turn == WHITE && white > black)) {
				value = Value.WIN;
			} else if ((turn == BLACK && black < white) || (turn == WHITE && white < black)) {
				value = Value.LOSE;
			}
		}
		return value;
	}

	@Override
	public Collection<Pair<String, TierState>> validMoves() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getTier() {
		return numPieces;
	}
	
	public long getHash() {
		return hash;
	}

	@Override
	public String stateToString() {
		String answer = "";
		for (int boardNumber = 0;boardNumber < boardSize;boardNumber++) {
			answer += pieces[boardNumber];
		}
		return answer;
	}

	@Override
	public void setFromString(String pos) {
		if (pos.length() != boardSize)
			throw new Error("Bad String: wrong length");
		else {
			for (int row = 0; row < height; row++) {
				for (int col = 0; col < width; col++) {
					board[row][col].setPiece(pos.charAt(row * width + col));
				}
			}
		}
		//need to set hash
	}

	@Override
	public void getState(TierState state) {
		state.tier = getTier();
		state.hash = getHash();
	}

	@Override
	public long numHashesForTier(int tier) {
		return offsetTable[tier][offsetTable[tier].length - 1];
	}

	@Override
	public String displayState() {
		String s = stateToString();
		StringBuilder str = new StringBuilder((width + 3) * height);
		for (int row = 0; row<height; row++) 
			str.append("|" + s.substring(row*width,row*width+width) + "|\n");
		return str.toString();
	}

	@Override
	public void setStartingPosition(int n) {
		for (int boardNumber = 0;boardNumber < boardSize - 1;boardNumber++) {
			pieces[boardNumber] = ' ';
		}
		board[height / 2 - 1][width / 2 - 1].setPiece('X');
		board[height / 2][width / 2 - 1].setPiece('O');
		board[height / 2 - 1][width / 2].setPiece('O');
		board[height / 2][width / 2].setPiece('X');
		
		turn = BLACK;
		isChildrenValid = false;
		hash = 0L; //not right.
	}

	@Override
	public int numStartingPositions() {
		return 1;
	}

	@Override
	public boolean hasNextHashInTier() {
		long currentHash = dbh.getHash();
		return (currentHash	!= numHashesForTier(getTier()) - 1);
	}

	@Override
	public void nextHashInTier() {
		if (this.hasNextHashInTier()) {
			TierState transition = new TierState();
			getState(transition);
			transition.hash++;
			setState(transition);
		} else
			throw new Error("End of Tier");
	}

	@Override
	public int numberOfTiers() {
		return boardSize + 1;
	}

	@Override
	public int maxChildren() {
		return boardSize;
	}

	@Override
	public int validMoves(TierState[] moves) {
		if (!(isChildrenValid))
			getChildren();
		System.arraycopy(children, 0, moves, 0, children.length);
		return children.length;
	}
	
	private void getChildren() {
		children = new TierState[0];
		//looks at every spot on the board
		for (int boardNumber = 0;boardNumber < boardSize; boardNumber++) {
			//only a valid child if there is an empty space there
			if (pieces[boardNumber] == ' ') {
				int[] childrenNumbers = {boardNumber-width-1,boardNumber-width,boardNumber-width+1,boardNumber-1,boardNumber+1,boardNumber+width-1,boardNumber+width,boardNumber+width+1};
				//looks at each spot around the given spot
				for (int index = 0;index < childrenNumbers.length; index++) {
					//makes sure the spot is within bounds, then checks if there is an opposing piece next to it
					if (((childrenNumbers[index] > 0 && childrenNumbers[index] < boardSize)) && (((turn == WHITE && pieces[childrenNumbers[index]] == 'X') || (turn == BLACK && pieces[childrenNumbers[index]] == 'O')) && isFlippable(boardNumber, index))) {
						TierState[] newChildren = new TierState[children.length + 1];
						System.arraycopy(children, 0, newChildren, 0, children.length);
						newChildren[newChildren.length-1] = flipPieces(childrenNumbers[index]);
						children = newChildren;
						break;
					}
				}
			}
		}
		isChildrenValid = true;
	}
	
	private boolean isFlippable(int boardNumber, int direction) {
		int addx = 0; int addy = 0;
		switch (direction) {
		case 0: addx=-1; addy=-1; break;
		case 1: addx=0; addy=-1; break;
		case 2: addx=1; addy=-1; break;
		case 3: addx=-1; addy=0; break;
		case 4: addx=1; addy=0; break;
		case 5: addx=-1; addy=1; break;
		case 6: addx=0; addy=1; break;
		case 7: addx=1; addy=1; break;
		}
		int x = boardNumber % width;
		int y = boardNumber / width;
		char piece = 'P';
		while (x>=0 && x<width && y>=0 && y<height && piece!=' ') {
			piece = board[x][y].getPiece();
			if ((turn == BLACK && piece == 'O') || (turn == WHITE && piece == 'X'))
				return true;
			x += addx;
			y += addy;
		}
		return false;
	}

	private TierState flipPieces(int boardNumber) {
		return new TierState();
	}

	@Override
	public String describe() {
		return width + "x" + height + " Reversi";
	}

	@Override
	public long recordStates() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void longToRecord(TierState recordState, long record, Record toStore) {
		// TODO Auto-generated method stub

	}

	@Override
	public long recordToLong(TierState recordState, Record fromRecord) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public static void main(String[] args) {
		Reversi reversiGame = null;
		try {
			reversiGame = new Reversi(new Configuration("../gamesman-java/jobs/Reversi4x4.job"));
		}
		catch (ClassNotFoundException c) {
			throw new Error(c);
		}
		String input = "     XO  OX     ";
		while (true) {
			if (input.equals("quit"))
				break;
		    reversiGame.setFromString(input);
			System.out.println(reversiGame.displayState());
			System.out.println(reversiGame.primitiveValue());
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		    try {
		    	input = br.readLine();
		    }
		    catch (IOException e) {
		    	throw new Error(e);
		    }
		    
		}
		reversiGame.setStartingPosition(1);
		System.out.println(reversiGame.displayState());
	}

}
