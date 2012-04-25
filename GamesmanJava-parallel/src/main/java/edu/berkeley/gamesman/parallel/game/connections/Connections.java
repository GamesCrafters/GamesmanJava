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
import edu.berkeley.gamesman.parallel.SingleRecord;
import edu.berkeley.gamesman.parallel.game.connect4.C4Hasher;
import edu.berkeley.gamesman.parallel.game.connect4.C4State;
import edu.berkeley.gamesman.parallel.game.connect4.Connect4;
import edu.berkeley.gamesman.parallel.game.connections.ConnectionsHasher;
import edu.berkeley.gamesman.parallel.ranges.RangeTree;
import edu.berkeley.gamesman.solve.reader.SolveReader;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.qll.QuickLinkedList;

public class Connections extends RangeTree<CountingState, FlipRecord> implements
		SolveReader<CountingState, FlipRecord> {
	// Scoring guidelines: win, lose or tie from the GameValue class (enumerations).
	// Note about CountingState: only positions that can be played in. (1-D array)
	private Move[] myMoves;
	private ConnectionsHasher myHasher;
	private int width = 3;
	private int height = 7;
	private int gameSize = 21;

	// private int suffLen;

	// Converts CountingState into an API string.
	public String getString(CountingState position) {
		StringBuilder sb = new StringBuilder(gameSize);
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				sb.append(charFor(position.get(col * height + row)));
			}
		}
		return sb.toString();
	}
	
	// Converts from our representation to API representation
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
	
	// Converts from API representation to our representation
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

	// Takes API string and converts into CountingState, which we will use
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

	// Resets. Returns a blank board.
	CountingState newState() {
		return myHasher.newState();
	}

	// Returns an arrayList of all the possible CountingState children, given a current CountingState (or board representation)
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

	// Finds the primitive value of the CountingState.
	@Override
	public GameValue getValue(CountingState state) {
		return getValueHelper(state, opposite(getTurn(state)));
	}
	
	// Checks if there is a connection or a surround. Called by getValue.
	private GameValue getValueHelper(CountingState state, int lastTurn) {
		// assert isComplete();
		return (hasSurround(state, lastTurn) || hasConnection(state, lastTurn)) ? GameValue.LOSE
				: (numPieces(state) == 45 ? GameValue.TIE : null);
	}

	// Checks to see if there is a connection. Uses a maze-traversal method. Called by getValueHelper.
	private static boolean hasConnection(CountingState state, int lastTurn) {
		if ((Character) charFor(lastTurn) == 'X') {
			for (int startingX = 1; startingX <= 5; startingX = startingX + 2) {
				int currXDir = -1;
				int currYDir = 0;
				int currX = startingX;
				int currY = 0;
				while (!(currXDir == 0 && currYDir == -1 && currY == 0)
						&& currY != 6) {
					if ((Character) getChar(state, currX + currXDir, currY
							+ currYDir) == charFor(lastTurn)) {
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
					if ((Character) getChar(state, currX + currXDir, currY
							+ currYDir) == charFor(lastTurn)) {
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

	// Checks to see if a loop around an opponent's pieces exists. Called by getValueHelper.
	// Only checks certain cycles. In our case, only three from middle row or column. 
	
	public static boolean hasSurround(CountingState state, int lastTurn) {
		if ((Character) charFor(lastTurn) == 'X') {
			return hasSurroundHelper(state, lastTurn, 3, 0,
					new ArrayList<Pair<Integer, Integer>>(1), 3, 0, 0)
					|| hasSurroundHelper(state, lastTurn, 3, 2,
							new ArrayList<Pair<Integer, Integer>>(1), 3, 2, 0)
					|| hasSurroundHelper(state, lastTurn, 3, 6,
							new ArrayList<Pair<Integer, Integer>>(1), 3, 6, 0);
		} else if ((Character) charFor(lastTurn) == 'O') {
			return hasSurroundHelper(state, lastTurn, 0, 3,
					new ArrayList<Pair<Integer, Integer>>(1), 0, 3, 0)
					|| hasSurroundHelper(state, lastTurn, 2, 3,
							new ArrayList<Pair<Integer, Integer>>(1), 2, 3, 0)
					|| hasSurroundHelper(state, lastTurn, 6, 3,
							new ArrayList<Pair<Integer, Integer>>(1), 6, 3, 0);
		} else {
			System.err.println("fail");
			return false;
		}
	}

	// Traverses every filled in line, starting from (origX, origY). Uses flood-fill (recursive).
	
	private static boolean hasSurroundHelper(CountingState state, int lastTurn,
			int currX, int currY, ArrayList<Pair<Integer, Integer>> used,
			int origX, int origY, int currDepth) {
		if (currX == origX && currY == origY && currDepth != 0
				&& currDepth != 2) {
			return true;
		} else if (currX == origX && currY == origY && currDepth == 2) {
			return false;
		}
		boolean hasSurround = false;
		for (int xDir = 1, yDir = 0, count = 0; count < 4; xDir = (xDir + 3) % 3 - 1, yDir = (yDir + 2) % 3 - 1, count++) {
			if (getChar(state, currX + xDir, currY + yDir) == lastTurn
					&& !isIn(new Pair<Integer, Integer>(currX + (2 * xDir),
							currY + (2 * yDir)), used)) {
				used.add(new Pair<Integer, Integer>(currX, currY));
				hasSurround = hasSurround
						|| hasSurroundHelper(state, lastTurn, currX
								+ (2 * xDir), currY + (2 * yDir), used, origX,
								origY, currDepth + 1);
			}
		}
		return hasSurround;
	}

	// Utility method called by hasSurroundHelper. Checks to see if we visited the square yet in our current connection that we are checking.
	
	private static boolean isIn(Pair<Integer, Integer> pair,
			ArrayList<Pair<Integer, Integer>> list) {
		for (Pair<Integer, Integer> p : list) {
			if (pair.car == p.car && pair.cdr == p.cdr) {
				return true;
			}
		}
		return false;
	}

	// Translates full-board representation to CountingState representation (going from a "fake CountingState" that represents the entire board to one that just represents playable positions)
	
	private static char getChar(CountingState state, int x, int y) {
		if (x < 1 || x > 5 || y < 1 || y > 5) {
			return (Character) null;
		}
		switch (x) {
		case 0:
			return (Character) charFor(state.get(0 + (x / 4)));
		case 1:
			return (Character) charFor(state.get(2 + (x / 2)));
		case 2:
			return (Character) charFor(state.get(5 + (x / 2)));
		case 3:
			return (Character) charFor(state.get(9 + (x / 2)));
		case 4:
			return (Character) charFor(state.get(12 + (x / 2)));
		case 5:
			return (Character) charFor(state.get(16 + (x / 2)));
		case 6:
			return (Character) charFor(state.get(19 + (x / 2)));
		default:
			return (Character) null;
		}
	}
	// Gets all possible moves, and assigns them to the array myMoves.
		public void rangeTreeConfigure(Configuration conf) {
			width = conf.getInt("gamesman.game.width", 3);
			height = conf.getInt("gamesman.game.height", 7);
			gameSize = width * height;
			if (gameSize + 2 >= Byte.MAX_VALUE)
				throw new RuntimeException("gameSize is too large");
			myHasher = new ConnectionsHasher(gameSize);
			ArrayList<Move> moveList = new ArrayList<Move>();
			for (int numPieces = 0; numPieces < gameSize; numPieces++) {
				int turn = getTurn(numPieces);
				for (int row = 0; row < height; row++) {
					for (int col = 0; col < width; col++) {
						int place = getPlace(row, col);
						moveList.add(new Move(place, 0, turn, gameSize, numPieces,
								numPieces + 1));
					}
				}
			}
			myMoves = moveList.toArray(new Move[moveList.size()]);
		}
		// getter.
		private static int getTurn(int numPieces) {
			return (numPieces % 2) + 1;
		}
		// getter.
		private int getPlace(int row, int col) {
			return col * height + row;
		}

		// Changes CountingState: i is index of the move, takes move from myMoves array and applies it.
		public boolean playMove(CountingState state, int i) {
			boolean made = false;
			Move m = myMoves[i];
			if (m.matches(state) == -1) {
				myHasher.makeMove(state, m);
				made = true;
			}

			return made;
		}

		// getter.
		public int getTurn(CountingState state) {
			return getTurn(numPieces(state));
		}
		// Returns number of pieces on the board (in the CountingState).
		int numPieces(CountingState state) {
			return state.get(gameSize);
		}

		// Returns the opposite of the turn. (i.e., blue's turn -> red's turn)
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

		// Returns the max variance length. :)
		protected int maxVarianceLength() {
			return gameSize;
		}
		
		
	/*
	 * (non-Javadoc)
	 * 
	 * #######################################
	 * This is stuff that we did not change.
	 * #######################################
	 * @see edu.berkeley.gamesman.parallel.ranges.RangeTree#getStartingPositions()
	 */

	// As name describes.
	@Override
	public Collection<CountingState> getStartingPositions() {
		CountingState result = myHasher.newState(); // newState() calls the
													// CountingState constructor
		return Collections.singleton(result);
	}
	// getter.
	@Override
	public GenHasher<CountingState> getHasher() {
		return myHasher;
	}
	// getter.
	@Override
	protected Move[] getMoves() {
		return myMoves;
	}
	// getter.
	@Override
	public GameRecord getRecord(CountingState position, FlipRecord fetchedRec) {
		return SingleRecord.getRecord((SingleRecord) fetchedRec, gameSize
				- numPieces(position));
	}
	// David's method. ~\('v')/~
	// Sets new record.
	@Override
	protected boolean setNewRecordAndHasChildren(CountingState state,
			FlipRecord rec) {
		GameValue val = getValue(state);
		if (val == null) {
			rec.set(GameValue.DRAW);
			return true;
		} else {
			if (val == GameValue.TIE)
				rec.set(GameValue.TIE, 0);
			else if (val == GameValue.LOSE)
				rec.set(GameValue.LOSE, 0);
			else
				throw new RuntimeException("No other primitives");
			return false;
		}
	}

	@Override
	protected boolean combineValues(QuickLinkedList<FlipRecord> grList,
			FlipRecord gr) {
		return SingleRecord.combineValues((QuickLinkedList) grList,
				(SingleRecord) gr);
	}

	@Override
	protected void previousPosition(FlipRecord gr, FlipRecord toFill) {
		toFill.previousPosition(gr);

	}

	@Override
	protected Class<? extends FlipRecord> getGameRecordClass() {
		return SingleRecord.class;
	}

}
