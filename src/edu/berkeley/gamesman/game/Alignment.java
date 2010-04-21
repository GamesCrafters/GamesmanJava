package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Game;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.util.Pair;

public class Alignment extends Game<AlignmentState> {
	private int gameWidth, gameHeight, piecesToWin;
	private ArrayList<Pair<Integer, Integer>> openCells;

	@Override
	public void initialize(Configuration conf) {
		super.initialize(conf);
		gameWidth = conf.getInteger("gamesman.game.width", 8);
		gameHeight = conf.getInteger("gamesman.game.height", 8);
		piecesToWin = conf.getInteger("gamesman.game.pieces", 5);
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
				+ " captures";
	}

	@Override
	public String displayState(AlignmentState pos) {
		// TODO Return pretty-printed board
		return null;
	}

	@Override
	public void hashToState(long hash, AlignmentState s) {
		// TODO Write hash into s

	}

	@Override
	public int maxChildren() {
		// TODO Maximum children for any position
		return 0;
	}

	@Override
	public AlignmentState newState() {
		return new AlignmentState(new char[gameWidth][gameHeight], 0, 0);
	}

	@Override
	public long numHashes() {
		// TODO: Maximum hash plus one
		return 0;
	}

	@Override
	public PrimitiveValue primitiveValue(AlignmentState pos) {
		// TODO Auto-generated method stub
		return null;
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
		return null;
	}

	@Override
	public AlignmentState stringToState(String pos) {
		// TODO Inverse of stateToString
		return null;
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

class AlignmentState implements State {
	char[][] board; // chars are 'X', 'O' and ' ' (X plays first)
	int xDead;
	int oDead;

	public AlignmentState(char[][] board, int xDead, int oDead) {
		this.board = board;
		this.xDead = xDead;
		this.oDead = oDead;
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
	}
}