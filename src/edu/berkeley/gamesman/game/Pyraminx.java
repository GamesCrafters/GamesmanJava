package edu.berkeley.gamesman.game;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Game;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.hasher.PermutationHash;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * @author Jeremy Fleischman
 * 
 */
public class Pyraminx extends Game<PyraminxState> {

	/**
	 * @param conf
	 *            The configuration object
	 */
	public Pyraminx(Configuration conf) {
		super(conf);
	}

	@Override
	public String describe() {
		return "Pyraminx";
	}

	@Override
	public int getPlayerCount() {
		return 1;
	}

	@Override
	public String displayState(PyraminxState pos) {
		return stateToString(pos);
	}

	@Override
	public PrimitiveValue primitiveValue(PyraminxState pos) {
		return pos.equals(SOLVED_STATE) ? PrimitiveValue.WIN
				: PrimitiveValue.UNDECIDED;
	}

	private static final PyraminxState SOLVED_STATE = new PyraminxState(
			new Integer[] { 0, 1, 2, 3, 4, 5 }, new Integer[] { 0, 0, 0, 0, 0,
					0 }, new Integer[] { 0, 0, 0, 0 });

	@Override
	public Collection<PyraminxState> startingPositions() {
		return Arrays.asList(SOLVED_STATE);
	}

	private static final int edgeCount = 6, centerCount = 4;

	// memoize some useful values for (un)hashing
	private static final BigInteger[] THREE_TO_X = powers(3, centerCount + 1),
			TWO_TO_X = powers(2, edgeCount + 1);

	// TODO - even permutations only!
	private static final PermutationHash epHasher = new PermutationHash(
			edgeCount, true);

	private static BigInteger[] powers(int base, int maxExp) {
		BigInteger[] powers = new BigInteger[maxExp];
		powers[0] = BigInteger.ONE;
		for (int i = 1; i < maxExp; i++)
			powers[i] = BigInteger.valueOf(base).multiply(powers[i - 1]);
		return powers;
	}

	@Override
	public long stateToHash(PyraminxState state) {
		BigInteger hash = BigInteger.ZERO;

		hash = hash.add(epHasher.hash(state.edgePermutation));

		// edge orientation
		hash = hash.multiply(TWO_TO_X[state.edgeOrientation.length - 1]);
		// don't need to hash the orientation of the last edge, as it is
		// determined by all the others
		for (int i = 0; i < state.edgeOrientation.length - 1; i++)
			hash = hash.add(BigInteger.valueOf(state.edgeOrientation[i])
					.multiply(TWO_TO_X[i]));

		// center orientation
		hash = hash.multiply(THREE_TO_X[state.centerOrientation.length]);
		for (int i = 0; i < state.centerOrientation.length; i++)
			hash = hash.add(BigInteger.valueOf(state.centerOrientation[i])
					.multiply(THREE_TO_X[i]));

		return hash.longValue();
	}

	@Override
	public long numHashes() {
		return epHasher.numHashes().multiply(
				TWO_TO_X[edgeCount - 1].multiply(THREE_TO_X[centerCount]))
				.longValue();
	}

	@Override
	public PyraminxState hashToState(long hash) {
		Integer[] centerOrientation = new Integer[centerCount];
		for (int i = 0; i < centerCount; i++) {
			centerOrientation[i] = (int) (hash % 3);
			hash /= 3;
		}

		Integer[] edgeOrientation = new Integer[edgeCount];
		int totalorient = 0;
		for (int i = 0; i < edgeCount - 1; i++) {
			edgeOrientation[i] = (int) (hash % 2);
			hash /= 2;
			totalorient += edgeOrientation[i];
		}
		// the number of flipped edges must be even!
		edgeOrientation[edgeCount - 1] = Util
				.positiveModulo(2 - totalorient, 2);

		Integer[] edgePermutation = Util.toArray(epHasher.unhash(BigInteger
				.valueOf(hash)));
		return new PyraminxState(edgePermutation, edgeOrientation,
				centerOrientation);
	}

	@Override
	public String stateToString(PyraminxState pos) {
		return Util.join(",", pos.edgePermutation) + ";"
				+ Util.join(",", pos.edgeOrientation) + ";"
				+ Util.join(",", pos.centerOrientation);
	}

	@Override
	public PyraminxState stringToState(String pos) {
		String[] eperm_orient_cperm = pos.split(";");
		return new PyraminxState(Util.parseIntegers(eperm_orient_cperm[0]
				.split(",")), Util.parseIntegers(eperm_orient_cperm[1]
				.split(",")), Util.parseIntegers(eperm_orient_cperm[2]
				.split(",")));
	}

	private static final int UP = 0, RIGHT = 1, LEFT = 2, BACK = 3;

	private static HashMap<Integer, Pair<Character, Integer[]>> EDGE_INDICES = new HashMap<Integer, Pair<Character, Integer[]>>();
	static {
		EDGE_INDICES.put(UP, new Pair<Character, Integer[]>('U', new Integer[] {
				0, 1, 2 }));
		EDGE_INDICES.put(RIGHT, new Pair<Character, Integer[]>('R',
				new Integer[] { 3, 0, 5 }));
		EDGE_INDICES.put(LEFT, new Pair<Character, Integer[]>('L',
				new Integer[] { 3, 4, 1 }));
		EDGE_INDICES.put(BACK, new Pair<Character, Integer[]>('B',
				new Integer[] { 2, 4, 5 }));
	}

	private <H> void cycle(H[] arr, Integer[] indices, int offset) {
		H[] clone = arr.clone();
		for (int i = 0; i < indices.length; i++)
			arr[Util.moduloAccess(indices, i + offset)] = clone[indices[i]];
	}

	@Override
	public Collection<Pair<String, PyraminxState>> validMoves(PyraminxState pos) {
		ArrayList<Pair<String, PyraminxState>> nextMoves = new ArrayList<Pair<String, PyraminxState>>();
		for (int axis : EDGE_INDICES.keySet()) {
			for (int dir : new int[] { 1, 2 }) {
				PyraminxState next = pos.clone();
				Integer[] edgeIndices = EDGE_INDICES.get(axis).cdr;
				for (int i = 0; i < dir; i++) {
					cycle(next.edgePermutation, edgeIndices, 1);
					next.centerOrientation[axis] = Util.positiveModulo(
							next.centerOrientation[axis] + 1, 3);
					cycle(next.edgeOrientation, edgeIndices, 1);
					// NOTE: we're updating the orientations *after* their
					// permutations have been updated
					if (axis == LEFT || axis == RIGHT) {
						// these twists don't affect edge orientation
					} else {
						int edge1 = -1, edge2 = -1;
						if (axis == BACK) {
							edge1 = 5;
							edge2 = 2;
						} else if (axis == UP) {
							edge1 = 1;
							edge2 = 2;
						}
						next.edgeOrientation[edge1] = 1 - next.edgeOrientation[edge1];
						next.edgeOrientation[edge2] = 1 - next.edgeOrientation[edge2];
					}
				}
				String move = "" + EDGE_INDICES.get(axis).car;
				if (Util.positiveModulo(dir, 3) == 2)
					move += "'";
				nextMoves.add(new Pair<String, PyraminxState>(move, next));
			}
		}
		return nextMoves;
	}

	private class PRecord extends Record {
		protected PRecord() {
			super(conf);
		}

		protected PRecord(long state) {
			super(conf);
			set(state);
		}

		protected PRecord(int remoteness) {
			super(conf);
			this.remoteness = remoteness;
		}

		public PRecord(PrimitiveValue pv) {
			super(conf);
			value = pv;
		}

		@Override
		public long getState() {
			if (value.equals(PrimitiveValue.UNDECIDED))
				return 0;
			else
				return remoteness + 1;
		}

		@Override
		public void set(long state) {
			if (state == 0)
				value = PrimitiveValue.UNDECIDED;
			else {
				value = PrimitiveValue.WIN;
				remoteness = (int) (state - 1);
			}
		}
	}

	@Override
	public Record newRecord() {
		return new PRecord();
	}

	@Override
	public Record newRecord(long val) {
		return new PRecord(val);
	}

	@Override
	public Record newRecord(PrimitiveValue pv) {
		return new PRecord(pv);
	}

	@Override
	public long recordStates() {
		return 21;
	}
}

class PyraminxState {
	final Integer[] edgePermutation, centerOrientation, edgeOrientation;

	public PyraminxState(Integer[] edgePermutation, Integer[] edgeOrientation,
			Integer[] centerOrientation) {
		this.edgePermutation = edgePermutation;
		this.edgeOrientation = edgeOrientation;
		this.centerOrientation = centerOrientation;
	}

	public PyraminxState clone() {
		return new PyraminxState(edgePermutation.clone(), edgeOrientation
				.clone(), centerOrientation.clone());
	}

	public boolean equals(PyraminxState other) {
		return Arrays.equals(centerOrientation, other.centerOrientation)
				&& Arrays.equals(edgeOrientation, other.edgeOrientation)
				&& Arrays.equals(edgePermutation, other.edgePermutation);
	}
}