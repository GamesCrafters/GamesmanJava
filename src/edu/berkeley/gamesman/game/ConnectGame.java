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
	protected final DartboardHasher mmh;
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
		if (this instanceof YGame) {
			/*
			 * int boardSide = conf.getInteger("gamesman.game.outerRows", 2) +
			 * 2; boardSize = boardSide * (boardSide - 1) / 2 * 3;
			 */
			boardSize = this.getBoardSize();
		} else if (this instanceof Connections) {
			int boardSide = conf.getInteger("gamesman.game.side", 4);
			boardSize = boardSide * boardSide + (boardSide - 1)
					* (boardSide - 1);
		} else {
			throw new Error("Subclass not known to calculate board size");
		}
		mmh = new DartboardHasher(boardSize, ' ', 'O', 'X');
		moveHashes = new long[boardSize];
		myCacher = new DartboardCacher(conf, mmh);
	}

	protected abstract int getBoardSize();

	@Override
	public void getState(TierState state) {
		state.tier = tier;
		state.hash = mmh.getHash();
	}

	@Override
	public int getTier() {
		return tier;
	}

	@Override
	public boolean hasNextHashInTier() {
		return mmh.numHashes() - 1 > mmh.getHash();
	}

	@Override
	public int maxChildren() {
		return getBoardSize();
	}

	@Override
	public void nextHashInTier() {
		mmh.next();
	}

	@Override
	public long numHashesForTier(int tier) {
		return Util.nCr(getBoardSize(), tier) * Util.nCr(tier, tier / 2);
	}

	@Override
	public int numStartingPositions() {
		return 1;
	}

	@Override
	public int numberOfTiers() {
		return getBoardSize() + 1;
	}

	@Override
	public void setFromString(String pos) {
		char[] arr = convertInString(pos);
		mmh.setNumsAndHash(arr);
		setToCharArray(arr);
	}

	@Override
	public void setStartingPosition(int n) {
		tier = 0;
		mmh.setNums(getBoardSize(), 0, 0);
		mmh.setReplacements(' ', 'X');
	}

	@Override
	public void setState(TierState pos) {
		tier = pos.tier;
		mmh.setNums(getBoardSize() - tier, tier / 2, (tier + 1) / 2);
		mmh.setReplacements(' ', tier % 2 == 0 ? 'X' : 'O');
		mmh.unhash(pos.hash);
	}

	@Override
	public Collection<Pair<String, TierState>> validMoves() {
		char turn = tier % 2 == 0 ? 'X' : 'O';
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
	public int validMoves(TierState[] moves) {
		char turn = tier % 2 == 0 ? 'X' : 'O';
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
	public long recordStates() {
		if (conf.hasRemoteness)
			return getBoardSize() + 1;
		else
			return 2;
	}

	protected final void getCharArray(char[] arr) {
		mmh.getCharArray(arr);
	}

	protected final char[] makeCharArray() {
		char[] arr = new char[mmh.boardSize()];
		getCharArray(arr);
		return arr;
	}

	protected final char get(int i) {
		return mmh.get(i);
	}

	@Override
	public String stateToString() {
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
	public Value primitiveValue() {
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
	public long recordToLong(TierState recordState, Record fromRecord) {
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
	public void longToRecord(TierState recordState, long state, Record toStore) {
		if ((state & 1) == 1)
			toStore.value = Value.WIN;
		else
			toStore.value = Value.LOSE;
		if (conf.hasRemoteness)
			toStore.remoteness = (int) state;
	}

	protected abstract boolean isWin(char c);

	@Override
	public TierReadCache getCache(Database db, long numPositions,
			long availableMem) {
		return myCacher.getCache(db, numPositions, availableMem, tier,
				hashOffsetForTier(tier + 1));
	}

	@Override
	public TierReadCache nextCache() {
		return myCacher.nextCache();
	}
}