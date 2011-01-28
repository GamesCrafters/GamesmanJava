package edu.berkeley.gamesman.game;

import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.game.util.TierState;
import edu.berkeley.gamesman.hasher.DartboardHasher2;
import edu.berkeley.gamesman.hasher.QuartoMinorHasher;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

public class Quarto extends TierGame {
	private final DartboardHasher2 majorHasher = new DartboardHasher2(' ', 'P');
	private final QuartoMinorHasher minorHasher = new QuartoMinorHasher();
	private int tier;
	private final Piece[][] pieces;

	private class Piece {
		private final int majorIndex;
		private int minorIndex;

		private Piece(int index) {
			majorIndex = index;
		}

		private int get() {
			if (majorHasher.get(majorIndex) == ' ')
				return 0;
			else
				return minorHasher.get(minorIndex);
		}

		private int getFlip() {
			if (majorHasher.get(majorIndex) == ' ')
				return 0;
			else
				return minorHasher.get(minorIndex) ^ 15;
		}

		private boolean hasPiece() {
			return majorHasher.get(majorIndex) == 'P';
		}

		public String toBinaryString() {
			StringBuilder sb = new StringBuilder(4);
			int val = get();
			for (int i = 0; i < 4; i++) {
				sb.append(val & 1);
				val >>= 1;
			}
			return sb.toString();
		}
	}

	public Quarto(Configuration conf) {
		super(conf);
		majorHasher.setReplacements(' ', 'P');
		pieces = new Piece[4][4];
		int i = 0;
		for (int row = 0; row < 4; row++) {
			for (int col = 0; col < 4; col++) {
				pieces[row][col] = new Piece(i++);
			}
		}
	}

	@Override
	public void setState(TierState pos) {
		setTier(tier);
		long tierHashes = minorHasher.numHashesForTier(tier);
		long majorHash = pos.hash / tierHashes;
		long minorHash = pos.hash % tierHashes;
		majorHasher.unhash(majorHash);
		setIndices();
		minorHasher.unhash(minorHash);
	}

	private void setIndices() {
		int c = 0;
		int i = 0;
		for (int row = 0; row < 4; row++) {
			for (int col = 0; col < 4; col++) {
				if (majorHasher.get(i++) == 'P') {
					pieces[row][col].minorIndex = c++;
				} else
					pieces[row][col].minorIndex = -1;
			}
		}
	}

	private void setTier(int tier) {
		this.tier = tier;
		majorHasher.setNums(16 - tier, tier);
		minorHasher.setTier(tier);
	}

	@Override
	public Value primitiveValue() {
		int unflippedDL = 15, flippedDL = 15, unflippedDR = 15, flippedDR = 15;
		for (int i = 0; i < 4; i++) {
			int unflippedH = 15, flippedH = 15, unflippedV = 15, flippedV = 15;
			for (int k = 0; k < 4; k++) {
				unflippedH &= pieces[i][k].get();
				flippedH &= pieces[i][k].getFlip();
				unflippedV &= pieces[k][i].get();
				flippedV &= pieces[k][i].getFlip();
			}
			if ((unflippedH | flippedH | unflippedV | flippedV) > 0)
				return Value.LOSE;
			unflippedDL &= pieces[i][3 - i].get();
			flippedDL &= pieces[i][3 - i].getFlip();
			unflippedDR &= pieces[i][i].get();
			flippedDR &= pieces[i][i].getFlip();
		}
		return (unflippedDL | flippedDL | unflippedDR | flippedDR) > 0 ? Value.LOSE
				: (tier == 16 ? Value.TIE : Value.UNDECIDED);
	}

	@Override
	public Collection<Pair<String, TierState>> validMoves() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getTier() {
		return tier;
	}

	@Override
	public String stateToString() {
		StringBuilder sb = new StringBuilder(16);
		for (int row = 0; row < 4; row++) {
			for (int col = 0; col < 4; col++) {
				if (pieces[row][col].hasPiece()) {
					sb.append((char) ('A' + pieces[row][col].get()));
				} else
					sb.append(" ");
			}
		}
		return sb.toString();
	}

	@Override
	public void setFromString(String pos) {
		int[] pieces = new int[16];
		int pieceCount = 0;
		char[] posArr = pos.toCharArray();
		int i = 0;
		for (int row = 0; row < 4; row++) {
			for (int col = 0; col < 4; col++) {
				char c = posArr[i];
				if (c == ' ')
					this.pieces[row][col].minorIndex = -1;
				else {
					posArr[i] = 'P';
					pieces[pieceCount] = c - 'A';
					this.pieces[row][col].minorIndex = pieceCount;
					pieceCount++;
				}
				i++;
			}
		}
		setTierAndPosition(pieceCount, posArr, pieces);
	}

	private void setTierAndPosition(int tier, char[] posArr, int[] pieces) {
		majorHasher.setNumsAndHash(posArr);
		minorHasher.setTierAndHash(tier, pieces);
	}

	@Override
	public void getState(TierState state) {
		state.tier = tier;
		long tierHashes = minorHasher.numHashesForTier(tier);
		state.hash = majorHasher.getHash() * tierHashes + minorHasher.getHash();
	}

	@Override
	public long numHashesForTier(int tier) {
		return Util.nCr(16, tier) * minorHasher.numHashesForTier(tier);
	}

	@Override
	public String displayState() {
		StringBuilder sb = new StringBuilder(80);
		for (int row = 3; row >= 0; row--) {
			for (int col = 0; col < 4; col++) {
				if (pieces[row][col].hasPiece()) {
					sb.append(pieces[row][col].toBinaryString());
				} else {
					sb.append("----");
				}
				if (col < 3)
					sb.append(" ");
				else
					sb.append("\n");
			}
		}
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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void nextHashInTier() {
		// TODO Auto-generated method stub

	}

	@Override
	public int numberOfTiers() {
		return 17;
	}

	@Override
	public int maxChildren() {
		return 256;
	}

	@Override
	public int validMoves(TierState[] moves) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String describe() {
		return "Quarto";
	}

	@Override
	public long recordStates() {
		return 18;
	}

	@Override
	public void longToRecord(TierState recordState, long record, Record toStore) {
		if (record == 17) {
			toStore.value = Value.TIE;
			toStore.remoteness = 16 - recordState.tier;
		} else {
			toStore.value = (record & 1) == 1 ? Value.WIN : Value.LOSE;
			toStore.remoteness = (int) record;
		}
	}

	@Override
	public long recordToLong(TierState recordState, Record fromRecord) {
		if (fromRecord.value == Value.TIE)
			return 17L;
		else
			return fromRecord.remoteness;
	}

}
