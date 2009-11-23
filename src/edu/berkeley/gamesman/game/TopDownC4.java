package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Collection;

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

	final int piecesToWin, gameWidth, gameHeight;

	public final int gameSize;

	private final int[] colHeights, lastMove;

	public final ExpCoefs ec;

	private final TDC4Hasher hasher;

	private int numPieces, openColumns;

	private char turn;

	public TopDownC4(Configuration conf) {
		super(conf);
		piecesToWin = conf.getInteger("gamesman.game.pieces", 4);
		gameWidth = conf.getInteger("gamesman.game.width", 5);
		gameHeight = conf.getInteger("gamesman.game.height", 4);
		gameSize = gameWidth * gameHeight;
		colHeights = new int[gameWidth];
		lastMove = new int[gameSize];
		numPieces = 0;
		openColumns = gameWidth;
		for (int i = 0; i < gameWidth; i++)
			colHeights[i] = 0;
		ec = new ExpCoefs(gameHeight, gameWidth);
		hasher = (TDC4Hasher) conf.getHasher();
	}

	@Override
	public boolean changeMove() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public long getHash() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public C4State getState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean makeMove() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int maxChildren() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int maxMoves() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public PrimitiveValue primitiveValue() {
		switch (bsb.xInALine(piecesToWin, turn)) {
		case 1:
			return PrimitiveValue.LOSE;
		case 0:
			if (numPieces == gameSize)
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
		// TODO Auto-generated method stub

	}

	@Override
	public void setToState(C4State pos) {
		setToHash(hasher.hash(pos));
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long numHashes() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Collection<C4State> startingPositions() {
		ArrayList<C4State> poses = new ArrayList<C4State>(1);
		poses.add(new C4State(0, 0, 0));
		return poses;
	}

	@Override
	public String displayState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Pair<String, C4State>> validMoves(C4State pos) {
		// TODO write method
		return null;
	}

}