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
import edu.berkeley.gamesman.util.QuickLinkedList;
import edu.berkeley.gamesman.util.Util;

public final class TopDownC4 extends TopDownMutaGame<C4State> {

	private class Move {
		final int[] openColumns;

		int columnIndex;

		public Move() {
			openColumns = new int[gameWidth];
		}
	}

	private BitSetBoard bsb;

	private final int piecesToWin;

	public final int gameWidth, gameHeight, gameSize;

	private final int[] colHeights;

	public final ExpCoefs ec;

	private final TDC4Hasher hasher;

	private final QuickLinkedList<Move> moves;

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
		Move[] moveArray = new Move[gameSize];
		QuickLinkedList<Move> myMoves = null;
		try {
			myMoves = new QuickLinkedList<Move>(moveArray, Move.class
					.getConstructor());
		} catch (SecurityException e) {
			Util.fatalError("Security error", e);
		} catch (NoSuchMethodException e) {
			Util.fatalError("This shouldn't happen", e);
		}
		moves = myMoves;
		myState = new C4State(0, 0, 0);
		for (int i = 0; i < gameWidth; i++)
			colHeights[i] = 0;
		ec = new ExpCoefs(gameHeight, gameWidth);
		hasher = (TDC4Hasher) conf.getHasher();
		bsb = new BitSetBoard(gameHeight, gameWidth);
		turn = 'X';
	}

	@Override
	public boolean changeMove() {
		Move myMove = moves.element();
		if (myMove.columnIndex == 0)
			return false;
		int col = myMove.openColumns[myMove.columnIndex];
		// TODO Remove piece (check all fields)
		--myMove.columnIndex;
		for (; col > myMove.openColumns[myMove.columnIndex]; --col) {
			// TODO Alter hash appropriately (use formula for column height, not
			// individual pieces)
		}
		// TODO Add piece (check all fields)
		return true;
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
		Move newMove = moves.add();
		int i = 0;
		for (int col = 0; col < gameWidth; col++) {
			if (colHeights[col] < gameHeight)
				newMove.openColumns[i++] = col;
		}
		if (i == 0)
			return false;
		newMove.columnIndex = i - 1;
		for (int col = gameWidth - 1; col > newMove.openColumns[newMove.columnIndex]; col--) {
			// TODO Alter hash appropriately (use formula for column height, not
			// individual pieces)
		}
		// TODO Add piece (remember to check all fields)
		return true;
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
		int col = moves.remove().openColumns[0];
		// TODO Remove piece (check all fields)
		for (++col; col < gameWidth; ++col) {
			// TODO Alter hash back
		}
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