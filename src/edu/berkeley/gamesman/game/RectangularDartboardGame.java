package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.database.DartboardCache;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.game.util.TierState;
import edu.berkeley.gamesman.hasher.DartboardHasher;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

public abstract class RectangularDartboardGame extends TierGame {
	protected final int gameWidth, gameHeight, gameSize;
	protected final DartboardHasher myHasher;
	private DartboardCache myCache;
	private int tier;
	private final long[] myChildren;
	private final int tieType;
	protected static final int NO_TIE = 0, LAST_MOVE_TIE = 1, ANY_TIE = 2;

	public RectangularDartboardGame(Configuration conf, int tieType) {
		super(conf);
		gameWidth = conf.getInteger("gamesman.game.width", 3);
		gameHeight = conf.getInteger("gamesman.game.height", 3);
		gameSize = gameWidth * gameHeight;
		myChildren = new long[gameSize];
		this.tieType = tieType;
		myHasher = new DartboardHasher(gameSize, ' ', 'O', 'X');
	}

	protected char get(int row, int col) {
		return myHasher.get(row * gameWidth + col);
	}

	@Override
	public void setState(TierState pos) {
		setTier(pos.tier);
		myHasher.unhash(pos.hash);
	}

	@Override
	public Collection<Pair<String, TierState>> validMoves() {
		int[] places = new int[myChildren.length];
		int numChildren = myHasher.getChildren(' ', getTurn(), places,
				myChildren);
		ArrayList<Pair<String, TierState>> validMoves = new ArrayList<Pair<String, TierState>>(
				gameSize - tier);
		for (int child = 0; child < numChildren; child++) {
			int pos = places[child];
			int row = pos / gameWidth, col = pos % gameWidth;
			validMoves.add(new Pair<String, TierState>(String
					.valueOf((char) (col + 'A')) + (row + 1), newState(
					tier + 1, myChildren[child])));
		}
		return validMoves;
	}

	@Override
	public int getTier() {
		return tier;
	}

	@Override
	public String stateToString() {
		return myHasher.toString();
	}

	@Override
	public void setFromString(String pos) {
		char[] pieces = pos.toCharArray();
		int tier = 0;
		for (char c : pieces) {
			if (c != ' ')
				tier++;
		}
		setTier(tier);
		myHasher.hash(pieces);
	}

	@Override
	public void getState(TierState state) {
		state.tier = tier;
		state.hash = myHasher.getHash();
	}

	@Override
	public long numHashesForTier(int tier) {
		return Util.nCr(gameSize, tier) * Util.nCr(tier, tier / 2);
	}

	protected void setTier(int tier) {
		this.tier = tier;
		myHasher.setNums(gameSize - tier, tier / 2, (tier + 1) / 2);
		myHasher.setReplacements(' ', getTurn());
		if (myCache != null) {
			myCache.setReplaceType(' ', getTurn());
		}
	}

	protected char getTurn() {
		return tier % 2 == 0 ? 'X' : 'O';
	}

	@Override
	public String displayState() {
		StringBuilder sb = new StringBuilder((gameWidth + 1) * 2
				* (gameHeight + 1));
		for (int row = gameHeight - 1; row >= 0; row--) {
			sb.append(row + 1);
			for (int col = 0; col < gameWidth; col++) {
				sb.append(" ");
				char piece = myHasher.get(row * gameWidth + col);
				if (piece == ' ')
					sb.append('-');
				else if (piece == 'X' || piece == 'O')
					sb.append(piece);
				else
					throw new Error(piece + " is not a valid piece");
			}
			sb.append("\n");
		}
		sb.append(" ");
		for (int col = 0; col < gameWidth; col++) {
			sb.append(" ");
			sb.append((char) ('A' + col));
		}
		sb.append("\n");
		return sb.toString();
	}

	@Override
	public void setStartingPosition(int n) {
		setTier(0);
	}

	@Override
	public int numStartingPositions() {
		return 1;
	}

	@Override
	public boolean hasNextHashInTier() {
		return myHasher.getHash() < myHasher.numHashes() - 1;
	}

	@Override
	public void nextHashInTier() {
		myHasher.next();
	}

	@Override
	public int numberOfTiers() {
		return gameSize + 1;
	}

	@Override
	public int maxChildren() {
		return gameSize;
	}

	@Override
	public int validMoves(TierState[] moves, int[] cachePlaces) {
		int numChildren = myHasher.getChildren(' ', getTurn(), cachePlaces,
				myChildren);
		for (int i = 0; i < numChildren; i++) {
			moves[i].tier = tier + 1;
			moves[i].hash = myChildren[i];
		}
		return numChildren;
	}

	@Override
	public int validMoves(TierState[] moves) {
		return validMoves(moves, null);
	}

	@Override
	public long recordStates() {
		if (conf.hasRemoteness) {
			switch (tieType) {
			case NO_TIE:
				return gameSize + 1;
			case LAST_MOVE_TIE:
				return gameSize + 2;
			case ANY_TIE:
				return (gameSize + 1) * 2;
			default:
				throw new Error("Bad tie type");
			}
		} else {
			if (tieType == NO_TIE)
				return 2;
			else
				return 3;
		}
	}

	@Override
	public void longToRecord(TierState recordState, long record, Record toStore) {
		if (conf.hasRemoteness) {
			if (record > gameSize) {
				toStore.value = Value.TIE;
				switch (tieType) {
				case NO_TIE:
					throw new Error("Bad record");
				case LAST_MOVE_TIE:
					toStore.remoteness = gameSize - recordState.tier;
					break;
				case ANY_TIE:
					toStore.remoteness = (int) (record - (gameSize + 1));
					break;
				default:
					throw new Error("Bad record");
				}
			} else {
				toStore.value = record % 2 == 0 ? Value.LOSE : Value.WIN;
				toStore.remoteness = (int) record;
			}
		} else {
			switch ((int) record) {
			case 0:
				toStore.value = Value.LOSE;
				break;
			case 1:
				toStore.value = Value.WIN;
				break;
			case 2:
				if (tieType == NO_TIE)
					throw new Error("Bad record");
				else
					toStore.value = Value.TIE;
				break;
			default:
				throw new Error("Bad record");
			}
		}
	}

	@Override
	public long recordToLong(TierState recordState, Record fromRecord) {
		if (conf.hasRemoteness) {
			if (fromRecord.value == Value.TIE) {
				switch (tieType) {
				case NO_TIE:
					throw new Error("Bad record");
				case LAST_MOVE_TIE:
					return gameSize + 1;
				case ANY_TIE:
					return gameSize + 1 + fromRecord.remoteness;
				}
			}
			return fromRecord.remoteness;
		} else {
			switch (fromRecord.value) {
			case LOSE:
				return 0;
			case WIN:
				return 1;
			case TIE:
				if (tieType == NO_TIE)
					throw new Error("Bad record");
				else
					return 2;
			default:
				throw new Error("Bad record");
			}
		}
	}

	protected char get(int i) {
		return myHasher.get(i);
	}

	protected void set(int row, int col, char c) {
		myHasher.set(row * gameWidth + col, c);
	}

	@Override
	public DartboardCache getCache(Database db, long availableMem) {
		if (myCache == null) {
			myCache = new DartboardCache(this, db, availableMem, myHasher);
			myCache.setReplaceType(' ', getTurn());
		}
		return myCache;
	}

	@Override
	public String toString() {
		return displayState();
	}
}
