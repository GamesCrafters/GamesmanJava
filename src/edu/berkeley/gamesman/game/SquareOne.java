package edu.berkeley.gamesman.game;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Game;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.hasher.PermutationHash;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

public class SquareOne extends Game<SquareOneState> {
	private final boolean TWO_LAYER;

	private final boolean IGNORE_COLORS;

	private final ArrayList<String> OPTIONS;

	private PermutationHash ph = null;

	private BitRearranger br = null;

	private static final int EDGE_COUNT = 8, CORNER_COUNT = 8,
			PIECE_COUNT = EDGE_COUNT + CORNER_COUNT;

	public SquareOne(Configuration conf) {
		super(conf);
		TWO_LAYER = conf.getBoolean("gamesman.game.twoLayer", false);
		IGNORE_COLORS = conf.getBoolean("gamesman.game.ignoreColors", false);
		OPTIONS = new ArrayList<String>();
		if (TWO_LAYER)
			OPTIONS.add("no middle layer");
		if (IGNORE_COLORS) {
			OPTIONS.add("no stickers");
			br = new BitRearranger(EDGE_COUNT, CORNER_COUNT);
		} else {
			ph = new PermutationHash(PIECE_COUNT, false);
		}

		Integer[] topLayer = new Integer[] { 0, 1, null, 2, 3, null, 4, 5,
				null, 6, 7, null };
		// the second layer is numbered like this to make corners odd and edges
		// even
		Integer[] bottomLayer = new Integer[] { 9, null, 10, 11, null, 12, 13,
				null, 14, 15, null, 8 };
		if (IGNORE_COLORS) {
			for (int i = 0; i < EDGE_COUNT; i++) {
				if (topLayer[i] != null)
					topLayer[i] %= 2;
				if (bottomLayer[i] != null)
					bottomLayer[i] %= 2;
			}
		}
		SOLVED = new SquareOneState(topLayer, bottomLayer, true);
	}

	@Override
	public String describe() {
		String name = "Square One";
		if (!OPTIONS.isEmpty())
			name += " (" + Util.join(", ", OPTIONS) + ")";
		return name;
	}

	@Override
	public String displayState(SquareOneState pos) {
		return stateToString(pos);
	}

	@Override
	public SquareOneState hashToState(long hash) {
		boolean middleEven = true;
		if (!TWO_LAYER) {
			middleEven = hash % 2 == 0;
			hash /= 2;
		}

		ArrayList<Integer> permutation = IGNORE_COLORS ? br.unhash(BigInteger
				.valueOf(hash)) : ph.unhash(BigInteger.valueOf(hash));
		ArrayList<Integer> topLayer = new ArrayList<Integer>();
		ArrayList<Integer> bottomLayer = new ArrayList<Integer>();
		int layerSize = 0;
		for (Integer piece : permutation) {
			ArrayList<Integer> currentLayer = layerSize < 12 ? topLayer
					: bottomLayer;
			currentLayer.add(piece);
			layerSize++;
			if (piece % 2 != 0) { // we have a corner
				layerSize++;
				currentLayer.add(null);
			}
		}
		return new SquareOneState(Util.toArray(topLayer), Util
				.toArray(bottomLayer), middleEven);
	}

	@Override
	public long numHashes() {
		long lastHash = IGNORE_COLORS ? br.maxHash().longValue() : (ph
				.numHashes().longValue() - 1L);
		if (TWO_LAYER)
			lastHash <<= 1;
		return lastHash + 1;
	}

	private static int stripNull(Integer[] src, Integer[] dest, int destIndex) {
		for (int i = 0; i < src.length; i++)
			if (src[i] != null)
				dest[destIndex++] = src[i];
		return destIndex;
	}

	@Override
	public long stateToHash(SquareOneState pos) {
		Integer[] permutation = new Integer[PIECE_COUNT];
		int dest = stripNull(pos.topLayer, permutation, 0);
		dest = stripNull(pos.bottomLayer, permutation, dest);
		long hash = IGNORE_COLORS ? br.hash(permutation).longValue() : ph.hash(
				permutation).longValue();
		if (!TWO_LAYER) {
			hash <<= 1;
			if (!pos.middleEven)
				hash |= 1;
		}
		return hash;
	}

	@Override
	public PrimitiveValue primitiveValue(SquareOneState pos) {
		boolean solved = Arrays.equals(pos.topLayer, SOLVED.topLayer)
				&& Arrays.equals(pos.bottomLayer, SOLVED.bottomLayer)
				&& (TWO_LAYER || pos.middleEven == SOLVED.middleEven);
		return solved ? PrimitiveValue.WIN : PrimitiveValue.UNDECIDED;
	}

	private final SquareOneState SOLVED;

	@Override
	public Collection<SquareOneState> startingPositions() {
		return Arrays.asList(SOLVED);
	}

	@Override
	public String stateToString(SquareOneState pos) {
		String state = Util.join(",", pos.topLayer) + ";"
				+ Util.join(",", pos.bottomLayer);
		if (!TWO_LAYER)
			state += ";" + (pos.middleEven ? "0" : "1");
		return state;
	}

	@Override
	public SquareOneState stringToState(String pos) {
		String[] top_bottom_middle = pos.split(";");
		Util.assertTrue((top_bottom_middle.length == 2) == TWO_LAYER,
				"String: " + pos
						+ " does not encode a SquareOne with TWO_LAYER=="
						+ TWO_LAYER);
		boolean middleEven = TWO_LAYER ? true : !top_bottom_middle[2]
				.equals("1");
		return new SquareOneState(Util.parseIntegers(top_bottom_middle[0]),
				Util.parseIntegers(top_bottom_middle[1]), middleEven);
	}

	// this shifts every element in src forward by amt
	private static Integer[] rotate(Integer[] src, int amt) {
		Integer[] dest = new Integer[src.length];
		for (int i = 0; i < dest.length; i++)
			dest[i] = Util.moduloAccess(src, i - amt);
		return dest;
	}

	private static void doSlash(Integer[] topLayer, Integer[] bottomLayer) {
		for (int i = 0; i < 6; i++) {
			Integer topPiece = topLayer[i];
			topLayer[i] = bottomLayer[i];
			bottomLayer[i] = topPiece;
		}
	}

	@Override
	public Collection<Pair<String, SquareOneState>> validMoves(
			SquareOneState pos) {
		// TODO will this break anything if any two moves lead to the same hash?
		// what if they lead to symmetric positions?
		ArrayList<Pair<String, SquareOneState>> nextMoves = new ArrayList<Pair<String, SquareOneState>>();
		for (int top = 0; top < 12; top++) {
			for (int bottom = 0; bottom < 12; bottom++) {
				if (pos.isLegalTurn(top, bottom)) {
					Integer[] newTop = rotate(pos.topLayer, top);
					Integer[] newBottom = rotate(pos.bottomLayer, bottom);
					boolean middleEven = pos.middleEven;
					String move = "(" + top + ", " + bottom + ")";
					nextMoves.add(new Pair<String, SquareOneState>(move,
							new SquareOneState(newTop, newBottom, middleEven)));
				}
			}
		}
		Integer[] newTop = pos.topLayer.clone();
		Integer[] newBottom = pos.bottomLayer.clone();
		doSlash(newTop, newBottom);
		nextMoves.add(new Pair<String, SquareOneState>("/", new SquareOneState(
				newTop, newBottom, !pos.middleEven)));
		return nextMoves;
	}

	public static void main(String[] args) {
		// SquareOne sq1 = new SquareOne(new
		// Configuration("jobs/SquareOne.job"));
		// for(BigInteger h : Util.bigIntIterator(sq1.lastHash())) {
		// // for(BigInteger h : Util.bigIntIterator(BigInteger.valueOf(2))) {
		// SquareOneState s = sq1.hashToState(h);
		// // System.out.println(h + " " + sq1.displayState(s) + " " +
		// sq1.stateToHash(s));
		// if(!h.equals(sq1.stateToHash(s)))
		// System.out.println(h + " " + sq1.displayState(s) + " " +
		// sq1.stateToHash(s));
		// }

		BitRearranger bits = new BitRearranger(8, 8);
		System.out.println(bits
				.hash(Util.toArray(bits.unhash(BigInteger.ZERO))));
	}
}

class BitRearranger {
	private final int[] unhash, hash;

	private final int oCount, iCount;

	public BitRearranger(int oCount, int iCount) {
		Util.assertTrue(oCount + iCount < 32, "Must fit into a 32 bit int!");
		this.oCount = oCount;
		this.iCount = iCount;
		unhash = new int[(int) (fact(oCount + iCount) / (fact(oCount) * fact(iCount)))];
		int maxPerm = 0;
		for (int i = 0; i < iCount; i++)
			maxPerm = (maxPerm << 1) | 1;
		for (int i = 0; i < oCount; i++)
			maxPerm = (maxPerm << 1);
		hash = new int[maxPerm + 1];
		permute(0, 0, oCount, iCount);
	}

	public BigInteger maxHash() {
		return BigInteger.valueOf(maxHash - 1);
	}

	public BigInteger hash(Integer[] permutation) {
		int perm = 0;
		for (int i = 0; i < permutation.length; i++)
			perm = (perm << 1) | (permutation[i] % 2);
		return BigInteger.valueOf(hash[perm]);
	}

	private long fact(long i) {
		return i <= 1 ? 1 : i * fact(i - 1);
	}

	private int maxHash = 0;

	private void permute(int i, int perm, int os, int is) {
		if (i == oCount + iCount) {
			unhash[maxHash] = perm;
			hash[perm] = maxHash;
			maxHash++;
			return;
		}
		if (os > 0)
			permute(i + 1, perm << 1, os - 1, is);
		if (is > 0)
			permute(i + 1, (perm << 1) | 1, os, is - 1);
	}

	public ArrayList<Integer> unhash(BigInteger hash) {
		int perm = unhash[hash.intValue()];
		ArrayList<Integer> permutation = new ArrayList<Integer>(oCount + iCount);
		for (int i = oCount + iCount - 1; i >= 0; i--) {
			int bit = perm >> i;
			perm -= bit << i;
			permutation.add(bit);
		}
		return permutation;
	}
}

class SquareOneState {
	final Integer[] topLayer, bottomLayer;

	final boolean middleEven;

	SquareOneState(Integer[] topLayer, Integer[] bottomLayer, boolean middleEven) {
		this.topLayer = topLayer;
		this.bottomLayer = bottomLayer;
		this.middleEven = middleEven;
	}

	boolean isLegalTurn(int top, int bottom) {
		return Util.moduloAccess(topLayer, 6 + top) != null
				&& Util.moduloAccess(bottomLayer, 6 + bottom) != null;
	}
}