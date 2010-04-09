package edu.berkeley.gamesman.game;

import java.util.Collection;

import edu.berkeley.gamesman.core.*;
import edu.berkeley.gamesman.game.util.ItergameState;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * A superclass for hex-style dartboard games in which the objective is to
 * connect across the board somehow
 * 
 * @author dnspies
 */
public abstract class ConnectGame extends TieredIterGame {
	private char turn;

	protected final ItergameState myState = newState();

	/**
	 * @param conf
	 *            The configuration object
	 */
	public void initialize(Configuration conf) {
		super.initialize(conf);
	}

	protected abstract int getBoardSize();

	@Override
	public ItergameState getState() {
		return myState;
	}

	@Override
	public int getTier() {
		return myState.tier;
	}

	@Override
	public boolean hasNextHashInTier() {
		return myState.hash < numHashesForTier() - 1;
	}

	@Override
	public int maxChildren() {
		return getBoardSize();
	}

	@Override
	public void nextHashInTier() {
		myState.hash++;
		gameMatchState();
	}

	@Override
	public long numHashesForTier() {
		char[] arr = getCharArray();
		int tier = getTier();
		return Util.nCr(arr.length, tier) * Util.nCr(tier, tier / 2);
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
		setToCharArray(pos.toCharArray());
		stateMatchGame();
	}

	private void stateMatchGame() {
		// TODO Auto-generated method stub

	}

	private void gameMatchState() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setStartingPosition(int n) {
		char[] arr = getCharArray();
		int size = getBoardSize();
		for (int i = 0; i < size; i++)
			arr[i] = ' ';
		setToCharArray(arr);
		myState.tier = 0;
		myState.hash = 0;
	}

	@Override
	public void setState(ItergameState pos) {
		myState.set(pos);
		gameMatchState();
	}

	@Override
	public void setTier(int tier) {
		myState.tier = tier;
		myState.hash = 0;
		gameMatchState();
	}

	@Override
	public Collection<Pair<String, ItergameState>> validMoves() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int validMoves(ItergameState[] moves) {
		char[] pieces = getCharArray();
		int c = 0;
		for (int i = 0; i < pieces.length; i++) {
			if (pieces[i] == ' ') {
				pieces[i] = turn;
				stateMatchGame();
				moves[c].set(myState);
				pieces[i] = ' ';
				c++;
			}
		}
		stateMatchGame();
		return c;
	}

	private final class ConnectRecord extends Record {
		protected ConnectRecord() {
			super(conf);
		}

		protected ConnectRecord(long state) {
			super(conf);
			set(state);
		}

		protected ConnectRecord(PrimitiveValue pVal) {
			super(conf, pVal);
		}

		@Override
		public long getState() {
			if (conf.remotenessStates > 0) {
				return remoteness;
			} else {
				switch (value) {
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
		public void set(long state) {
			if ((state & 1) == 1)
				value = PrimitiveValue.WIN;
			else
				value = PrimitiveValue.LOSE;
			if (conf.remotenessStates > 0)
				remoteness = (int) state;
		}
	}

	@Override
	public Record newRecord(PrimitiveValue pv) {
		return new ConnectRecord(pv);
	}

	@Override
	public Record newRecord() {
		return new ConnectRecord();
	}

	@Override
	public Record newRecord(long val) {
		return new ConnectRecord(val);
	}

	@Override
	public long recordStates() {
		if (conf.remotenessStates > 0)
			return getBoardSize() + 1;
		else
			return 2;
	}

	protected abstract char[] getCharArray();

	@Override
	public String stateToString() {
		return new String(getCharArray());
	}

	protected abstract void setToCharArray(char[] myPieces);

	@Override
	public PrimitiveValue primitiveValue() {
		if ((myState.tier & 1) == 1)
			return isWin('X') ? PrimitiveValue.LOSE : PrimitiveValue.UNDECIDED;
		else
			return isWin('O') ? PrimitiveValue.LOSE : PrimitiveValue.UNDECIDED;
	}

	protected abstract boolean isWin(char c);
}
