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
		//  2     [4032,...]
		//  ...
		//  63    [64,...]    ]
	
	private String[] stringMoves; //only for testing.

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
		if (n < 0 || k < 0)
			throw new Error("Trying to take combination of a negative number");
		if (n < k)
			return 0;
		if (n == k || k == 0)
			return 1;
		if (n==0)
			throw new Error("Trying to take combination of 0");
		long answer = 1;
		for (int x = n; x>n-k; x--)
			answer *= x;
		for (int x = k; x>1; x--)
			answer /= x;
		return (int) answer;
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
		stringMoves = new String[0]; //only for testing.
		
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
			offsetTable[tier][0] = combo; //* combination(tier,0);
			for (int offset = 1;offset < tier + 1;offset++) {
				offsetTable[tier][offset] = offsetTable[tier][offset-1] + combo; //* combination(tier,offset);
			}
		}
		
		hash = hash(stateToString()); //not right?
	}

	@Override
	public void setState(TierState pos) {
		unhash(pos.tier+1, pos.hash);
		isChildrenValid = false;
		//not done. remember to set hash.
	}
	
	private long hash(String pos) {
		//System.out.println(pos);
		if (pos.length() != boardSize)
			throw new Error("Wrong length: "+ pos.length() + " != " + boardSize);
		long majorHash = 0L;
		long minorHash = 0L;
		int pieceCounter = 0;
		int whiteCounter = 0;
		for (int index = 0;index<pos.length();index++) {
			if (pos.charAt(index) != ' ') {
				pieceCounter++;
				if (pos.charAt(index) == 'O') {
					whiteCounter++;
				}
			}
		}
		int dummyPieceCounter = pieceCounter;
		int dummyWhiteCounter = whiteCounter;
		for (int index = pos.length()-1;index >= 0;index--) {
			if (pos.charAt(index) != ' ') {
				majorHash += combination(index+1,dummyPieceCounter);
				if (pos.charAt(index) == 'O') {
					minorHash += combination(dummyPieceCounter,dummyWhiteCounter);
					dummyWhiteCounter--;
				}
				dummyPieceCounter--;
			}
		}
		//System.out.println(pos);
		System.out.println(majorHash+","+minorHash+"|"+whiteCounter+","+pieceCounter);
		return (majorHash + offsetTable[pieceCounter][whiteCounter]) * (int) Math.pow(2, pieceCounter) + minorHash;
	}
	
	private void unhash(int tier, long hash) {
		this.hash = hash;
		long majorHash = -1;
		long minorHash = hash % (int) Math.pow(2, tier);
		int numWhite = 0;
		for (int index = 0; index < tier+1; index++) {
			if ((hash - minorHash) / (int) Math.pow(2, tier) < offsetTable[tier][index]) {
				majorHash = ((hash - minorHash) / (int) Math.pow(2,tier)) - offsetTable[tier][index-1];
				numWhite = index-1;
				break;
			}
		}
		System.out.println(hash);
		System.out.println(majorHash+","+minorHash+","+tier+","+numWhite+"|hash="+hash);
		if (majorHash == -1)
			throw new Error("Given hash is not in the tier");
		//creating the order of pieces of be put in from minorHash
		char[] orderOfPieces = new char[tier];
		for (int orderIndex = tier-1; orderIndex >= 0; orderIndex--) {
			int combo = combination(orderIndex+1,numWhite);
			if (minorHash >= combo) {
				minorHash -= combo;
				numWhite--;
				orderOfPieces[orderIndex] = 'O';
			} else {
				orderOfPieces[orderIndex] = 'X';
			}
		}
		//putting pieces in from the major hash, order specified by orderOfPieces array above
		int currentTier = tier;
		for (int pieceIndex = boardSize-1; pieceIndex >= 0; pieceIndex--) {
			int combo = combination(pieceIndex+1,currentTier);
			System.out.println(majorHash+","+combo+","+(pieceIndex+1)+","+currentTier);
			if (majorHash >= combo) {
				char test = orderOfPieces[currentTier-1];
				board[pieceIndex / width][pieceIndex % height].setPiece(test);
				currentTier--;
				majorHash -= combo;
			}
		}
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
		if (pos.length() != boardSize+1)
			throw new Error("Bad String - wrong length: " + pos.length() + " != " + boardSize);
		else {
			for (int row = 0; row < height; row++) {
				for (int col = 0; col < width; col++) {
					board[row][col].setPiece(pos.charAt(row * width + col));
				}
			}
		}
		hash = hash(stateToString()); //setting initial hash
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
		for (int x = 0; x < width; x++) {
			for (int y = 0; y< height; y++) {
				board[x][y].setPiece(' ');
			}
		}
		board[height / 2 - 1][width / 2 - 1].setPiece('X');
		board[height / 2][width / 2 - 1].setPiece('O');
		board[height / 2 - 1][width / 2].setPiece('O');
		board[height / 2][width / 2].setPiece('X');
		
		turn = BLACK;
		isChildrenValid = false;
		hash = hash(stateToString()); //not right?
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
		stringMoves = new String[0]; //only for testing.
		//looks at every spot on the board
		for (int boardNumber = 0;boardNumber < boardSize; boardNumber++) {
			//only a valid child if there is an empty space there
			if (pieces[boardNumber] == ' ') {
				int[] childrenNumbers = {boardNumber-width-1,boardNumber-width,boardNumber-width+1,boardNumber-1,boardNumber+1,boardNumber+width-1,boardNumber+width,boardNumber+width+1};
				//looks at each spot around the given spot
				for (int index = 0;index < childrenNumbers.length; index++) {
					//makes sure the spot is within bounds, then checks if there is an opposing piece next to it
					if (((childrenNumbers[index] >= 0 && childrenNumbers[index] < boardSize)) && (((turn == WHITE && pieces[childrenNumbers[index]] == 'X') || (turn == BLACK && pieces[childrenNumbers[index]] == 'O')) && isFlippable(boardNumber, index, false, null))) {
						String[] newStringMoves = new String[stringMoves.length+1]; //only for testing.
						System.arraycopy(stringMoves,0,newStringMoves,0,stringMoves.length); //only for testing.
						newStringMoves[newStringMoves.length-1] = ""+(boardNumber / width)+""+(boardNumber % height); //only for testing.
						stringMoves = newStringMoves; //only for testing.
						String[] stringState = {stateToString()};
						boolean x = isFlippable(boardNumber,index,true,stringState);
						TierState[] newChildren = new TierState[children.length + 1];
						System.arraycopy(children, 0, newChildren, 0, children.length);
						newChildren[newChildren.length-1] = new TierState(getTier()+1,hash(stringState[0]));
						children = newChildren;
						break;
					}
				}
			}
		}
		isChildrenValid = true;
	}
	
	private boolean isFlippable(int boardNumber, int direction, boolean flip, String[] state) {
		int addRow = 0; int addCol = 0;
		switch (direction) {
		case 0: addRow=-1; addCol=-1; break;
		case 1: addRow=-1; addCol=0; break;
		case 2: addRow=-1; addCol=1; break;
		case 3: addRow=0; addCol=-1; break;
		case 4: addRow=0; addCol=1; break;
		case 5: addRow=1; addCol=-1; break;
		case 6: addRow=1; addCol=0; break;
		case 7: addRow=1; addCol=1; break;
		default: throw new Error("Bad direction");
		}
		int row = boardNumber / width;
		int col = boardNumber % height;
		if (flip) {
			state[0] = "" + state[0].substring(0,row*width+col)+(turn == BLACK ? 'X' : 'O')+state[0].substring(row*width+col+1,state[0].length());
		}
		char piece = 'P';
		row += addRow;
		col += addCol;
		int pieceCounter = 0;
		while (col>=0 && col<width && row>=0 && row<height) {
			piece = (flip ? state[0].charAt(row*width+col) : board[row][col].getPiece());
			if (piece == ' ' || (pieceCounter == 0 && piece == (turn==BLACK ? 'X' : 'O')))
				break;
			else {
				if (flip)
					state[0] = "" + state[0].substring(0,row*width+col)+(turn == BLACK ? 'X' : 'O')+state[0].substring(row*width+col+1,state[0].length());
				if (pieceCounter > 0 && piece == (turn == BLACK ? 'X' : 'O')) {
					if (flip) {
						for (int direct = 0;direct<8;direct++) {
							if (direct != direction && isFlippable(boardNumber, direct, false, null))
								isFlippable(boardNumber, direct, true, state);
						}
					}
					return true;
				}
			row += addRow;
			col += addCol;
			if (piece == (turn==BLACK ? 'O' : 'X'))
				pieceCounter++;
			}
		}
		return false;
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
	
	public void changeTurn() {
		if (this.turn==BLACK)
			this.turn = WHITE;
		else if (this.turn==WHITE)
			this.turn = BLACK;
	}
	
	public String getTurn() {
		if (turn==BLACK)
			return "BLACK TO MOVE";
		else if (turn==WHITE)
			return "WHITE TO MOVE";
		return "";
	}
	
	public String[] getStringMoves() {
		return stringMoves;
	}
	
	public static void main(String[] args) {
		Reversi reversiGame = null;
		try {
			reversiGame = new Reversi(new Configuration(args[0]));
		}
		catch (ClassNotFoundException c) {
			throw new Error(c);
		}
		String input;
		while (true) {
			System.out.println(reversiGame.getTurn());
			System.out.println(reversiGame.displayState());
			System.out.println(reversiGame.primitiveValue().toString());
			TierState[] moves = new TierState[16];
			int y = reversiGame.validMoves(moves);
			System.out.print("Valid Moves: ");
			for (int x=0;x<y;x++) {
				System.out.print(reversiGame.getStringMoves()[x]+",");
			}
			System.out.println();
			System.out.print("Hashes: ");
			for (int x=0;x<y;x++) {
				System.out.print(moves[x].hash+",");
			}
			System.out.println();
			//System.out.println(reversiGame.turn);
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			try {
				input = br.readLine();
			} catch (IOException e) {
				throw new Error(e);
			}
			int index = 0;
			for (index=0;index<reversiGame.getStringMoves().length;index++)
				if(reversiGame.getStringMoves()[index].equals(input))
					break;
			if (index == reversiGame.getStringMoves().length) {
				System.out.println("Invalid Move");
				break;
			}
			reversiGame.setState(new TierState(reversiGame.getTier(),new Long(moves[index].hash)));
			reversiGame.changeTurn();
		}
		
/**
		while (true) {
			if (input.equals("quit"))
				break;
		    reversiGame.setFromString(input);
			System.out.println(reversiGame.displayState());
			System.out.println(reversiGame.primitiveValue());
			System.out.println(reversiGame.getHash());
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
*/
	}

}
