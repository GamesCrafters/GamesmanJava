package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.TierReadCache;
import edu.berkeley.gamesman.game.util.DartboardCacher;
import edu.berkeley.gamesman.game.util.TierState;
import edu.berkeley.gamesman.hasher.DartboardHasher;
import edu.berkeley.gamesman.hasher.RearrangeHasher;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * A superclass for hex-style dartboard games in which the objective is to
 * connect across the board somehow
 * 
 * @author dnspies
 */
public abstract class ConnectGame extends TierGame {
	private final DartboardHasher mmh;
	private final RearrangeHasher otherHasher;
	private int tier;
	private final long[] moveHashes;
	private final DartboardCacher myCacher;

	/**
	 * @param conf
	 *            The configuration object
	 */
	public ConnectGame(Configuration conf) {
		super(conf);
		int boardSize;
		// You can't call getBoardSize because the board size hasn't been
		// initialized yet (the child constructor hasn't been called).
		// It's either this or lazy initialization for the DartboardHasher
		if (this instanceof YGame) {
			int innerTriangleSegments = conf.getInteger(
					"gamesman.game.centerRows", 3) - 1;
			int outerRows = conf.getInteger("gamesman.game.outerRows", 2);
			boardSize = (innerTriangleSegments + 1)
					* (innerTriangleSegments + 2) / 2
					+ (innerTriangleSegments * 2 + outerRows + 1) * outerRows
					/ 2 * 3;
		} else if (this instanceof Connections) {
			int boardSide = conf.getInteger("gamesman.game.side", 4);
			boardSize = boardSide * boardSide + (boardSide - 1)
					* (boardSide - 1);
		} else if (this instanceof Y2) {
			int centerRows = conf.getInteger("gamesman.game.centerRows", 2);
			int outerRows = conf.getInteger("gamesman.game.outerRows", 2);
			boardSize = centerRows * (centerRows + 1) / 2
					+ (centerRows * 2 + outerRows - 1) * outerRows / 2 * 3;
		} else {
			throw new Error("Subclass not known to calculate board size");
		}
		boolean useRearrangeHasher = conf.getBoolean(
				"gamesman.game.rearrange.hasher", false);
		if (useRearrangeHasher) {
			mmh = null;
			otherHasher = new RearrangeHasher(boardSize);
			myCacher = null;
		} else {
			mmh = new DartboardHasher(boardSize, ' ', 'O', 'X');
			otherHasher = null;
			myCacher = new DartboardCacher(conf, mmh);
		}
		moveHashes = new long[boardSize];
	}

	protected abstract int getBoardSize();

	@Override
	public final void getState(TierState state) {
		state.tier = tier;
		state.hash = hashInTier();
	}

	private final long hashInTier() {
		if (mmh == null)
			return otherHasher.getHash();
		else
			return mmh.getHash();
	}

	@Override
	public final int getTier() {
		return tier;
	}

	@Override
	public final boolean hasNextHashInTier() {
		return numHashesForTier() - 1 > hashInTier();
	}

	private long numHashesForTier() {
		if (mmh == null)
			return otherHasher.numHashes();
		else
			return mmh.numHashes();
	}

	@Override
	public final int maxChildren() {
		return getBoardSize();
	}

	@Override
	public final void nextHashInTier() {
		if (mmh == null)
			otherHasher.next();
		else
			mmh.next();
	}

	@Override
	public final long numHashesForTier(int tier) {
		return Util.nCr(getBoardSize(), tier) * Util.nCr(tier, tier / 2);
	}

	@Override
	public final int numStartingPositions() {
		return 1;
	}

	@Override
	public final int numberOfTiers() {
		return getBoardSize() + 1;
	}

	@Override
	public final void setFromString(String pos) {
		char[] arr = convertInString(pos);
		int xCount = 0, oCount = 0;
		for (int i = 0; i < arr.length; i++) {
			if (arr[i] == 'X')
				xCount++;
			else if (arr[i] == 'O')
				oCount++;
			else if (arr[i] != ' ')
				throw new Error("Bad board string");
		}
		if (xCount - oCount < 0 || xCount - oCount > 1)
			throw new Error("Bad board string");
		tier = xCount + oCount;
		setTurn(tier % 2 == 0 ? 'X' : 'O');
		setNumsAndHash(arr);
		setToCharArray(arr);
	}

	private final void setTurn(char c) {
		if (mmh != null)
			mmh.setReplacements(' ', c);
	}

	@Override
	public final void setStartingPosition(int n) {
		tier = 0;
		if (mmh == null) {
			otherHasher.setNums(getBoardSize(), 0, 0);
		} else {
			mmh.setNums(getBoardSize(), 0, 0);
			mmh.setReplacements(' ', 'X');
		}
	}

	@Override
	public final void setState(TierState pos) {
		tier = pos.tier;
		if (mmh == null) {
			otherHasher
					.setNums(getBoardSize() - tier, tier / 2, (tier + 1) / 2);
			otherHasher.unhash(pos.hash);
		} else {
			mmh.setNums(getBoardSize() - tier, tier / 2, (tier + 1) / 2);
			mmh.setReplacements(' ', tier % 2 == 0 ? 'X' : 'O');
			mmh.unhash(pos.hash);
		}
	}

	@Override
	public final Collection<Pair<String, TierState>> validMoves() {
		char turn = tier % 2 == 0 ? 'X' : 'O';
		if (mmh == null)
			otherHasher.getChildren(' ', turn, moveHashes);
		else
			mmh.getChildren(' ', turn, moveHashes);
		ArrayList<Pair<String, TierState>> moves = new ArrayList<Pair<String, TierState>>(
				moveHashes.length);
		for (int i = 0; i < moveHashes.length; i++) {
			if (moveHashes[i] >= 0) {
				moves.add(new Pair<String, TierState>(Integer
						.toString(translateOut(i)), newState(tier + 1,
						moveHashes[i])));
			}
		}
		return moves;
	}

	/**
	 * @param i
	 *            The index into the char array
	 * @return The index into the passed game string.
	 */
	public int translateOut(int i) {
		return i;
	}

	@Override
	public final int validMoves(TierState[] moves) {
		char turn = tier % 2 == 0 ? 'X' : 'O';
		if (mmh == null)
			otherHasher.getChildren(' ', turn, moveHashes);
		else
			mmh.getChildren(' ', turn, moveHashes);
		int numChildren = 0;
		for (int i = 0; i < moveHashes.length; i++) {
			if (moveHashes[i] >= 0) {
				moves[numChildren].tier = tier + 1;
				moves[numChildren].hash = moveHashes[i];
				numChildren++;
			}
		}
		return numChildren;
	}

	@Override
	public final long recordStates() {
		if (conf.hasRemoteness)
			return getBoardSize() + 1;
		else
			return 2;
	}

	protected final void getCharArray(char[] arr) {
		if (mmh == null)
			otherHasher.getCharArray(arr);
		else
			mmh.getCharArray(arr);
	}

	protected final char[] makeCharArray() {
		char[] arr = new char[getBoardSize()];
		getCharArray(arr);
		return arr;
	}

	protected final char get(int i) {
		if (mmh == null)
			return otherHasher.get(i);
		else
			return mmh.get(i);
	}

	@Override
	public final String stateToString() {
		return convertOutString(makeCharArray());
	}

	public char[] convertInString(String s) {
		return s.toCharArray();
	}

	public String convertOutString(char[] charArray) {
		return new String(charArray);
	}

	protected abstract void setToCharArray(char[] myPieces);

	@Override
	public final Value primitiveValue() {
		Value result;
		if ((tier & 1) == 1)
			result = isWin('X') ? Value.LOSE : Value.UNDECIDED;
		else
			result = isWin('O') ? Value.LOSE : Value.UNDECIDED;
		assert Util.debug(DebugFacility.GAME, result.name() + "\n");
		if (tier == numberOfTiers() - 1 && result == Value.UNDECIDED)
			return Value.IMPOSSIBLE;
		else
			return result;
	}

	@Override
	public final long recordToLong(TierState recordState, Record fromRecord) {
		if (conf.hasRemoteness) {
			return fromRecord.remoteness;
		} else {
			switch (fromRecord.value) {
			case LOSE:
				return 0L;
			case WIN:
				return 1L;
			default:
				return 0L;
			}
		}
	}

	@Override
	public final void longToRecord(TierState recordState, long state,
			Record toStore) {
		if ((state & 1) == 1)
			toStore.value = Value.WIN;
		else
			toStore.value = Value.LOSE;
		if (conf.hasRemoteness)
			toStore.remoteness = (int) state;
	}

	protected abstract boolean isWin(char c);

	@Override
	public final TierReadCache getCache(Database db, long numPositions,
			long availableMem) {
		return myCacher.getCache(db, numPositions, availableMem, tier,
				hashOffsetForTier(tier + 1));
	}

	@Override
	public final TierReadCache nextCache() {
		return myCacher.nextCache();
	}

	public final long setNumsAndHash(char[] pieces) {
		if (mmh == null)
			return otherHasher.setNumsAndHash(pieces);
		else
			return mmh.setNumsAndHash(pieces);
	}

	public final void set(int index, char piece) {
		if (mmh == null)
			throw new UnsupportedOperationException();
		else
			mmh.set(index, piece);
	}
}