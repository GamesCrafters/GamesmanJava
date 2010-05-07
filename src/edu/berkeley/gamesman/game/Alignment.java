package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;



import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Game;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.game.util.AlignmentState;
import edu.berkeley.gamesman.hasher.AlignmentHasher;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;



/**
 * @author Aloni & Brent
 *
 */
public class Alignment extends Game<AlignmentState> {
	private int gameWidth, gameHeight;
	public int piecesToWin; 
	private AlignmentVariant variant; //should be an enum?
	public ArrayList<Pair<Integer, Integer>> openCells;

	@Override
	public void initialize(Configuration conf) {
		super.initialize(conf);
		openCells = new ArrayList<Pair<Integer,Integer>>();
		gameWidth = conf.getInteger("gamesman.game.width", 3);
		gameHeight = conf.getInteger("gamesman.game.height", 3);
		piecesToWin = conf.getInteger("gamesman.game.pieces", 5);

		variant = AlignmentVariant.getVariant(conf.getInteger("gamesman.game.variant", 2)); 

		for (int row = 0; row < gameHeight; row++) {
			for (int col = 0; col < gameWidth; col++) {

				openCells.add(new Pair<Integer, Integer>(row, col));
			}
		}
		//Removing corners
		/* Not compatible with AlignmentState and this removal is incorrect.
		if (gameWidth > 4 && gameHeight > 4) {
			openCells.remove(0); openCells.remove(1); openCells.remove(gameWidth);
			openCells.remove(gameWidth-1); openCells.remove(gameWidth-2); openCells.remove(2*gameWidth - 1);
			openCells.remove((gameHeight-1)*gameWidth); openCells.remove((gameHeight-2)*gameWidth); openCells.remove((gameHeight-1)*gameWidth + 1);
			openCells.remove((gameHeight-1)*gameWidth - 1); openCells.remove((gameHeight)*gameWidth - 1); openCells.remove((gameHeight)*gameWidth - 2);
		}
		 */
	}

	@Override
	public String describe() {
		return "Alignment: " + gameWidth + "x" + gameHeight + " " + piecesToWin
		+ " captures " + variant;
	}

	@Override
	public String displayState(AlignmentState pos) {
		StringBuilder board = new StringBuilder(2 * (gameWidth + 2) * gameHeight );
		int row = 0;
		for (; row < gameHeight; row++) {
			for (int col = 0; col < gameWidth; col++) {
				board.append(pos.get(row, col) + " ");
			}
		}
		for (row = 0; row < gameHeight; row++) {
			board.replace((2*gameWidth*(row+1) - 1), (2*gameWidth*(row+1)), "\n"); //is this correct?
		}
		board.append("xDead: " + pos.xDead + " oDead: " + pos.oDead + " " + opposite(pos.lastMove) + "\'s turn");
		return board.toString();
	}

	@Override
	public void hashToState(long hash, AlignmentState s) {
		((AlignmentHasher) conf.getHasher()).unhash(hash, s);
		System.out.println("The newest state is " + stateToString(s));
	}

	@Override
	public int maxChildren() {
		if (variant == AlignmentVariant.NO_SLIDE) {
			return gameWidth*gameHeight;
		}
		else {return gameHeight*65;}

	}

	@Override
	public AlignmentState newState() {
		return new AlignmentState(new char[gameHeight][gameWidth], 0, 0, 'O');
	}

	@Override
	public long numHashes() {
		return conf.getHasher().numHashes();
	}

	@Override
	public PrimitiveValue primitiveValue(AlignmentState pos) {
		if (pos.lastMove == 'X') {
			if (pos.oDead >= piecesToWin) {
				return PrimitiveValue.LOSE;
			}
			if (pos.xDead >= piecesToWin) {
				return PrimitiveValue.WIN;
			}
			if (pos.full()) {
				return PrimitiveValue.TIE;
			}
			else {
				return PrimitiveValue.UNDECIDED;
			}
		}
		if (pos.lastMove == 'O') {
			if (pos.xDead >= piecesToWin) {
				return PrimitiveValue.LOSE;
			}
			if (pos.oDead >= piecesToWin) {
				return PrimitiveValue.WIN;
			} 
			if (pos.full()) {
				return PrimitiveValue.TIE;
			} else {
				return PrimitiveValue.UNDECIDED;
			}
		} else {
			throw new IllegalArgumentException("Last move cannot be " + pos.lastMove);
		}
	}
	@Override
	public Collection<AlignmentState> startingPositions() {
		AlignmentState as = newState();
		for (Pair<Integer, Integer> place : openCells)
			as.put(place.car,place.cdr,' ');
		ArrayList<AlignmentState> retVal = new ArrayList<AlignmentState>(1);
		retVal.add(as);
		return retVal;
	}

	@Override
	public long stateToHash(AlignmentState pos) {
		return ((AlignmentHasher) conf.getHasher()).hash(pos);
	}

	@Override
	public String stateToString(AlignmentState pos) {
		StringBuilder board = new StringBuilder(2 * (gameWidth + 2) * gameHeight );
		for (int row = 0; row < gameHeight; row++) {
			for (int col = 0; col < gameWidth; col++) {
				board.append(pos.get(row, col));
			}
		}
		board.append(pos.xDead + ":" + pos.lastMove + ":" + pos.oDead);
		return board.toString();
	}

	@Override
	public AlignmentState stringToState(String pos) {
		char[][] board = new char[gameHeight][gameWidth];
		int xDead, oDead;
		char lastMove;
		int square = 0;
		for (int row = 0; row < gameHeight; row++) {
			for (int col = 0; col < gameWidth; col++) {
				board[row][col] = pos.charAt(square);
				square++;
			}
		}
		String[] auxData = pos.substring(gameWidth * gameHeight).split(":");
		xDead = Integer.parseInt(auxData[0]); oDead = Integer.parseInt(auxData[2]);
		lastMove = auxData[1].charAt(0);
		return new AlignmentState(board, xDead, oDead, lastMove);
	}

	@Override
	public Collection<Pair<String, AlignmentState>> validMoves(AlignmentState pos) {
		AlignmentState s = new AlignmentState(pos);
		Collection<String> strings = new ArrayList<String>();
		Collection<AlignmentState> states = new ArrayList<AlignmentState>();
		if (variant == AlignmentVariant.STANDARD) {
			throw new UnsupportedOperationException ("STANDARD variant not complete");
		}
		else if (variant == AlignmentVariant.NO_SLIDE) {
			for (int row = 0; row < gameHeight; row++) {
				for (int col = 0; col < gameWidth; col++) {
					if (' ' == pos.get(row, col)) {
						s.put(row, col, opposite(pos.lastMove));
						strings.add("row: " + row + " col: " + col);
						states.add(new AlignmentState(s));
					}
				}
			}
			return Pair.zip(strings, states);
		}
		else if (variant == AlignmentVariant.DEAD_SQUARES) {
			throw new UnsupportedOperationException ("DEAD_SQUARES variant not complete");
		}

		return null;
	}

	@Override
	public int validMoves(AlignmentState pos, AlignmentState[] children) {
		int moves = 0;
		if (variant == AlignmentVariant.STANDARD) {
			throw new UnsupportedOperationException ("STANDARD variant not complete");
		}
		else if (variant == AlignmentVariant.NO_SLIDE) {
			for (int row = 0; row < gameHeight; row++) {
				for (int col = 0; col < gameWidth; col++) {
					if (' ' == pos.get(row, col)) {
						children[moves].set(pos);
						children[moves].put(row, col, opposite(pos.lastMove)); 
						children[moves].setLastMove(opposite(pos.lastMove));
						moves++;
					}
				}
			}

		}
		else if (variant == AlignmentVariant.DEAD_SQUARES) {
			throw new UnsupportedOperationException ("DEAD_SQUARES variant not complete");
		}
		System.out.printf("%c just moved\n%d moves possible", opposite(pos.lastMove), moves);
		return moves;

	}

	static char opposite(char player) {
		switch(player) {
		case('X'):
			return 'O';
		case('O'):
			return 'X';
		default:
			return player;
		}
	}

	int getWidth() {
		return gameWidth;
	}

	int getHeight() {
		return gameHeight;
	}








	//=======================================================================================


	// Will never be called on a square without a non-emptcol, non-valid piece. REturns arracol of bools [N W E S]
	void checkGun(int row, int col, Boolean[] guns, AlignmentState pos) { // Catch ArrayIndexOutOfBound 
		char[][] board = pos.board;
		char base = board[row][col];
		char NW = ' ';
		char NE = ' ';
		char SW = ' ';
		char SE =  ' ';
		try {
			NW = board[row-1][col-1];
		} catch (ArrayIndexOutOfBoundsException e) {	}

		try {
			NE = board[row+1][col-1];
		} catch (ArrayIndexOutOfBoundsException e) {	}

		try {
			SW = board[row-1][col+1];
		} catch (ArrayIndexOutOfBoundsException e) {	}

		try {
			SE = board[row+1][col+1];
		} catch (ArrayIndexOutOfBoundsException e) {	}

		if (SE == base && SW == base){ guns[0] = true; }
		if (NE == base && SE == base){ guns[1] = true; }
		if (NW == base && SW == base){ guns[2] = true; }
		if (NE == base && NW == base){ guns[3] = true; }

	}

	/**
	 * Populates myBullets with all bullets of the current 
	 * player's color.  Does not alter the board in any way.
	 * 
	 * return
	 */
	void makeBullets(AlignmentState pos) {
		Boolean[] guns = new Boolean[4];
		char[][] board = pos.board;
		for (int row = 0; row < gameHeight; row++) {
			for (int col = 0; col< gameWidth; col++) {
				if (board[row][col] == opposite(pos.lastMove)) {
					checkGun(row, col, guns, pos);
					for (int dir  = 0; dir < 4; dir++) {
						if (guns[dir]) {
							myBullets.add(new Bullet(row,col,dir,opposite(pos.lastMove)));
						}
					}
				}
			}
		}

	}

	/**
	 * moves the piece at (x0,y0) to (x1, y1)
	 */
	Boolean movePiece (int row0, int col0, int row1, int col1, AlignmentState pos) {
		if (pos.legalMove(row0,col0,row1,col1)) {
			pos.board[row1][col1] = pos.board[row0][col0];
			pos.board[row0][col0] = ' ';
			return true;
		}
		return false;
	}


	/** true if the square (x0,y0) is one of 8 points adjacent to (x1, y1) */
	Boolean adjacent (int x0, int y0, int x1, int y1) {
		return (Math.abs(y1-y0)<=1 && Math.abs(x1-x0)<=1 && !(x1==x0 && y1==y0));
	}

	/** Only the guns of the player whose turn it currently is 
	 * fire at the end of a given turn.  Finds and fires guns, returning
	 * the number of enemies removed from the board. Destructive*/
	void fireGuns (AlignmentState pos) {
		makeBullets(pos);
		char whoseTurn = opposite(pos.lastMove);
		int xDead = pos.xDead;
		int oDead = pos.oDead;
		char[][] myBoard = pos.board;
		Iterator<Bullet> iter = myBullets.iterator();
		int deathCount = 0;
		while (iter.hasNext()) {
			Bullet b = iter.next();
			int row = b.row(); int col = b.col(); int drow = b.drow(); int dcol = b.dcol();
			Boolean stillGoing = true;
			while(stillGoing) {
				row += drow; col += dcol;
				if (myBoard[row][col] == whoseTurn){// Catch ArrayException
					stillGoing = false;
					continue;
				}
				else {
					if (myBoard[row][col] == whoseTurn) {
						stillGoing = false;
					}
					else if (myBoard[row][col] == opposite(whoseTurn)) {
						myBoard[row][col] = ' ';
						deathCount++;
					}

				}
			}
		}
		switch(whoseTurn) {
		case('X'):
			oDead -= deathCount;
		break;
		case('O'):
			xDead -= deathCount;
		break;
		}
		pos.set(myBoard, xDead, oDead, whoseTurn);
	}
	private ArrayList<Bullet> myBullets;




	//=======================================================================================


}

enum AlignmentVariant {
	STANDARD, NO_SLIDE, DEAD_SQUARES; //STANDARD = 1, NO_SLIDE = 2, DEAD_SQUARES = 3;

	static AlignmentVariant getVariant(int varNum) {
		switch (varNum) {
		case(1): 
			return STANDARD;
		case(2):
			return NO_SLIDE;
		case(3):
			return DEAD_SQUARES;
		default:
			throw new IllegalArgumentException("No Alignment Variant exists for number " + varNum);
		}

	}
}


class Bullet {

	Bullet(int row, int col, int dir_num, char owner) {
		this.row = row;
		this.col = col;
		this.owner = owner;
		switch(dir_num) {
		case(0):
			dir = 'n';
		break;
		case(1):
			dir = 'w';
		break;
		case(2):
			dir = 'e';
		break;
		case(3):
			dir = 's';
		break;
		}			
	}

	void set(int row, int col, int dir_num, char owner) {
		this.row = row;
		this.col = col;
		this.owner = owner;
		switch(dir_num) {
		case(0):
			dir = 'n';
		break;
		case(1):
			dir = 'w';
		break;
		case(2):
			dir = 'e';
		break;
		case(3):
			dir = 's';
		break;
		}			
	}

	char owner() {
		return owner;
	}

	int row() {
		return row;
	}

	int col() {
		return col;
	}

	int drow() {
		switch(dir) {
		case('n'):
			return -1;
		case('w'):
			return 0;
		case('e'):
			return 0;
		case('s'):
			return 1;
		default:
			throw new IllegalArgumentException("bad direction " + dir);
		}			
	}

	int dcol() {
		switch(dir) {
		case('n'):
			return 0;
		case('w'):
			return -1;
		case('e'):
			return 1;
		case('s'):
			return 0;
		default:
			throw new IllegalArgumentException("bad direction " + dir);
		}			
	}

	private int row;
	private int col;
	private char dir; //
	private char owner;
}
