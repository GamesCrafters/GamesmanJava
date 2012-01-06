package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.util.Pair;

/**
 * Quarto without symmetries so the front-end has something to connect to which
 * won't surprisingly shift the perspective around. Use a QuartoDatabase to wrap
 * the database containing the actual game
 * 
 * @author dnspies
 */
public class StrictQuarto extends Game<StrictQuarto.QuartoState> {
	/**
	 * State for StrictQuarto
	 * 
	 * @author dnspies
	 */
	public static class QuartoState implements State<QuartoState> {
		private int tier = 0;
		private final int[] pieces = new int[16];
		private final boolean[] used = new boolean[16];

		@Override
		public void set(QuartoState qs) {
			tier = qs.tier;
			System.arraycopy(qs.pieces, 0, pieces, 0, 16);
			System.arraycopy(qs.used, 0, used, 0, 16);
		}

		private void reset() {
			tier = 0;
			Arrays.fill(pieces, -1);
			Arrays.fill(used, false);
		}

		private void getUsed(boolean[] used2) {
			System.arraycopy(used, 0, used2, 0, 16);
		}

		private int get(int place) {
			return pieces[place];
		}

		private String toBinaryString(int row, int col) {
			return toBinaryString(pieceIndex(row, col));
		}

		private String toBinaryString(int pieceIndex) {
			StringBuilder sb = new StringBuilder(4);
			int val = pieces[pieceIndex];
			for (int i = 3; i >= 0; i--) {
				sb.append((val >>> i) & 1);
			}
			return sb.toString();
		}

		/**
		 * Whether the piece at row,col has been played
		 * 
		 * @param row
		 *            The row
		 * @param col
		 *            The column
		 * @return Is there a piece there
		 */
		boolean filled(int row, int col) {
			return filled(pieceIndex(row, col));
		}

		private int pieceForPrimitive(int row, int col) {
			int piece = pieces[pieceIndex(row, col)];
			if (piece == -1)
				return 0;
			else
				return piece;
		}

		private int flippedPieceForPrimitive(int row, int col) {
			int piece = pieces[pieceIndex(row, col)];
			if (piece == -1)
				return 0;
			else
				return piece ^ 15;
		}

		private int pieceIndex(int row, int col) {
			return row * 4 + col;
		}

		private boolean filled(int place) {
			return pieces[place] != -1;
		}

		private boolean used(int piece) {
			return used[piece];
		}

		private int tier() {
			return tier;
		}

		private void set(int place, int piece) {
			assert place >= 0 && place < 16;
			assert piece >= -1 && piece < 16;
			if (pieces[place] == -1 && piece != -1)
				tier++;
			else if (pieces[place] != -1 && piece == -1)
				tier--;
			if (pieces[place] != -1)
				used[pieces[place]] = false;
			if (piece != -1)
				used[piece] = true;
			pieces[place] = piece;
		}

		private QuartoState() {
			Arrays.fill(pieces, -1);
		}

		private QuartoState(String pos) {
			char[] c = pos.toCharArray();
			for (int i = 0; i < 16; i++) {
				if (c[i] == ' ')
					pieces[i] = -1;
				else {
					pieces[i] = c[i] - 'A';
					assert !used[pieces[i]];
					used[pieces[i]] = true;
					tier++;
				}
			}
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(16);
			for (int piece : pieces) {
				sb.append(piece == -1 ? ' ' : Character
						.toString((char) ('A' + piece)));
			}
			return sb.toString();
		}

		/**
		 * @return The internal int-array which tells the pieces
		 */
		public int[] getPieces() {
			return pieces;
		}
	}

	private final long[] tierStarts = new long[18];
	private boolean[] used = new boolean[16];

	/**
	 * Standard game constructor
	 * 
	 * @param conf
	 *            The configuration object
	 */
	public StrictQuarto(Configuration conf) {
		super(conf);
		long start = 0L;
		for (int i = 0; i <= 16; i++) {
			tierStarts[i] = start;
			long numHashes = pick(16, i) * choose(16, i);
			start += numHashes;
		}
		tierStarts[17] = start;
	}

	private long pick(int n, int k) {
		if (n < k)
			return 0L;
		else if (k < 0)
			throw new ArithmeticException("Cannot pick: " + n + ", " + k);
		else if (k == 0)
			return 1L;
		else
			return n * pick(n - 1, k - 1);
	}

	private long choose(int n, int k) {
		if (k < 0)
			throw new ArithmeticException("Cannot choose: " + n + ", " + k);
		else if (n < k)
			return 0L;
		else if (k == 0)
			return 1L;
		else
			return n * choose(n - 1, k - 1) / k;
	}

	@Override
	public Collection<QuartoState> startingPositions() {
		return Collections.singleton(new QuartoState());
	}

	@Override
	public Collection<Pair<String, QuartoState>> validMoves(QuartoState pos) {
		int open = 16 - pos.tier();
		ArrayList<Pair<String, QuartoState>> moves = new ArrayList<Pair<String, QuartoState>>(
				open * open);
		for (int piece = 0; piece < 16; piece++) {
			if (!pos.used(piece)) {
				int place = 0;
				for (int row = 0; row < 4; row++) {
					for (int col = 0; col < 4; col++) {
						if (!pos.filled(place)) {
							QuartoState newState = newState(pos);
							newState.set(place, piece);
							moves.add(new Pair<String, QuartoState>(Character
									.toString((char) ('A' + piece))
									+ Character.toString((char) ('A' + col))
									+ row, newState));
						}
						place++;
					}
				}
			}
		}
		assert moves.size() == open * open;
		return moves;
	}

	@Override
	public int validMoves(QuartoState pos, QuartoState[] children) {
		int child = 0;
		for (int piece = 0; piece < 16; piece++) {
			if (!pos.used(piece)) {
				int place = 0;
				for (int row = 0; row < 4; row++) {
					for (int col = 0; col < 4; col++) {
						if (!pos.filled(place)) {
							children[child].set(pos);
							children[child].set(place, piece);
							child++;
						}
						place++;
					}
				}
			}
		}
		return child;
	}

	@Override
	public int maxChildren() {
		return 256;
	}

	@Override
	public Value primitiveValue(QuartoState pos) {
		int addedLDiag = 15;
		int addedLDiagFlipped = 15;
		int addedRDiag = 15;
		int addedRDiagFlipped = 15;
		for (int i = 0; i < 4; i++) {
			int addedRow = 15;
			int addedRowFlipped = 15;
			int addedCol = 15;
			int addedColFlipped = 15;
			for (int j = 0; j < 4; j++) {
				addedRow &= pos.pieceForPrimitive(i, j);
				addedRowFlipped &= pos.flippedPieceForPrimitive(i, j);
				addedCol &= pos.pieceForPrimitive(j, i);
				addedColFlipped &= pos.flippedPieceForPrimitive(j, i);
			}
			if ((addedRow | addedRowFlipped | addedCol | addedColFlipped) > 0)
				return Value.LOSE;
			addedLDiag &= pos.pieceForPrimitive(i, i);
			addedLDiagFlipped &= pos.flippedPieceForPrimitive(i, i);
			addedRDiag &= pos.pieceForPrimitive(i, 3 - i);
			addedRDiagFlipped &= pos.flippedPieceForPrimitive(i, 3 - i);
		}
		if ((addedLDiag | addedLDiagFlipped | addedRDiag | addedRDiagFlipped) > 0) {
			return Value.LOSE;
		} else if (pos.tier() == 16)
			return Value.TIE;
		else
			return Value.UNDECIDED;
	}

	@Override
	public long stateToHash(QuartoState pos) {
		long majorHash = 0L;
		long minorHash = 0L;
		int numFilled = 0;
		int extra = 16 - pos.tier();
		pos.getUsed(used);
		for (int place = 0; place < 16; place++) {
			if (pos.filled(place)) {
				used[pos.get(place)] = false;
				minorHash += count(place, used)
						* pick(numFilled + extra, numFilled);
				numFilled++;
				majorHash += choose(place, numFilled);
			}
		}
		assert pos.tier() == numFilled;
		return tierStarts[pos.tier()] + majorHash * pick(16, numFilled)
				+ minorHash;
	}

	private static int count(int place, boolean[] used) {
		int count = 0;
		for (int i = 0; i < place; i++) {
			if (!used[i])
				count++;
		}
		return count;
	}

	@Override
	public String stateToString(QuartoState pos) {
		return pos.toString();
	}

	@Override
	public String displayState(QuartoState pos) {
		StringBuilder sb = new StringBuilder(80);
		for (int row = 3; row >= 0; row--) {
			for (int col = 0; col < 4; col++) {
				if (pos.filled(row, col)) {
					sb.append(pos.toBinaryString(row, col));
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
	public QuartoState stringToState(String pos) {
		return new QuartoState(pos);
	}

	@Override
	public String describe() {
		return "Quarto";
	}

	@Override
	public long numHashes() {
		return tierStarts[17];
	}

	@Override
	public long recordStates() {
		return 19;
	}

	@Override
	public void hashToState(long hash, QuartoState s) {
		s.reset();
		int tier = Arrays.binarySearch(tierStarts, hash);
		if (tier < 0) {
			tier = -tier - 2;
		}
		hash -= tierStarts[tier];
		long minorArrangements = pick(16, tier);
		long majorHash = hash / minorArrangements;
		long minorHash = hash % minorArrangements;
		int numFilled = tier;
		int extra = 16 - tier;
		Arrays.fill(used, false);
		for (int place = 15; place >= 0; place--) {
			long pieceHash = choose(place, numFilled);
			if (majorHash >= pieceHash) {
				majorHash -= pieceHash;
				numFilled--;
				int count = (int) (minorHash / pick(numFilled + extra,
						numFilled));
				minorHash %= pick(numFilled + extra, numFilled);
				int piece = count;
				for (int i = 0; i <= piece; i++) {
					if (used[i])
						piece++;
				}
				s.set(place, piece);
				used[piece] = true;
			}
		}
	}

	@Override
	public QuartoState newState() {
		return new QuartoState();
	}

	@Override
	public void longToRecord(QuartoState recordState, long record,
			Record toStore) {
		int intRecord = (int) record;
		switch (intRecord) {
		case 17:
			toStore.value = Value.TIE;
			toStore.remoteness = 16 - recordState.tier();
			break;
		case 18:
			toStore.value = Value.UNDECIDED;
			break;
		default:
			toStore.value = intRecord % 2 == 0 ? Value.LOSE : Value.WIN;
			toStore.remoteness = intRecord;
			break;
		}
	}

	@Override
	public long recordToLong(QuartoState recordState, Record fromRecord) {
		switch (fromRecord.value) {
		case UNDECIDED:
			return 18L;
		case TIE:
			return 17L;
		default:
			return fromRecord.remoteness;
		}
	}
}
