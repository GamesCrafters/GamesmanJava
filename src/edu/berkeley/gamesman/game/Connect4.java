package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.TieredGame;
import edu.berkeley.gamesman.game.connect4.C4Board;
import edu.berkeley.gamesman.game.connect4.C4Piece;
import edu.berkeley.gamesman.hasher.PerfectConnect4Hash;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.DependencyResolver;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * Connect 4!
 * 
 * @author Steven Schlansker
 */
public class Connect4 extends TieredGame<C4Board> {
	final int piecesToWin, gameWidth, gameHeight;

	static {
		DependencyResolver.allowHasher(Connect4.class, PerfectConnect4Hash.class);
	}

	/**
	 * Connect4 Constructor Creates the hashers we use (does not use the
	 * command-line specified one, needs special hasher)
	 * 
	 * @param conf the configuration
	 */
	public Connect4(Configuration conf) {
		super(conf);
		piecesToWin = Integer.parseInt(conf.getProperty("gamesman.game.pieces", "4"));
		gameWidth = Integer.parseInt(conf.getProperty("gamesman.game.width", "7"));
		gameHeight = Integer.parseInt(conf.getProperty("gamesman.game.height", "6"));
	}

	@Override
	public Collection<C4Board> startingPositions() {
		ArrayList<C4Board> boards = new ArrayList<C4Board>();
		C4Board startBoard = new C4Board(gameWidth, gameHeight);
		boards.add(startBoard);
		return boards;
	}

	@Override
	public PrimitiveValue primitiveValue(C4Board pos) {
		PrimitiveValue v = pos.primitiveValue(piecesToWin);
		Util.debug(DebugFacility.GAME,"Primitive value of ",pos," is ",v);
		return v;
	}

	@Override
	public String displayState(C4Board pos) {
		String s = stateToString(pos);
		StringBuilder str = new StringBuilder(s.length() + 3 * gameHeight);
		for (int row = gameHeight - 1; row >= 0; row--) {
			str.append('|');
			str.append(s.substring(row * gameWidth, (row + 1) * gameWidth));
			str.append("|\n");
		}
		return str.toString();
	}

	@Override
	public C4Board stringToState(String pos) {
		C4Piece[][] board = new C4Piece[gameHeight][gameWidth];
		for (int row = 0; row < gameHeight; row++) {
			for (int col = 0; col < gameWidth; col++) {
				board[row][col] = C4Piece.toPiece(pos.charAt(Util.index(row,
						col, gameWidth)));
			}
		}
		// Util.debug("stringToState yields "+Arrays.deepToString(board));
		return new C4Board(board);
	}

	@Override
	public String stateToString(C4Board pos) {
		char[] state = new char[gameWidth * gameHeight];
		for (int row = 0; row < gameHeight; row++) {
			for (int col = 0; col < gameWidth; col++) {
				state[Util.index(row, col, gameWidth)] = pos.get(row, col)
						.toChar();
			}
		}
		// Util.debug("stringToState yields "+Arrays.deepToString(board));
		return new String(state);
	}

	@Override
	public Collection<Pair<String, C4Board>> validMoves(C4Board pos) {
		ArrayList<Pair<String, C4Board>> nextBoards = new ArrayList<Pair<String, C4Board>>();
		C4Board b;
		for (int col = 0; col < gameWidth; col++) {
			b = pos.makeMove(col);
			if (b != null)
				nextBoards.add(new Pair<String, C4Board>("c" + col, b));
		}
		Util.debug(DebugFacility.GAME, "Connect4 board ",pos," yields children ",nextBoards);
		return nextBoards;
	}

	@Override
	public String describe() {
		return String.format("%dx%d Connect %d", gameWidth, gameHeight, piecesToWin);
	}
}
