package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Game;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.util.Pair;



/**
 * @author Aloni & Brent
 *
 */
public class Alignment extends Game<AlignmentState> {
	private int gameWidth, gameHeight, piecesToWin; 
	private AlignmentVariant variant;
	private ArrayList<Pair<Integer, Integer>> openCells;

	@Override
	public void initialize(Configuration conf) {
		super.initialize(conf);
		gameWidth = conf.getInteger("gamesman.game.width", 8);
		gameHeight = conf.getInteger("gamesman.game.height", 8);
		piecesToWin = conf.getInteger("gamesman.game.pieces", 5);
		variant = AlignmentVariant.getVariant(conf.getInteger("gamesman.game.variant", 1));
		for (int row = 0; row < gameHeight; row++) {
			for (int col = 0; col < gameWidth; col++) {
				// TODO Eliminate corners
				openCells.add(new Pair<Integer, Integer>(row, col));
			}
		}
	}

	@Override
	public String describe() {
		return "Alignment: " + gameWidth + "x" + gameHeight + " " + piecesToWin
		+ " captures " + variant.name();
	}

	@Override
	public String displayState(AlignmentState pos) {
		// TODO Return pretty-printed board
		StringBuilder board = new StringBuilder(gameWidth * gameHeight); //adapted from Connect4.java;
		int row = 0;
		for (; row < gameHeight; row++) {
			for (int col = 0; col < gameWidth; col++) {
				board.append(pos.get(row, col) + " "); //could put '#' on invalid squares?
			}
		}
		for (row = 0; row < gameHeight; row++) {
			board.replace((2*gameWidth*(row+1) - 1), (2*gameWidth*(row+1)), "\n"); //is this correct?
		}
		return null;
	}

	@Override
	public void hashToState(long hash, AlignmentState s) { //do the hashes have to be dense?
		// TODO Write hash into s

	}

	@Override
	public int maxChildren() {
		// TODO Maximum children for any position
		// upperbound equals 2*number-of-squares (can be variant specific); can make one move or two, or place, or something elsse;
		return 0;
	}

	@Override
	public AlignmentState newState() {
		return new AlignmentState(new char[gameWidth][gameHeight], 0, 0, 'O');
	}

	@Override
	public long numHashes() {
		// TODO: Maximum hash plus one
		return 0;
	}

	@Override
	public PrimitiveValue primitiveValue(AlignmentState pos) {
		// TODO Auto-generated method stub
		// Just check the number dead
		if (pos.lastMove == 'X') {
			if (pos.oDead >= piecesToWin) {
				return PrimitiveValue.LOSE;
			}
			if (pos.xDead >= piecesToWin) {
				return PrimitiveValue.WIN;
			} else {
				return PrimitiveValue.UNDECIDED;
			}
		}
		if (pos.lastMove == 'O') {
			if (pos.xDead >= piecesToWin) {
				return PrimitiveValue.LOSE;
			}
			if (pos.oDead >= piecesToWin) {
				return PrimitiveValue.WIN;
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
			as.board[place.car][place.cdr] = ' ';
		ArrayList<AlignmentState> retVal = new ArrayList<AlignmentState>(1);
		retVal.add(as);
		return retVal;
	}

	@Override
	public long stateToHash(AlignmentState pos) {
		// TODO hash board
		return 0;
	}

	@Override
	public String stateToString(AlignmentState pos) {
		// TODO Machine-readable string
		StringBuilder board = new StringBuilder(gameWidth * gameHeight); //adapted from Connect4.java;
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
		// TODO Inverse of stateToString
		char[][] board = new char[gameWidth][gameHeight];
		int xDead, oDead;
		char lastMove;
		int square = 0;
		for (int row = 0; row < gameHeight; row++) {
			for (int col = 0; col < gameWidth; col++) {
				board[row][col] = pos.charAt(square);
				square++;
			}
		}
		String[] auxData = pos.substring((gameWidth * gameHeight) - 1).split(":");
		xDead = Integer.parseInt(auxData[0]); oDead = Integer.parseInt(auxData[2]);
		lastMove = auxData[1].charAt(0);
		return new AlignmentState(board, xDead, oDead, lastMove);
	}

	@Override
	public Collection<Pair<String, AlignmentState>> validMoves(
			AlignmentState pos) {
		// TODO Return children
		return null;
	}

	@Override
	public int validMoves(AlignmentState pos, AlignmentState[] children) {
		// TODO Modify children to be validMoves of pos. Return number of
		// children

		return 0;
	}
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

class AlignmentState implements State {
	char[][] board; // chars are 'X', 'O' and ' ' (X plays first)
	char lastMove;
	int xDead;
	int oDead;

	public AlignmentState(char[][] board, int xDead, int oDead, char lastMove) {
		this.board = board;
		this.xDead = xDead;
		this.oDead = oDead;
		this.lastMove = lastMove;
	}

	public void set(State s) {
		AlignmentState as = (AlignmentState) s;
		for (int row = 0; row < board.length; row++) {
			for (int col = 0; col < board[row].length; col++) {
				board[row][col] = as.board[row][col];
			}
		}
		xDead = as.xDead;
		oDead = as.oDead;
		lastMove = as.lastMove;
	}

	char get(int row, int col) {
		return board[row][col];
	}
}
