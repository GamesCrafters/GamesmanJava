package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.game.util.TierState;
import edu.berkeley.gamesman.hasher.RearrangeHasher;
import edu.berkeley.gamesman.util.Pair;

public abstract class RearrangeDartboardGame extends RearrangeTierGame {
	protected final int gameWidth, gameHeight, gameSize;
	protected final RearrangeHasher myHasher;
	private int tier;
	private final long[] myChildren;
	private final int tieType;
	private boolean majorChanged = true;
	protected static final int NO_TIE = 0, LAST_MOVE_TIE = 1, ANY_TIE = 2;

	public RearrangeDartboardGame(Configuration conf, int tieType) {
		super(conf);
		gameWidth = conf.getInteger("gamesman.game.width", 3);
		gameHeight = conf.getInteger("gamesman.game.height", 3);
		gameSize = gameWidth * gameHeight;
		myHasher = new RearrangeHasher(gameSize);
		myChildren = new long[gameSize];
		this.tieType = tieType;
	}

	protected char get(int row, int col) {
		return myHasher.get(row * gameWidth + col);
	}

	@Override
	public void setState(TierState pos) {
		setTier(pos.tier);
		myHasher.unhash(pos.hash);
		majorChanged = true;
	}

	@Override
	public Collection<Pair<String, TierState>> validMoves() {
		myHasher.getChildren(' ', tier % 2 == 0 ? 'X' : 'O', myChildren);
		ArrayList<Pair<String, TierState>> validMoves = new ArrayList<Pair<String, TierState>>(
				gameSize - tier);
		int pos = 0;
		for (int row = 0; row < gameHeight; row++) {
			for (int col = 0; col < gameWidth; col++) {
				if (myChildren[pos] >= 0) {
					validMoves.add(new Pair<String, TierState>(String
							.valueOf((char) (col + 'A')) + (row + 1), newState(
							tier + 1, myChildren[pos])));
				}
				pos++;
			}
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
		majorChanged = true;
	}

	@Override
	public void getState(TierState state) {
		state.tier = tier;
		state.hash = myHasher.getHash();
	}

	@Override
	public long numHashesForTier(int tier) {
		setTier(tier);
		return myHasher.numHashes();
	}

	protected void setTier(int tier) {
		this.tier = tier;
		myHasher.setNums(gameSize - tier, tier / 2, (tier + 1) / 2);
		majorChanged = true;
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
		if(myHasher.majorChanged())
			majorChanged = true;
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
	public int validMoves(TierState[] moves) {
		myHasher.getChildren(' ', tier % 2 == 0 ? 'X' : 'O', myChildren);
		int moveCount = 0;
		for (int i = 0; i < myChildren.length; i++) {
			if (myChildren[i] >= 0) {
				moves[moveCount].tier = tier + 1;
				moves[moveCount].hash = myChildren[i];
				moveCount++;
			}
		}
		return moveCount;
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

	@Override
	public String toString() {
		return displayState();
	}

	@Override
	public boolean majorChanged() {
		if (majorChanged) {
			majorChanged = false;
			return true;
		} else
			return false;
	}
}
