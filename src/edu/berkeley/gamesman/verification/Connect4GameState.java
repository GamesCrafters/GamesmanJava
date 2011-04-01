package edu.berkeley.gamesman.verification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Value;

/**
 * Represents a <tt>GameState</tt> for a Connect 4 game.
 * 
 * @author adegtiar
 * @author rchengyuep
 */
public class Connect4GameState extends GameState {

	private static final int[][] xyComboPairs = new int[][] { { 1, 0 },
			{ 1, 1 }, { 0, 1 }, { -1, 1 } };

	private final int width;
	private final int height;
	private final int inARow;
	private final int totalPieces;

	private Connect4Piece[][] positionBoard;
	private Connect4Player player;
	private boolean isPrimitive;
	private Value stateValue;
	private int moveCount;

	/**
	 * The next row index of the next free spot for each column (or == height)
	 */
	private int[] nextRowPositions;

	public Connect4GameState(Configuration conf) {
		this(conf.getInteger("gamesman.game.width", 7), conf.getInteger(
				"gamesman.game.height", 6), conf.getInteger(
				"gamesman.game.pieces", 4));
	}

	/**
	 * Constructs a <tt>Connect4GameState</tt> at its initial position with the
	 * given width and height.
	 * 
	 * @param width
	 *            the width of the Connect 4 board.
	 * @param height
	 *            the height of the Connect 4 board.
	 * @param primitivePieceCount
	 *            the number of pieces in a row needed to win.
	 */
	private Connect4GameState(int width, int height, int primitivePieceCount) {
		this.width = width;
		this.height = height;
		this.inARow = primitivePieceCount;
		this.totalPieces = width * height;

		this.positionBoard = new Connect4Piece[width][height];
		this.player = Connect4Player.X;
		this.isPrimitive = false;
		for (int i = 0; i < width; i++) {
			Arrays.fill(positionBoard[i], Connect4Piece.BLANK);
		}
		this.nextRowPositions = new int[width];
	}

	public Iterator<String> generateChildren() {
		Set<String> childrenPositions = new HashSet<String>();
		List<Move> moves = generateMoves();

		if (isPrimitive && moves.size() > 0) {
			throw new IllegalStateException("GameState is primitive with moves");
		}

		for (Move move : moves) {
			Connect4Move connect4Move = (Connect4Move) move;
			int columnIndex = connect4Move.ordinal();
			positionBoard[columnIndex][nextRowPositions[columnIndex]] = player
					.getOppositePlayer().getPiece();
			childrenPositions.add(boardToString(positionBoard));
			positionBoard[columnIndex][nextRowPositions[columnIndex]] = Connect4Piece.BLANK;
		}
		return childrenPositions.iterator();
	}

	@Override
	public List<Move> generateMoves() {
		ArrayList<Move> validMoves = new ArrayList<Move>(width);
		if (!isPrimitive) {
			for (int column = 0; column < width; column++) {
				if (nextRowPositions[column] != height)
					validMoves.add(Connect4Move.values()[column]);
			}
		}
		return validMoves;
	}

	@Override
	public boolean isPrimitive() {
		return isPrimitive;
	}

	/**
	 * Computes the new result for whether or not the <tt>GameState</tt> is
	 * primitive after placing the given piece. Should only be called in doMove
	 * or undoMove, and has the side effect of setting the <tt>stateValue</tt>.
	 * 
	 * @param row
	 *            the row in in which the new piece is placed.
	 * @param column
	 *            the column in which the new piece is placed.
	 * @param placedPiece
	 *            the type of Connect4Piece that is placed.
	 * @return whether or not the new position is primitive;
	 */
	private boolean isPrimitive(int row, int column, Connect4Piece placedPiece) {

		int tempCount;
		for (int[] xyPair : xyComboPairs) {
			int xDir = xyPair[0];
			int yDir = xyPair[1];

			tempCount = getPieceCount(column, row, xDir, yDir, placedPiece);
			tempCount += getPieceCount(column, row, xDir * -1, yDir * -1,
					placedPiece);
			if (tempCount > inARow) {
				stateValue = Value.LOSE;
				return true;
			}
		}

		if (moveCount == totalPieces) {
			stateValue = Value.TIE;
			return true;
		}

		return false;
	}

	/**
	 * Checks consecutive pieces starting at xStart, yStart in the given
	 * direction to see if they match the pieceToCheck.
	 * 
	 * @param xStart
	 *            the column to start checking (inclusive).
	 * @param yStart
	 *            the row to start checking (inclusive).
	 * @param xDirection
	 *            the x direction to check in (-1, 0, or 1).
	 * @param yDirection
	 *            the y direction to check in (-1, 0, or 1).
	 * @param pieceToCheck
	 *            the type of Connect4Piece to check for.
	 * @return the number of consecutive pieces along the given path that match
	 *         pieceToCheck.
	 */
	private int getPieceCount(int xStart, int yStart, int xDirection,
			int yDirection, Connect4Piece pieceToCheck) {
		int countInARow = 0;
		while (xStart >= 0 && xStart < width && yStart >= 0 && yStart < height) {
			if (positionBoard[xStart][yStart] == pieceToCheck)
				countInARow++;
			else
				break;
			xStart += xDirection;
			yStart += yDirection;
		}
		return countInARow;
	}

	@Override
	public void doMove(Move move) {
		Connect4Move c4Move = (Connect4Move) move;
		int columnIndex = c4Move.getColumnIndex();
		if (columnIndex < 0 || columnIndex > width || isPrimitive)
			throw new IllegalArgumentException("Illegal move: " + c4Move);
		Connect4Piece[] moveColumn = positionBoard[columnIndex];

		int lowestRow = nextRowPositions[columnIndex];

		if (lowestRow == height)
			throw new IllegalArgumentException("Illegal move: " + c4Move);
		Connect4Piece newPiece = Connect4Piece.valueOf(player.toString());
		moveColumn[lowestRow] = newPiece;
		nextRowPositions[columnIndex]++;
		moveCount++;
		isPrimitive = isPrimitive(lowestRow, columnIndex, newPiece);
		player = player.getOppositePlayer();
	}

	@Override
	public void undoMove(Move move) {
		Connect4Move c4Move = (Connect4Move) move;
		int columnIndex = c4Move.getColumnIndex();
		if (columnIndex < 0 || columnIndex > width)
			throw new IllegalArgumentException("Illegal move: " + c4Move);
		Connect4Piece[] moveColumn = positionBoard[columnIndex];

		int lowestRow = nextRowPositions[columnIndex] - 1;

		if (lowestRow == -1)
			throw new IllegalArgumentException("Illegal move: " + c4Move);

		moveColumn[lowestRow] = Connect4Piece.BLANK;
		nextRowPositions[columnIndex]--;
		moveCount--;
		isPrimitive = false;
		stateValue = null;
		player = player.getOppositePlayer();
	}

	private String boardToString(Connect4Piece[][] positionBoard) {
		StringBuilder boardString = new StringBuilder();

		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				boardString.append(positionBoard[col][row].toChar());
			}
		}
		return boardString.toString();
	}

	private String debugBoardToString(Connect4Piece[][] positionBoard) {
		StringBuilder boardString = new StringBuilder();

		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				boardString.append(positionBoard[col][row].toChar());
			}
			boardString.append('|');
		}
		return boardString.toString();
	}

	@Override
	public String toString() {
		return '"' + boardToString(positionBoard) + '"';
	}

	@Override
	public String getBoardString() {
		return boardToString(positionBoard);
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getInARow() {
		return inARow;
	}

	@Override
	public GameState clone() {
		Connect4GameState toReturn = new Connect4GameState(width, height,
				inARow);
		toReturn.positionBoard = new Connect4Piece[width][];
		for (int col = 0; col < width; col++)
			toReturn.positionBoard[col] = positionBoard[col].clone();
		toReturn.player = player;
		toReturn.isPrimitive = this.isPrimitive;
		toReturn.nextRowPositions = nextRowPositions.clone();
		return toReturn;
	}

	@Override
	public Value getValue() {
		return stateValue;
	}
}
