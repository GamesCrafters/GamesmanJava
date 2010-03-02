package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import edu.berkeley.gamesman.core.*;
import edu.berkeley.gamesman.game.util.BitSetBoard;
import edu.berkeley.gamesman.game.util.C4State;
import edu.berkeley.gamesman.game.util.TopDownPieceRearranger;
import edu.berkeley.gamesman.hasher.TDC4Hasher;
import edu.berkeley.gamesman.util.*;

/**
 * The game for solving Connect4 top down
 * 
 * @author dnspies
 */
public final class TopDownC4 extends TopDownMutaGame<C4State> {

	private class Move {
		final int[] openColumns;

		int columnIndex;

		int piecesLeft;

		int moveSerial;

		public Move() {
			openColumns = new int[gameWidth];
		}
	}

	private BitSetBoard bsb;

	private final int piecesToWin;

	/**
	 * The width of the game
	 */
	public final int gameWidth;
	/**
	 * The height of the game
	 */
	public final int gameHeight;
	/**
	 * gameWidth * gameHeight
	 */
	public final int gameSize;

	private final int[] colHeights;

	/**
	 * An ExpCoefs object of degree gameHeight for making hash calculations
	 */
	public final ExpCoefs ec;

	private final QuickLinkedList<Move> moves;

	private final TopDownPieceRearranger arranger;

	private char turn;

	private C4State myState;

	/**
	 * @param conf
	 *            The configuration object
	 */
	public TopDownC4(Configuration conf) {
		super(conf);
		piecesToWin = conf.getInteger("gamesman.game.pieces", 4);
		gameWidth = conf.getInteger("gamesman.game.width", 5);
		gameHeight = conf.getInteger("gamesman.game.height", 4);
		gameSize = gameWidth * gameHeight;
		colHeights = new int[gameWidth];
		Move[] moveArray = new Move[gameSize];
		QuickLinkedList<Move> myMoves = null;
		myMoves = new QuickLinkedList<Move>(moveArray, new Factory<Move>() {
			public Move newElement() {
				return new Move();
			}
		});
		moves = myMoves;
		myState = new C4State(0, 0, 0);
		for (int i = 0; i < gameWidth; i++)
			colHeights[i] = 0;
		ec = new ExpCoefs(gameHeight, gameWidth);
		bsb = new BitSetBoard(gameHeight, gameWidth);
		arranger = new TopDownPieceRearranger(gameSize);
		turn = 'X';
	}

	@Override
	public boolean changeMove() {

		Move myMove = moves.element();
		if (myMove.columnIndex == 0)
			return false;
		int col = myMove.openColumns[myMove.columnIndex];
		int stepsBack = --colHeights[col];
		bsb.removePiece(colHeights[col], col);
		--myMove.columnIndex;
		myMove.piecesLeft -= colHeights[col];
		myState.spaceArrangement -= ec.getCoef(col, myMove.piecesLeft);
		for (--col; col > myMove.openColumns[myMove.columnIndex]; --col) {
			myState.spaceArrangement += ec.getCoef(col, myMove.piecesLeft);
			myMove.piecesLeft -= colHeights[col];
			stepsBack += colHeights[col];
			myState.spaceArrangement -= ec.getCoef(col, myMove.piecesLeft);
		}
		myMove.moveSerial = arranger.changeMove(myMove.moveSerial, stepsBack);
		bsb.addPiece(colHeights[col], col, oppositeTurn());
		++colHeights[col];
		myState.spaceArrangement += ec.getCoef(col, myMove.piecesLeft);
		myState.pieceArrangement = arranger.getHash();

		return true;
	}

	@Override
	public long getHash() {
		return ((TDC4Hasher) conf.getHasher()).hash(myState);
	}

	@Override
	public C4State getState() {
		return myState;
	}

	@Override
	public boolean makeMove() {
		Move newMove = moves.addFirst();
		int i = 0;
		for (int col = 0; col < gameWidth; col++) {
			if (colHeights[col] < gameHeight)
				newMove.openColumns[i++] = col;
		}
		if (i == 0)
			return false;
		++myState.numPieces;
		newMove.columnIndex = i - 1;
		newMove.piecesLeft = myState.numPieces;
		int col;
		int stepsBack = 0;
		for (col = gameWidth - 1; col > newMove.openColumns[newMove.columnIndex]; col--) {
			myState.spaceArrangement += ec.getCoef(col, newMove.piecesLeft);
			newMove.piecesLeft -= colHeights[col];
			stepsBack += colHeights[col];
			myState.spaceArrangement -= ec.getCoef(col, newMove.piecesLeft);
		}
		bsb.addPiece(colHeights[col], col, turn);
		newMove.moveSerial = arranger.makeMove(turn, stepsBack);
		++colHeights[col];
		turn = oppositeTurn();
		myState.spaceArrangement += ec.getCoef(col, newMove.piecesLeft);
		myState.pieceArrangement = arranger.getHash();
		return true;
	}

	@Override
	public int maxChildren() {
		return gameWidth;
	}

	@Override
	public int maxRemoteness() {
		return gameSize;
	}

	@Override
	public PrimitiveValue primitiveValue() {
		switch (bsb.xInALine(piecesToWin, oppositeTurn())) {
		case 1:
			return PrimitiveValue.LOSE;
		case 0:
			if (myState.numPieces == gameSize)
				return PrimitiveValue.TIE;
			else
				return PrimitiveValue.UNDECIDED;
		case -1:
			return PrimitiveValue.IMPOSSIBLE;
		default:
			Util.fatalError("This shouldn't happen");
			return null;
		}
	}

	private char oppositeTurn() {
		switch (turn) {
		case 'X':
			return 'O';
		case 'O':
			return 'X';
		default:
			throw new RuntimeException("This should never happen");
		}
	}

	@Override
	public void setToHash(long hash) {
		setToState(((TDC4Hasher) conf.getHasher()).unhash(hash));
	}

	@Override
	public void setToState(C4State pos) {
		myState.set(pos);
		turn = ((myState.numPieces & 1) > 0) ? 'O' : 'X';
		setColumnHeights(pos.spaceArrangement);
		arranger.setArrangement(pos.numPieces, pos.numPieces / 2,
				pos.pieceArrangement);
		setBSB();
	}

	private void setBSB() {
		CharIterator iter = arranger.getCharIterator();
		bsb.clear();
		for (int col = 0; col < gameWidth; col++) {
			for (int row = 0; row < colHeights[col]; row++)
				bsb.addPiece(row, col, iter.next());
		}
	}

	private void setColumnHeights(long spaceArrangement) {
		int remainingPieces = myState.numPieces;
		for (int col = gameWidth - 1; col >= 0; col--) {
			colHeights[col] = 0;
			long pieceHash = ec.getCoef(col, remainingPieces);
			while (spaceArrangement >= pieceHash && remainingPieces > 0) {
				--remainingPieces;
				++colHeights[col];
				spaceArrangement -= pieceHash;
				pieceHash = ec.getCoef(col, remainingPieces);
			}
		}
	}

	@Override
	public void setFromString(String pos) {
		char[] posChars = pos.toCharArray();
		StringBuilder arrangerChars = new StringBuilder();
		myState.numPieces = 0;
		myState.spaceArrangement = 0;
		myState.pieceArrangement = 0;
		int charIndex;
		for (int col = 0; col < gameWidth; col++) {
			colHeights[col] = 0;
			for (int row = 0; row < gameHeight; row++) {
				charIndex = row * gameWidth + col;
				if (posChars[charIndex] != ' ') {
					++myState.numPieces;
					++colHeights[col];
					myState.spaceArrangement += ec.getCoef(col,
							myState.numPieces);
					bsb.addPiece(row, col, posChars[charIndex]);
					arrangerChars.append(posChars[charIndex]);
				}
			}
		}
		turn = ((myState.numPieces & 1) > 0) ? 'O' : 'X';
		arranger.setArrangement(arrangerChars.toString());
		myState.pieceArrangement = arranger.getHash();
	}

	@Override
	public void undoMove() {
		Move myMove = moves.remove();
		int col = myMove.openColumns[myMove.columnIndex];
		--colHeights[col];
		bsb.removePiece(colHeights[col], col);
		myState.spaceArrangement -= ec.getCoef(col, myMove.piecesLeft);
		for (++col; col < gameWidth; ++col) {
			myState.spaceArrangement += ec.getCoef(col, myMove.piecesLeft);
			myMove.piecesLeft += colHeights[col];
			myState.spaceArrangement -= ec.getCoef(col, myMove.piecesLeft);
		}
		arranger.undoMove(myMove.moveSerial);
		turn = oppositeTurn();
		--myState.numPieces;
		myState.pieceArrangement = arranger.getHash();
	}

	// private void checkArrangement() {
	// long totalHash = 0L;
	//
	// int pieceCount = 0;
	//
	// for (int col = 0; col < gameWidth; col++) {
	// for (int row = 0; row < colHeights[col]; row++) {
	// ++pieceCount;
	// totalHash += ec.getCoef(col, pieceCount);
	// }
	// }
	//
	// if (totalHash != myState.spaceArrangement)
	// throw new RuntimeException("Not Equal");
	// }

	@Override
	public String describe() {
		return "Top Down" + gameWidth + "x" + gameHeight + " Connect "
				+ piecesToWin;
	}

	@Override
	public long numHashes() {
		return ((TDC4Hasher) conf.getHasher()).numHashes();
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
	public Collection<Pair<String, C4State>> validMoves() {
		LinkedList<Pair<String, C4State>> moves = new LinkedList<Pair<String, C4State>>();
		int col = gameWidth - 1;
		while (col >= 0 && colHeights[col] == gameHeight)
			col--;
		boolean made = makeMove();
		while (made) {
			moves.addFirst(new Pair<String, C4State>(Integer.toString(col),
					getState().clone()));
			col--;
			while (col >= 0 && colHeights[col] == gameHeight)
				col--;
			if (col < 0)
				break;
			made = changeMove();
		}
		if (made)
			undoMove();
		return moves;
	}

	private class C4Record extends Record {
		protected C4Record() {
			super(conf);
		}

		protected C4Record(long state) {
			super(conf);
			set(state);
		}

		protected C4Record(PrimitiveValue pVal) {
			super(conf, pVal);
		}

		@Override
		public long getState() {
			if (conf.remotenessStates > 0) {
				PrimitiveValue val = value;
				if (val.equals(PrimitiveValue.TIE)) {
					return gameSize + 1;
				} else if (val.equals(PrimitiveValue.UNDECIDED)) {
					return gameSize + 2;
				} else {
					return remoteness;
				}
			} else {
				return value.value;
			}
		}

		@Override
		public void set(long state) {
			if (conf.remotenessStates > 0) {
				if (state == gameSize + 1) {
					value = PrimitiveValue.TIE;
					remoteness = gameSize - myState.numPieces;
				} else if (state == gameSize + 2) {
					value = PrimitiveValue.UNDECIDED;
				} else if ((state & 1L) > 0) {
					value = PrimitiveValue.WIN;
					remoteness = (int) state;
				} else {
					value = PrimitiveValue.LOSE;
					remoteness = (int) state;
				}
			} else {
				value = PrimitiveValue.values[(int) state];
			}
		}
	}

	@Override
	public Record newRecord(PrimitiveValue pv) {
		return new C4Record(pv);
	}

	@Override
	public Record newRecord() {
		return new C4Record();
	}

	@Override
	public Record newRecord(long val) {
		return new C4Record(val);
	}

	/**
	 * Sets the board to contain the passed number of pieces
	 * 
	 * @param numPieces
	 *            The number of pieces on the board
	 */
	public void setNumPieces(int numPieces) {
		myState.numPieces = numPieces;
		turn = ((numPieces & 1) > 0) ? 'O' : 'X';
	}

	@Override
	public long recordStates() {
		return gameSize + 3;
	}

	@Override
	public String toString() {
		return bsb.toString();
	}

	@Override
	public long stateToHash(C4State pos) {
		return ((TDC4Hasher) conf.getHasher()).hash(pos);
	}

	@Override
	public C4State newState() {
		return new C4State(0, 0, 0);
	}

	@Override
	public C4State[] newStateArray(int len) {
		C4State[] arr = new C4State[len];
		for (int i = 0; i < len; i++) {
			arr[i] = newState();
		}
		return arr;
	}
}