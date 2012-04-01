package edu.berkeley.gamesman.parallel.game.connections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.hadoop.conf.Configuration;

import edu.berkeley.gamesman.game.type.GameRecord;
import edu.berkeley.gamesman.game.type.GameValue;
import edu.berkeley.gamesman.hasher.DBHasher;
import edu.berkeley.gamesman.hasher.counting.CountingState;
import edu.berkeley.gamesman.hasher.genhasher.GenHasher;
import edu.berkeley.gamesman.hasher.genhasher.Move;
import edu.berkeley.gamesman.parallel.FlipRecord;
import edu.berkeley.gamesman.parallel.game.connect4.C4State;
import edu.berkeley.gamesman.parallel.game.connect4.Connect4;
import edu.berkeley.gamesman.parallel.game.connections.ConnectionsHasher;
import edu.berkeley.gamesman.parallel.ranges.RangeTree;
import edu.berkeley.gamesman.solve.reader.SolveReader;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.qll.QuickLinkedList;

public class Connections extends RangeTree<CountingState, FlipRecord> implements
		SolveReader<CountingState, FlipRecord> {
	private Move[] myMoves;
	private ConnectionsHasher myHasher;
	private int width, height = 7;
	private int gameSize = 49;
	private int suffLen;

	public String getString(CountingState position) {
		StringBuilder sb = new StringBuilder(gameSize);
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				sb.append(charFor(position.get(col * height + row)));
			}
		}
		return sb.toString();
	}

	private static Object charFor(int piece) {
		switch (piece) {
		case 0:
			return ' ';
		case 1:
			return 'X';
		case 2:
			return 'O';
		default:
			return '?';
		}
	}

	private static int pieceFor(char c) {
		switch (c) {
		case ' ':
			return 0;
		case 'X':
			return 1;
		case 'O':
			return 2;
		default:
			throw new IllegalArgumentException();
		}
	}

	@Override
	public CountingState getPosition(String board) {
		assert board.length() == gameSize;
		int[] pos = new int[gameSize + 1];
		int pieceCount = 0;
		int i = 0;
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				char c = board.charAt(i++);
				pos[col * height + row] = pieceFor(c);
				if (c != ' ')
					pieceCount++;
			}
		}
		pos[gameSize] = pieceCount;
		CountingState s = newState();
		getHasher().set(s, pos);
		return s;
	}

	private CountingState newState() {
		return myHasher.newState();
	}

	@Override
	public Collection<Pair<String, CountingState>> getChildren(
			CountingState position) {
		ArrayList<Pair<String, CountingState>> children = new ArrayList<Pair<String, CountingState>>();
		for (int i = 0; i < gameSize; i++) {
			CountingState s = newState();
			getHasher().set(s, position);
			if (playMove(s, i)) {
				children.add(new Pair<String, CountingState>(Integer
						.toString(i), s));
			}
		}
		return children;
	}

	@Override
	public GameValue getValue(CountingState state) {
		return getValueHelper(state, opposite(getTurn(state)));
	}

	private GameValue getValueHelper(CountingState state, int lastTurn) {
		// assert isComplete();
		return (hasSurround(state, lastTurn) || hasConnection(state, lastTurn)) ? GameValue.LOSE
				: (numPieces(state) == 45 ? GameValue.TIE : null);
	}

	private static boolean hasConnection(CountingState state, int lastTurn) {
		if ((Character) charFor(lastTurn) == 'X') {
			for (int startingX = 1; startingX <= 5; startingX = startingX + 2) {
				int currXDir = -1;
				int currYDir = 0;
				int currX = startingX;
				int currY = 0;
				while (!(currXDir == 0 && currYDir == -1 && currY == 0)
						&& currY != 6) {
					if ((Character) getChar(state, currX + 2 * currXDir, currY
							+ 2 * currYDir) == charFor(lastTurn)) {
						currX = currX + 2 * currXDir;
						currY = currY + 2 * currYDir;
						currXDir = (currXDir + 3) % 3 - 1;
						currYDir = (currYDir + 2) % 3 - 1;
					} else {
						currXDir = (currXDir + 2) % 3 - 1;
						currYDir = (currYDir + 3) % 3 - 1;
					}
				}
				if (currY == 6) {
					return true;
				}
			}
			return false;
		} else if ((Character) charFor(lastTurn) == 'O') {
			for (int startingY = 1; startingY <= 5; startingY = startingY + 2) {
				int currXDir = 0;
				int currYDir = 1;
				int currX = 0;
				int currY = startingY;
				while (!(currXDir == -1 && currYDir == 0 && currX == 0)
						&& currX != 6) {
					if ((Character) getChar(state, currX + 2 * currXDir, currY
							+ 2 * currYDir) == charFor(lastTurn)) {
						currX = currX + 2 * currXDir;
						currY = currY + 2 * currYDir;
						currXDir = (currXDir + 3) % 3 - 1;
						currYDir = (currYDir + 2) % 3 - 1;
					} else {
						currXDir = (currXDir + 2) % 3 - 1;
						currYDir = (currYDir + 3) % 3 - 1;
					}
				}
				if (currY == 6) {
					return true;
				}
			}
			return false;
		} else {
			System.err.println("fail");
			return false;
		}

	}

	public static boolean hasSurround(CountingState state, int lastTurn) {

	}

	private static char getChar(CountingState state, int x, int y) {
		return (Character) charFor(state.get(y * 4 + x));
	}

	@Override
	public Collection<CountingState> getStartingPositions() {
		CountingState result = myHasher.newState(); // newState() calls the
													// CountingState constructor
		return Collections.singleton(result);
	}

	@Override
	public GenHasher<CountingState> getHasher() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Move[] getMoves() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected int suffixLength() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public GameRecord getRecord(CountingState position, FlipRecord fetchedRec) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected boolean setNewRecordAndHasChildren(CountingState state,
			FlipRecord rec) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean combineValues(QuickLinkedList<FlipRecord> grList,
			FlipRecord gr) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void previousPosition(FlipRecord gr, FlipRecord toFill) {
		// TODO Auto-generated method stub

	}

	@Override
	protected Class<FlipRecord> getGameRecordClass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void rangeTreeConfigure(Configuration conf) {
		width = conf.getInt("gamesman.game.width", 4);
		height = conf.getInt("gamesman.game.height", 4);
		gameSize = width * height;
		if (gameSize + 2 >= Byte.MAX_VALUE)
			throw new RuntimeException("gameSize is too large");
		myHasher = new ConnectionsHasher(16); // board size is 4x4
		ArrayList<Move>[] columnMoveList = new ArrayList[width];

		// ###############JUST GENERATED
		Move[][] colMoves = new Move[width][];
		for (int i = 0; i < width; i++) {
			columnMoveList[i] = new ArrayList<Move>();
		}
		for (int numPieces = 0; numPieces < gameSize; numPieces++) {
			int turn = getTurn(numPieces);
			for (int row = 0; row < height; row++) {
				for (int col = 0; col < width; col++) {
					int place = getPlace(row, col);
					if (isBottom(row, col)) {
						columnMoveList[col].add(new Move(place, 0, turn,
								gameSize, numPieces, numPieces + 1));
					} else {
						columnMoveList[col].add(new Move(place - 1, 1, 1,
								place, 0, turn, gameSize, numPieces,
								numPieces + 1));
						columnMoveList[col].add(new Move(place - 1, 2, 2,
								place, 0, turn, gameSize, numPieces,
								numPieces + 1));
					}
				}
			}
		}
		ArrayList<Move> allMoves = new ArrayList<Move>();
		for (int i = 0; i < width; i++) {
			colMoves[i] = columnMoveList[i].toArray(new Move[columnMoveList[i]
					.size()]);
			allMoves.addAll(columnMoveList[i]);
		}
		myMoves = allMoves.toArray(new Move[allMoves.size()]);
		int varianceLength = conf.getInt("gamesman.game.variance.length", 10);
		suffLen = Math.max(1, gameSize + 1 - varianceLength);
	}

	// #################Just generated

	private static int getTurn(int numPieces) {
		return (numPieces % 2) + 1;
	}

	private int getPlace(int row, int col) {
		return col * height + row;
	}

	public boolean playMove(CountingState state, int i) {
		boolean made = false;
		Move m = myMoves[i];
		if (m.matches(state) == -1) {
			myHasher.makeMove(state, m);
			made = true;
		}

		return made;
	}

	int getTurn(CountingState state) {
		return getTurn(numPieces(state));
	}

	int numPieces(CountingState state) {
		return state.get(gameSize);
	}

	private static int opposite(int turn) {
		switch (turn) {
		case 1:
			return 2;
		case 2:
			return 1;
		default:
			throw new IllegalArgumentException(Integer.toString(turn));
		}
	}

}
