package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.TopDownMutaGame;
import edu.berkeley.gamesman.game.util.BitSetBoard;
import edu.berkeley.gamesman.game.util.C4State;
import edu.berkeley.gamesman.hasher.TDC4Hasher;
import edu.berkeley.gamesman.util.ExpCoefs;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

public final class TopDownC4 extends TopDownMutaGame<C4State> {

	private BitSetBoard bsb;

	private final int piecesToWin;

	public final int gameWidth, gameHeight, gameSize;

	private final int[] colHeights, lastMove;

	public final ExpCoefs ec;

	private final TDC4Hasher hasher;

	private int openColumns;

	private char turn;

	private C4State myState;

	public TopDownC4(Configuration conf) {
		super(conf);
		piecesToWin = conf.getInteger("gamesman.game.pieces", 4);
		gameWidth = conf.getInteger("gamesman.game.width", 5);
		gameHeight = conf.getInteger("gamesman.game.height", 4);
		gameSize = gameWidth * gameHeight;
		colHeights = new int[gameWidth];
		lastMove = new int[gameSize];
		myState = new C4State(0, 0, 0);
		openColumns = gameWidth;
		for (int i = 0; i < gameWidth; i++)
			colHeights[i] = 0;
		ec = new ExpCoefs(gameHeight, gameWidth);
		hasher = (TDC4Hasher) conf.getHasher();
		bsb = new BitSetBoard(gameHeight, gameWidth);
		turn = 'X';
	}

	@Override
	public boolean changeMove() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public long getHash() {
		return hasher.hash(myState);
	}

	@Override
	public C4State getState() {
		return myState;
	}

	@Override
	public boolean makeMove() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int maxChildren() {
		return gameWidth;
	}

	@Override
	public int maxMoves() {
		return gameSize;
	}

	@Override
	public PrimitiveValue primitiveValue() {
		switch (bsb.xInALine(piecesToWin, turn)) {
		case 1:
			return PrimitiveValue.LOSE;
		case 0:
			if (myState.numPieces == gameSize)
				return PrimitiveValue.TIE;
			else
				return PrimitiveValue.WIN;
		case -1:
			return PrimitiveValue.IMPOSSIBLE;
		default:
			Util.fatalError("This shouldn't happen");
			return null;
		}
	}

	@Override
	public void setToHash(long hash) {
		setToState(hasher.unhash(hash));
	}

	@Override
	public void setToState(C4State pos) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setToState(String pos) {
		// TODO Auto-generated method stub

	}

	@Override
	public void undoMove() {
		// TODO Auto-generated method stub

	}

	@Override
	public String describe() {
		return "Top Down" + gameWidth + "x" + gameHeight + " Connect "
				+ piecesToWin;
	}

	@Override
	public long numHashes() {
		return hasher.numHashes();
	}

	@Override
	public Collection<C4State> startingPositions() {
		ArrayList<C4State> poses = new ArrayList<C4State>(1);
		poses.add(new C4State(0, 0, 0));
		return poses;
	}

	@Override
	public String displayState() {
		return bsb.toString();
	}

	@Override
	public Collection<Pair<String, C4State>> validMoves(C4State pos) {
		LinkedList<Pair<String, C4State>> moves = new LinkedList<Pair<String, C4State>>();
		int col = gameWidth - 1;
		while (colHeights[col] == gameHeight && col >= 0)
			col--;
		boolean made = makeMove();
		while (made) {
			moves.addFirst(new Pair<String, C4State>(Integer.toString(col),
					getState().clone()));
			col--;
			while (colHeights[col] == gameHeight && col >= 0)
				col--;
			made = changeMove();
		}
		undoMove();
		return moves;
	}
}