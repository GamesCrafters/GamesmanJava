package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Collection;

import edu.berkeley.gamesman.core.*;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.cache.GenCache;
import edu.berkeley.gamesman.database.cache.TierCache;
import edu.berkeley.gamesman.game.util.BitSetBoard;
import edu.berkeley.gamesman.game.util.TierState;
import edu.berkeley.gamesman.hasher.C4Hasher;
import edu.berkeley.gamesman.hasher.cachehasher.CacheHasher;
import edu.berkeley.gamesman.hasher.cachehasher.CacheMove;
import edu.berkeley.gamesman.hasher.fixed.FixedState;
import edu.berkeley.gamesman.hasher.genhasher.GenHasher;
import edu.berkeley.gamesman.util.Pair;

/**
 * Implementation of Connect 4 using the general IterArrangerHasher
 * 
 * @author DNSpies
 */
public final class GenConnect4 extends TierGame {
	private final C4Hasher[] tierHashers;
	private final CacheHasher<FixedState>[] cacheHashers;
	private final int width, height, piecesInARow;
	private final int boardSize;
	private int tier;
	private final BitSetBoard myBoard;
	private final long[] children;
	private final CacheMove[] xAllMoves, oAllMoves;
	private final int[] testCachePlaces;

	/**
	 * @param conf
	 */
	@SuppressWarnings("unchecked")
	public GenConnect4(Configuration conf) {
		super(conf);
		width = conf.getInteger("gamesman.game.width", 7);
		height = conf.getInteger("gamesman.game.height", 6);
		piecesInARow = conf.getInteger("gamesman.game.pieces", 4);
		boardSize = width * height;
		myBoard = new BitSetBoard(height, width);
		tierHashers = new C4Hasher[boardSize + 1];
		cacheHashers = new CacheHasher[boardSize + 1];
		for (int i = 0; i <= boardSize; i++) {
			tierHashers[i] = new C4Hasher(width, height, i);
		}
		xAllMoves = new CacheMove[width * (2 * height - 1)];
		oAllMoves = new CacheMove[width * (2 * height - 1)];
		int moveNum = 0;
		for (int col = 0; col < width; col++) {
			for (int row = 0; row < height; row++) {
				if (row == 0) {
					xAllMoves[moveNum] = new CacheMove(col * height + row, 0, 1);
					oAllMoves[moveNum] = new CacheMove(col * height + row, 0, 2);
					moveNum++;
				} else {
					xAllMoves[moveNum] = new CacheMove(col * height + row - 1,
							1, 1, col * height + row, 0, 1);
					oAllMoves[moveNum] = new CacheMove(col * height + row - 1,
							1, 1, col * height + row, 0, 2);
					moveNum++;
					xAllMoves[moveNum] = new CacheMove(col * height + row - 1,
							2, 2, col * height + row, 0, 1);
					oAllMoves[moveNum] = new CacheMove(col * height + row - 1,
							2, 2, col * height + row, 0, 2);
					moveNum++;
				}
			}
		}
		for (int i = 0; i < boardSize; i++) {
			cacheHashers[i] = new CacheHasher<FixedState>(tierHashers[i],
					tierHashers[i + 1], i % 2 == 0 ? xAllMoves : oAllMoves,
					true);
		}
		cacheHashers[boardSize] = new CacheHasher<FixedState>(
				tierHashers[boardSize], new C4Hasher[0], new CacheMove[0], true);
		children = new long[width];
		if (GenHasher.useToughAsserts())
			testCachePlaces = new int[width];
		else
			testCachePlaces = null;
	}

	@Override
	public void setState(TierState pos) {
		tier = pos.tier;
		cacheHashers[tier].unhash(pos.hash);
		match();
	}

	private void match() {
		myBoard.clear();
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				char c = get(row, col);
				if (c != ' ')
					myBoard.addPiece(row, col, c);
			}
		}
	}

	@Override
	public Value primitiveValue() {
		if (myBoard.xInALine(piecesInARow, getTurn() == 'X' ? 'O' : 'X') != 0)
			return Value.LOSE;
		else if (tier == boardSize)
			return Value.TIE;
		else
			return Value.UNDECIDED;
	}

	private char getTurn() {
		return tier % 2 == 0 ? 'X' : 'O';
	}

	@Override
	public Collection<Pair<String, TierState>> validMoves() {
		TierState[] ts = newStateArray(width);
		int numMoves = validMoves(ts);
		ArrayList<Pair<String, TierState>> result = new ArrayList<Pair<String, TierState>>();
		int col = 0;
		for (int i = 0; i < numMoves; i++) {
			while (col < width && get(height - 1, col) != ' ')
				col++;
			result.add(new Pair<String, TierState>(Integer.toString(col), ts[i]));
			col++;
		}
		return result;
	}

	@Override
	public int getTier() {
		return tier;
	}

	@Override
	public String stateToString() {
		StringBuilder sb = new StringBuilder(boardSize);
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				sb.append(get(row, col));
			}
		}
		return sb.toString();
	}

	private char get(int row, int col) {
		return charOf(cacheHashers[tier].get(tierHashers[tier]
				.indexOf(row, col)));
	}

	private char charOf(int i) {
		switch (i) {
		case 0:
			return ' ';
		case 1:
			return 'X';
		case 2:
			return 'O';
		default:
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public void setFromString(String pos) {
		int[] seq = new int[width * height];
		tier = 0;
		int i = 0;
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				char c = pos.charAt(i++);
				seq[tierHashers[0].indexOf(row, col)] = getInt(c);
				if (c != ' ')
					tier++;
			}
		}
		FixedState state = tierHashers[tier].newState();
		tierHashers[tier].set(state, seq);
		cacheHashers[tier].hash(state);
	}

	private int getInt(char c) {
		switch (c) {
		case 'X':
			return 1;
		case 'O':
			return 2;
		case ' ':
			return 0;
		default:
			throw new Error("Bad char " + c);
		}
	}

	@Override
	public void getState(TierState state) {
		state.tier = tier;
		state.hash = cacheHashers[tier].getHash();
	}

	@Override
	public long numHashesForTier(int tier) {
		return tierHashers[tier].totalPositions();
	}

	@Override
	public String displayState() {
		return myBoard.toString();
	}

	@Override
	public void setStartingPosition(int n) {
		tier = 0;
		cacheHashers[0].unhash(0);
	}

	@Override
	public int numStartingPositions() {
		return 1;
	}

	@Override
	public boolean hasNextHashInTier() {
		return cacheHashers[tier].hasNext();
	}

	@Override
	public void nextHashInTier() {
		int changedTo = cacheHashers[tier].next();
		int i = 0;
		OUTER: for (int col = 0; col < width; col++) {
			for (int row = 0; row < height; row++) {
				myBoard.setPiece(row, col, get(row, col));
				i++;
				if (i > changedTo)
					break OUTER;
			}
		}
		assert !GenHasher.useToughAsserts() || matches();
	}

	private boolean matches() {
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				if (myBoard.getPiece(row, col) != charOf(cacheHashers[tier]
						.get(tierHashers[tier].indexOf(row, col))))
					return false;
			}
		}
		return true;
	}

	@Override
	public int numberOfTiers() {
		return boardSize + 1;
	}

	@Override
	public int maxChildren() {
		return width;
	}

	public int validMoves(TierState[] children, int[] cachePlaces) {
		if (cachePlaces == null)
			cachePlaces = testCachePlaces;
		int numChildren = cacheHashers[tier].getChildren(cachePlaces,
				this.children);
		for (int i = 0; i < numChildren; i++) {
			children[i].tier = tier + 1;
			children[i].hash = this.children[i];
		}
		if (GenHasher.useToughAsserts()) {
			FixedState pState = tierHashers[tier].getPoolState();
			FixedState cState = tierHashers[tier + 1].getPoolState();
			cacheHashers[tier].getState(pState);
			for (int i = 0; i < numChildren; i++) {
				tierHashers[tier + 1].makeMove(pState, getMove(cachePlaces[i]),
						cState);
				assert tierHashers[tier + 1].hash(cState) == children[i].hash;
			}
			tierHashers[tier].release(pState);
			tierHashers[tier + 1].release(cState);
		}
		return numChildren;
	}

	private CacheMove getMove(int i) {
		if (tier % 2 == 0)
			return xAllMoves[i];
		else
			return oAllMoves[i];
	}

	@Override
	public int validMoves(TierState[] moves) {
		return validMoves(moves, null);
	}

	@Override
	public String describe() {
		return width + " x " + height + " Connect " + piecesInARow;
	}

	@Override
	public long recordStates() {
		return boardSize + 2;
	}

	@Override
	public void longToRecord(TierState recordState, long record, Record toStore) {
		if (record == boardSize + 1) {
			toStore.value = Value.TIE;
			toStore.remoteness = boardSize - recordState.tier;
		} else {
			toStore.value = record % 2 == 1 ? Value.WIN : Value.LOSE;
			toStore.remoteness = (int) record;
		}
	}

	@Override
	public long recordToLong(TierState recordState, Record fromRecord) {
		if (fromRecord.value == Value.TIE)
			return boardSize + 1;
		else
			return fromRecord.remoteness;
	}

	@Override
	public TierCache getCache(Database db, long availableMem) {
		if (tier + 1 == numberOfTiers())
			return null;
		else
			return new GenCache<FixedState>(db, availableMem,
					tierHashers[tier], tierHashers[tier + 1],
					tier % 2 == 0 ? xAllMoves : oAllMoves, this, tier);
	}

}