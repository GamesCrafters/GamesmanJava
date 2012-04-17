package edu.berkeley.gamesman.parallel.game.connect4;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;

import edu.berkeley.gamesman.game.type.GameRecord;
import edu.berkeley.gamesman.game.type.GameValue;
import edu.berkeley.gamesman.hasher.genhasher.Move;
import edu.berkeley.gamesman.parallel.DualRecord;
import edu.berkeley.gamesman.parallel.FlipRecord;
import edu.berkeley.gamesman.parallel.SingleRecord;
import edu.berkeley.gamesman.parallel.ranges.Suffix;
import edu.berkeley.gamesman.parallel.ranges.RangeTree;
import edu.berkeley.gamesman.solve.reader.SolveReader;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.qll.QuickLinkedList;

public class Connect4 extends RangeTree<C4State, FlipRecord> implements
		SolveReader<C4State, FlipRecord> {
	private Move[] myMoves;
	private Move[][] colMoves;
	private C4Hasher myHasher;
	private int width, height, inARow;
	private int gameSize;
	private boolean useDual;
	private boolean misere;

	@Override
	public Collection<C4State> getStartingPositions() {
		C4State result = myHasher.newState();
		return Collections.singleton(result);
	}

	@Override
	public GameValue getValue(C4State state) {
		return state.getValue(inARow, opposite(getTurn(state)));
	}

	/**
	 * Returns expected number of pieces for a state (the high-index element of
	 * the state array)
	 * 
	 * @param state
	 *            The state
	 * @return The number of pieces/tier for this state
	 */
	int numPieces(C4State state) {
		return state.get(gameSize);
	}

	/**
	 * Given a state, fetches the piece at row-col (0 empty, 1 first player, 2
	 * second player)
	 * 
	 * @param state
	 * @param row
	 * @param col
	 * @return
	 */
	int get(C4State state, int row, int col) {
		return state.get(col * height + row);
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

	/**
	 * Returns whose turn it is for this state
	 * 
	 * @param state
	 * @return
	 */
	int getTurn(C4State state) {
		return getTurn(numPieces(state));
	}

	private static int getTurn(int numPieces) {
		return (numPieces % 2) + 1;
	}

	@Override
	public C4Hasher getHasher() {
		return myHasher;
	}

	@Override
	protected Move[] getMoves() {
		return myMoves;
	}

	private int getPlace(int row, int col) {
		return col * height + row;
	}

	private boolean isBottom(int row, int col) {
		return row == 0;
	}

	@Override
	public void rangeTreeConfigure(Configuration conf) {
		width = conf.getInt("gamesman.game.width", 5);
		height = conf.getInt("gamesman.game.height", 4);
		gameSize = width * height;
		if (gameSize + 2 >= Byte.MAX_VALUE)
			throw new RuntimeException("gameSize is too large");
		inARow = conf.getInt("gamemsan.game.pieces", 4);
		myHasher = new C4Hasher(width, height);
		ArrayList<Move>[] columnMoveList = new ArrayList[width];
		colMoves = new Move[width][];
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
		useDual = conf.getBoolean("gamesman.game.dual.record", false);
		misere = conf.getBoolean("gamesman.game.misere", false);
		assert useDual || !misere;
	}

	public C4State newState() {
		return myHasher.newState();
	}

	public boolean playMove(C4State state, int col) {
		boolean made = false;
		for (Move m : colMoves[col]) {
			if (m.matches(state) == -1) {
				myHasher.makeMove(state, m);
				made = true;
				break;
			}
		}
		return made;
	}

	/**
	 * Returns the tier which is just the last element of the sequence.
	 */
	@Override
	public int getDivision(Suffix<C4State> suff) {
		assert suff.length() == suffLen();
		return suff.get(suffLen() - 1);
	}

	@Override
	public C4State getPosition(String board) {
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
		C4State s = newState();
		getHasher().set(s, pos);
		return s;
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
	public Collection<Pair<String, C4State>> getChildren(C4State position) {
		ArrayList<Pair<String, C4State>> children = new ArrayList<Pair<String, C4State>>();
		for (int col = 0; col < width; col++) {
			C4State s = newState();
			getHasher().set(s, position);
			if (playMove(s, col)) {
				children.add(new Pair<String, C4State>(Integer.toString(col), s));
			}
		}
		return children;
	}

	@Override
	public String getString(C4State position) {
		StringBuilder sb = new StringBuilder(gameSize);
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				sb.append(charFor(position.get(col * height + row)));
			}
		}
		return sb.toString();
	}

	static char charFor(int piece) {
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

	@Override
	protected boolean setNewRecordAndHasChildren(C4State state, FlipRecord rec) {
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
		if (useDual)
			return DualRecord.combineValues((QuickLinkedList) grList,
					(DualRecord) gr);
		else
			return SingleRecord.combineValues((QuickLinkedList) grList,
					(SingleRecord) gr);
	}

	@Override
	protected void previousPosition(FlipRecord gr, FlipRecord toFill) {
		toFill.previousPosition(gr);
	}

	@Override
	protected Class<? extends FlipRecord> getGameRecordClass() {
		if (useDual)
			return DualRecord.class;
		else
			return SingleRecord.class;
	}

	@Override
	public GameRecord getRecord(C4State position, FlipRecord fetchedRec) {
		if (useDual)
			return DualRecord.getRecord((DualRecord) fetchedRec, gameSize
					- numPieces(position), misere);
		else {
			assert !misere;
			return SingleRecord.getRecord((SingleRecord) fetchedRec, gameSize
					- numPieces(position));
		}
	}

	protected int edgeMultiplier(Set<Integer> tiers) {
		return width + 1;
	}

	protected int maxVarianceLength() {
		return gameSize;
	}

	@Override
	public long getCleanupSplitSize(Configuration conf) {
		return toSplits(super.getCleanupSplitSize(conf));
	}

	@Override
	public long getCombineSplitSize(Configuration conf, int tier) {
		return toSplits(super.getCombineSplitSize(conf, tier), tier);
	}

	@Override
	public long getCreateSplitSize(Configuration conf, int tier) {
		return toSplits(super.getCreateSplitSize(conf, tier), tier);
	}

	@Override
	public long getPropogateSplitSize(Configuration conf, Set<Integer> tiers) {
		return toSplits(super.getPropogateSplitSize(conf, tiers), tiers);
	}

	private long toSplits(long positions) {
		HashSet<Integer> tierList = new HashSet<Integer>(gameSize + 1);
		for (int i = 0; i <= gameSize; i++) {
			tierList.add(i);
		}
		return toSplits(positions, tierList);
	}

	private long toSplits(long positions, int tier) {
		return toSplits(positions, Collections.singleton(tier));
	}

	private synchronized long toSplits(long positions, Set<Integer> tiers) {
		C4Hasher h1 = new C4Hasher(width, height, gameSize + 1 - suffLen());
		double numSum = 0;
		long numRanges = 0;
		int[] use = new int[1];
		for (int tier : tiers) {
			use[0] = tier;
			numSum += myHasher.numPositions(use);
			numRanges += h1.numPositions(use);
		}
		System.out.println(tiers);
		System.out.printf("\tAverage item = %.3f", numSum / numRanges);
		System.out.println();
		return Math.max((long) (positions * numRanges / numSum), 1);
	}
}
