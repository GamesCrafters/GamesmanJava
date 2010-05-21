package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.hasher.PermutationHash;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * @author Jeremy Fleischman
 * 
 */
public class Pyraminx extends TwistyPuzzle<PyraminxState> {

	public Pyraminx(Configuration conf) {
		super(conf);
	}

	@Override
	public String describe() {
		return "Pyraminx";
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
			new int[] { 0, 1, 2, 3, 4, 5 }, new int[] { 0, 0, 0, 0, 0, 0 },
			new int[] { 0, 0, 0, 0 });

	private int[] clone;

	@Override
	public Collection<PyraminxState> startingPositions() {
		return Arrays.asList(SOLVED_STATE);
	}

	private static final int edgeCount = 6, centerCount = 4;

	// memoize some useful values for (un)hashing
	private static final long[] THREE_TO_X = powers(3, centerCount + 1),
			TWO_TO_X = powers(2, edgeCount + 1);

	private final PermutationHash epHasher = new PermutationHash(edgeCount,
			true);

	private static long[] powers(int base, int maxExp) {
		long[] powers = new long[maxExp];
		powers[0] = 1;
		for (int i = 1; i < maxExp; i++)
			powers[i] = base * powers[i - 1];
		return powers;
	}

	@Override
	public long stateToHash(PyraminxState state) {
		long hash = 0;

		hash += epHasher.hash(state.edgePermutation);

		// edge orientation
		hash <<= state.edgeOrientation.length - 1;
		// don't need to hash the orientation of the last edge, as it is
		// determined by all the others
		for (int i = 0; i < state.edgeOrientation.length - 1; i++)
			hash += state.edgeOrientation[i] << i;

		// center orientation
		hash *= THREE_TO_X[state.centerOrientation.length];
		for (int i = 0; i < state.centerOrientation.length; i++)
			hash += state.centerOrientation[i] * THREE_TO_X[i];
		return hash;
	}

	@Override
	public long numHashes() {
		return epHasher.numHashes() * TWO_TO_X[edgeCount - 1]
				* THREE_TO_X[centerCount];
	}

	@Override
	public void hashToState(long hash, PyraminxState state) {
		for (int i = 0; i < centerCount; i++) {
			state.centerOrientation[i] = (int) (hash % 3);
			hash /= 3;
		}
		int totalorient = 0;
		for (int i = 0; i < edgeCount - 1; i++) {
			state.edgeOrientation[i] = (int) (hash & 1);
			hash >>= 1;
			totalorient += state.edgeOrientation[i];
		}
		// the number of flipped edges must be even!
		state.edgeOrientation[edgeCount - 1] = (2 - totalorient) & 1;
		epHasher.unhash(hash, state.edgePermutation);
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
		return new PyraminxState(Util.parseInts(eperm_orient_cperm[0]
				.split(",")), Util.parseInts(eperm_orient_cperm[1].split(",")),
				Util.parseInts(eperm_orient_cperm[2].split(",")));
	}

	private static final int UP = 0, RIGHT = 1, LEFT = 2, BACK = 3;

	private static HashMap<Integer, Pair<Character, int[]>> EDGE_INDICES = new HashMap<Integer, Pair<Character, int[]>>();
	static {
		EDGE_INDICES.put(UP, new Pair<Character, int[]>('U', new int[] { 0, 1,
				2 }));
		EDGE_INDICES.put(RIGHT, new Pair<Character, int[]>('R', new int[] { 3,
				0, 5 }));
		EDGE_INDICES.put(LEFT, new Pair<Character, int[]>('L', new int[] { 3,
				4, 1 }));
		EDGE_INDICES.put(BACK, new Pair<Character, int[]>('B', new int[] { 2,
				4, 5 }));
	}

	private void cycle(int[] arr, int[] indices, int offset) {
		if (clone == null || clone.length < arr.length)
			clone = arr.clone();
		else
			for (int i = 0; i < arr.length; i++)
				clone[i] = arr[i];
		for (int i = 0; i < indices.length; i++)
			arr[indices[Util.nonNegativeModulo(i + offset, indices.length)]] = clone[indices[i]];
	}

	@Override
	public int validMoves(PyraminxState pos, PyraminxState[] moves) {
		int countMoves = 0;
		for (int axis : EDGE_INDICES.keySet()) {
			// TODO: Bad! Instantiates a very expensive iterator. (You'll be
			// shocked if you run under debugger and see just how much effort
			// that one line requires)
			for (int dir = 1; dir <= 2; dir++) {
				if (moves[countMoves] == null)
					moves[countMoves] = new PyraminxState(edgeCount,
							centerCount);
				PyraminxState next = moves[countMoves++];
				next.set(pos);
				int[] edgeIndices = EDGE_INDICES.get(axis).cdr;
				for (int i = 0; i < dir; i++) {
					cycle(next.edgePermutation, edgeIndices, 1);
					next.centerOrientation[axis] = Util.nonNegativeModulo(
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
			}
		}
		return countMoves;
	}

	@Override
	public Collection<Pair<String, PyraminxState>> validMoves(PyraminxState pos) {
		ArrayList<Pair<String, PyraminxState>> nextMoves = new ArrayList<Pair<String, PyraminxState>>();
		for (int axis : EDGE_INDICES.keySet()) {
			for (int dir : new int[] { 1, 2 }) {
				PyraminxState next = pos.clone();
				int[] edgeIndices = EDGE_INDICES.get(axis).cdr;
				for (int i = 0; i < dir; i++) {
					cycle(next.edgePermutation, edgeIndices, 1);
					next.centerOrientation[axis] = Util.nonNegativeModulo(
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
				if (Util.nonNegativeModulo(dir, 3) == 2)
					move += "'";
				nextMoves.add(new Pair<String, PyraminxState>(move, next));
			}
		}
		return nextMoves;
	}

	@Override
	public int maxChildren() {
		return 8;
		// TODO: How can this be generalized?
	}

	@Override
	public PyraminxState newState() {
		return new PyraminxState(edgeCount, centerCount);
	}
}

class PyraminxState implements State {
	final int[] edgePermutation, centerOrientation, edgeOrientation;

	public PyraminxState(int[] edgePermutation, int[] edgeOrientation,
			int[] centerOrientation) {
		this.edgePermutation = edgePermutation;
		this.edgeOrientation = edgeOrientation;
		this.centerOrientation = centerOrientation;
	}

	public PyraminxState(int numEdges, int numCenters) {
		edgePermutation = new int[numEdges];
		centerOrientation = new int[numCenters];
		edgeOrientation = new int[numEdges];
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

	public void set(State s) {
		if (s instanceof PyraminxState) {
			PyraminxState p = (PyraminxState) s;
			for (int i = 0; i < edgePermutation.length; i++)
				edgePermutation[i] = p.edgePermutation[i];
			for (int i = 0; i < edgeOrientation.length; i++)
				edgeOrientation[i] = p.edgeOrientation[i];
			for (int i = 0; i < centerOrientation.length; i++)
				centerOrientation[i] = p.centerOrientation[i];
		} else
			throw new RuntimeException("Type mismatch");
	}
}