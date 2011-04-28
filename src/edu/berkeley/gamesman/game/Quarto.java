package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.cache.QuartoCache;
import edu.berkeley.gamesman.database.cache.TierCache;
import edu.berkeley.gamesman.game.util.TierState;
import edu.berkeley.gamesman.hasher.ChangedIterator;
import edu.berkeley.gamesman.hasher.DartboardHasher;
import edu.berkeley.gamesman.hasher.QuartoMinorHasher;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * Quarto: There are 16 unique pieces with 4 binary features. Players alternate
 * playing a piece and picking up a piece which the opponent must play next. A
 * player wins when they complete a set of four pieces in a row which share any
 * one feature. The game state is only distinguishable up to logical symmetries
 * (ie (0110 1101) = (0000 1011) = (0000 0111))
 * 
 * @author dnspies
 */
public class Quarto extends TierGame {
	private final DartboardHasher majorHasher = new DartboardHasher(16, ' ',
			'P');
	private final ChangedIterator myChanged = new ChangedIterator(16);
	private final QuartoMinorHasher minorHasher = new QuartoMinorHasher();
	private int tier;
	/**
	 * The 4x4 board of pieces.
	 */
	public final Piece[][] pieces;
	/**
	 * A 1-dimensional array containing the same pieces as pieces, but in
	 * row-major order
	 */
	public final Piece[] placeList;
	private final int[] places = new int[16];
	private final long[] majorChildren = new long[16];
	private final long[] minorChildren = new long[256];
	private final char[] posArr = new char[16];

	public class Piece {
		public final int majorIndex;
		private int minorIndex;

		public int getMinorIndex() {
			return minorIndex;
		}

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

		public boolean hasPiece() {
			return majorHasher.get(majorIndex) == 'P';
		}

		public String toBinaryString() {
			StringBuilder sb = new StringBuilder(4);
			int val = get();
			for (int i = 3; i >= 0; i--) {
				sb.append((val >>> i) & 1);
			}
			return sb.toString();
		}
	}

	public Quarto(Configuration conf) {
		super(conf);
		majorHasher.setNums(16, 0);
		majorHasher.setReplacements(' ', 'P');
		pieces = new Piece[4][4];
		placeList = new Piece[16];
		int i = 0;
		for (int row = 0; row < 4; row++) {
			for (int col = 0; col < 4; col++) {
				placeList[i] = new Piece(i);
				pieces[row][col] = placeList[i];
				i++;
			}
		}
	}

	@Override
	public void setState(TierState pos) {
		setTier(pos.tier);
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
		TierState[] moves = newStateArray(maxChildren());
		int numChildren = validMoves(moves);
		int child = 0;
		ArrayList<Pair<String, TierState>> movePairs = new ArrayList<Pair<String, TierState>>(
				numChildren);
		for (int childPiece = 0; childPiece < 16; childPiece++) {
			if (minorHasher.used(childPiece))
				continue;
			for (int row = 0; row < 4; row++) {
				for (int col = 0; col < 4; col++) {
					if (!pieces[row][col].hasPiece()) {
						movePairs.add(new Pair<String, TierState>(Character
								.toString((char) ('A' + childPiece))
								+ Character.toString((char) ('a' + col))
								+ Integer.toString(row + 1), moves[child++]));
					}
				}
			}
		}
		return movePairs;
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
					sb.append("\n\n");
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
		return majorHasher.getHash() < majorHasher.numHashes() - 1
				|| minorHasher.getHash() < minorHasher.numHashesForTier(tier) - 1;
	}

	@Override
	public void nextHashInTier() {
		if (minorHasher.getHash() < minorHasher.numHashesForTier(tier) - 1)
			minorHasher.nextHashInTier();
		else {
			minorHasher.reset();
			majorHasher.next(myChanged);
			int count = 0;
			int lastIndex = myChanged.hasNext() ? myChanged.next() : -1;
			OUTER: for (int row = 0; row < 4; row++) {
				for (int col = 0; col < 4; col++) {
					int majorIndex = pieces[row][col].majorIndex;
					while (majorIndex > lastIndex) {
						if (myChanged.hasNext())
							lastIndex = myChanged.next();
						else
							break OUTER;
					}
					if (majorHasher.get(majorIndex) == 'P')
						pieces[row][col].minorIndex = count++;
					else
						pieces[row][col].minorIndex = -1;
				}
			}
		}
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
		return validMoves(moves, null);
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

	public boolean used(int i) {
		return minorHasher.used(i);
	}

	public boolean usedPlace(int i) {
		return majorHasher.get(i) == 'P';
	}

	public TierCache getCache(Database db, long availableMem) {
		return new QuartoCache(this, majorHasher, minorHasher, db, availableMem);
	}

	public int validMoves(TierState[] children, int[] cachePlaces) {
		int numMajor = majorHasher.getChildren(' ', 'P', places, majorChildren);
		int numMinor = minorHasher.getChildren(minorChildren);
		assert numMajor == 16 - tier;
		assert numMinor == (16 - tier) * (tier + 1);
		long tierHashes = minorHasher.numHashesForTier(tier + 1);
		int numMoves = 0;
		int minorPlace = 0;
		int cachePlace = 0;
		int thisPiece = 0;
		for (int childPiece = 0; childPiece < numMajor; childPiece++) {
			while (minorHasher.used(thisPiece)) {
				thisPiece++;
				cachePlace += 16;
			}
			int majorPlace = 0;
			for (int i = 0; i < 16; i++) {
				if (majorHasher.get(i) == ' ') {
					long majorHash = majorChildren[majorPlace++];
					long minorHash = minorChildren[minorPlace];
					children[numMoves].tier = tier + 1;
					children[numMoves].hash = majorHash * tierHashes
							+ minorHash;
					if (cachePlaces != null)
						cachePlaces[numMoves] = cachePlace;
					numMoves++;
				} else
					minorPlace++;
				cachePlace++;
			}
			minorPlace++;
			thisPiece++;
		}
		assert numMoves == (16 - tier) * (16 - tier);
		return numMoves;
	}

	/**
	 * @param qs A StrictQuarto state
	 * @return The with-symmetry hash
	 */
	public synchronized long getHash(StrictQuarto.QuartoState qs) {
		int[] pieces = qs.getPieces();
		int pieceCount = 0;
		int i = 0;
		for (int row = 0; row < 4; row++) {
			for (int col = 0; col < 4; col++) {
				if (qs.filled(row, col)) {
					posArr[i] = 'P';
					this.pieces[row][col].minorIndex = pieceCount;
					pieceCount++;
				} else {
					posArr[i] = ' ';
					this.pieces[row][col].minorIndex = -1;
				}
				i++;
			}
		}
		setTierAndPosition(pieceCount, posArr, pieces);
		long tierHashes = minorHasher.numHashesForTier(tier);
		return hashForTierAndOffset(tier, majorHasher.getHash() * tierHashes
				+ minorHasher.getHash());
	}
}
