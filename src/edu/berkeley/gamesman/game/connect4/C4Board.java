package edu.berkeley.gamesman.game.connect4;

import java.util.Arrays;

import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Util;

/**
 * Represents a board of pieces
 * @author DNSpies
 * @author Steven Schlansker
 */
public final class C4Board {
	private C4Piece[][] stateArr; // states are in state[row][col] order
	private C4Piece turn;
	private Integer[] columnHeightsArr; // Integer, not int so that it's
	
	private final int width,height;

	// initialized to null, not zero

	/**
	 * @param gameWidth The width of the board
	 * @param gameHeight The height of the board
	 */
	public C4Board(int gameWidth, int gameHeight) {
		
		width = gameWidth;
		height = gameHeight;
		
		stateArr = new C4Piece[width][height];
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				put(stateArr,row,col,C4Piece.EMPTY);
			}
		}
		columnHeightsArr = new Integer[width];
		for (int col = 0; col < gameWidth; col++)
			columnHeightsArr[col] = 0;
		turn = C4Piece.RED;
	}

	/**
	 * @param state A piece array representing the board
	 */
	public C4Board(C4Piece[][] state) {
		stateArr = state;
		width = state.length;
		height = state[0].length;
		columnHeightsArr = new Integer[width];
	}

	/**
	 * @param state A piece array representing the board
	 * @param turn Who's turn it is currently
	 */
	public C4Board(C4Piece[][] state, C4Piece turn) {
		stateArr = state;
		this.turn = turn;
		height = state.length;
		width = state[0].length;
		columnHeightsArr = new Integer[width];
	}

	/**
	 * @param state A piece array representing the connect 4 board
	 * @param turn Who's turn it is currently
	 * @param columnHeights The respective heights of each column as ints
	 */
	public C4Board(C4Piece[][] state, C4Piece turn, int[] columnHeights) {
		this(state, turn, addIntWrappers(columnHeights));
	}

	private static Integer[] addIntWrappers(int[] columnHeights) {
		Integer[] ch = new Integer[columnHeights.length];
		for (int i = 0; i < columnHeights.length; i++) {
			ch[i] = columnHeights[i];
		}
		return ch;
	}

	/**
	 * @param state A piece array representing the connect 4 board
	 * @param turn Who's turn it is currently
	 * @param columnHeights The respective heights of each column as Integers
	 */
	public C4Board(C4Piece[][] state, C4Piece turn, Integer[] columnHeights) {
		stateArr = state;
		this.turn = turn;
		width = state.length;
		height = state[0].length;
		columnHeightsArr = columnHeights;
		Util.assertTrue(columnHeights.length == width, "Column heights array wrong size");
	}

	/**
	 * @param board The board pieces stored as chars
	 */
	public C4Board(char[][] board) {
		this(convertCharBoard(board));
	}
	
	private static C4Piece[][] convertCharBoard(char[][] charBoard){
		C4Piece[][] board=new C4Piece[charBoard.length][charBoard[0].length];
		for(int row=0;row<charBoard[0].length;row++)
			for(int col=0;col<charBoard.length;col++)
				put(board,row,col,C4Piece.toPiece(get(charBoard,row,col)));
		return board;
	}

	/**
	 * @return Who's turn it is currently
	 */
	public C4Piece getTurn() {
		int col, numPieces = 0;
		if (turn == null) {
			for (col = 0; col < width; col++) {
				numPieces += getHeight(col);
			}
			turn = numPieces % 2 == 0 ? C4Piece.RED : C4Piece.BLACK;
		}
		return turn;
	}
	
	/**
	 * @return The player who played last
	 * Equivalent to getTurn().opposite()
	 */
	public C4Piece lastPlayed(){
		return getTurn().opposite();
	}

	/**
	 * @param col The column number
	 * @return The current number of pieces in that column
	 */
	public int getHeight(int col) {
		if (columnHeightsArr[col] == null) {
			for (int i = 0; i < height; i++) {
				if (get(stateArr,i,col).equals(C4Piece.EMPTY)) {
					columnHeightsArr[col] = i;
					Util.assertTrue(columnHeightsArr[col] <= height,"Column too tall!");
					return i;
				}
			}
			columnHeightsArr[col] = height;
			Util.assertTrue(columnHeightsArr[col] <=height,"Column too tall!");
			return height;
		}
		Util.assertTrue(columnHeightsArr[col] <= height,"Column too tall!");
		return columnHeightsArr[col];
	}

	/**
	 * Return primitive "value".
	 * Usually this value includes WIN, LOSE, and perhaps TIE
	 * Return UNDECIDED if this is not a primitive state (shouldn't usually be called)
	 * @param piecesToWin The number of pieces in a row necessary to win
	 * @return the Record representing the state
	 * @see edu.berkeley.gamesman.core.Record
	 */
	public PrimitiveValue primitiveValue(int piecesToWin) {
		int colHeight;
		boolean moreMoves = false;
		for (int col = 0; col < width; col++) {
			colHeight = getHeight(col);
			if (!moreMoves && colHeight < height)
				moreMoves = true;
			if (colHeight > 0 && get(stateArr, colHeight - 1, col) == lastPlayed()
					&& checkLastWin(colHeight - 1,col, piecesToWin))
				return PrimitiveValue.LOSE;
		}
		if (moreMoves)
			return PrimitiveValue.UNDECIDED;
		return PrimitiveValue.TIE;
	}

	private boolean checkLastWin(int row, int col, int piecesToWin) {
		C4Piece turn = get(row, col);
		Util.debug(DebugFacility.GAME,turn," last played in column ",col,", checking for win");
		int ext;

		// Check horizontal win
		ext = 1;
		for (int i = col-1; i >= 0; i--)
			if (get(row, i) == turn)
				ext++;
			else
				break;
		for (int i = col+1; i < width; i++)
			if (get(row, i) == turn)
				ext++;
			else
				break;
		if (ext >= piecesToWin)
			return true;

		// Check DownLeft/UpRight Win
		ext = 1;
		for (int i = 1; row-i >= 0 && col-i >= 0; i++)
			if (get(row - i, col - i) == turn)
				ext++;
			else
				break;
		for (int i = 1; row+i < height && col+i < width; i++)
			if (get(row + i, col + i) == turn)
				ext++;
			else
				break;
		if (ext >= piecesToWin)
			return true;

		// Check UpLeft/DownRight Win
		ext = 1;
		for (int i = 1; row+i < height && col-i >= 0; i++)
			if (get(row + i, col - i) == turn)
				ext++;
			else
				break;
		for (int i = 1; row-i >= 0 && col+i < width; i++)
			if (get(row - i, col + i) == turn)
				ext++;
			else
				break;
		if (ext >= piecesToWin)
			return true;

		// Check Vertical Win: Since it's assumed x,y is on top, it's only
		// necessary to look down, not up
		if (row >= piecesToWin - 1)
			for (ext = 1; ext < piecesToWin && row-ext >= 0; ext++)
				if (get(row - ext,col) != turn)
					break;
		if (ext >= piecesToWin)
			return true;
		return false;
	}

	/**
	 * @param moveCol The column in which to move
	 * @return A new Board representing the state after this move has been made
	 *         or null if the column is full
	 */
	public C4Board makeMove(int moveCol) {
		if (getHeight(moveCol) == height)
			return null;
		C4Piece[][] newState = new C4Piece[width][height];
		Integer[] newHeight = new Integer[width];
		for (int row = 0; row < height; row++)
			for (int col = 0; col < width; col++)
				put(newState,row,col,get(stateArr,row,col));
		put(newState,getHeight(moveCol),moveCol,getTurn()); // This also ensures
															// moveCol's height
															// is already
															// initialized
		for (int col = 0; col < width; col++)
			newHeight[col] = columnHeightsArr[col];
		newHeight[moveCol]++;
		return new C4Board(newState, getTurn().opposite(), newHeight);
	}

	/**
	 * @return A char-array representation of the board
	 */
	public char[][] getCharBoard() {
		char[][] resBoard = new char[width][height];
		for (int row = 0; row < height; row++)
			for (int col = 0; col < width; col++)
				put(resBoard,row,col,get(row,col).toChar());
		return resBoard;
	}

	
	
	/**
	 * @param row The piece's row
	 * @param col The piece's column
	 * @return The piece
	 */
	public C4Piece get(int row, int col) {
		return get(stateArr,row,col);
	}
	
	@Override
	public String toString(){
		return Arrays.deepToString(getCharBoard())+" ("+lastPlayed()+")";
	}
	
	protected final static char get(char[][] b, int row, int col){
		return b[col][row];
	}
	
	protected final static void put(char[][] b, int row, int col, char c){
		b[col][row] = c;
	}
	
	protected final static C4Piece get(C4Piece[][] b, int row, int col){
		return b[col][row];
	}
	
	protected final static void put(C4Piece[][] b, int row, int col, C4Piece c){
		b[col][row] = c;
	}
}
