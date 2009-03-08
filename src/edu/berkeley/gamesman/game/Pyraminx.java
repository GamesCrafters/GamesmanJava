package edu.berkeley.gamesman.game;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Game;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * @author Jeremy Fleischman
 *
 */
public class Pyraminx extends Game<PyraminxState> {

	/**
	 * @param conf
	 */
	public Pyraminx(Configuration conf) {
		super(conf);
	}

	@Override
	public String describe() {
		return "Pyraminx";
	}

	@Override
	public String displayState(PyraminxState pos) {
		// TODO Auto-generated method stub
		return stateToString(pos);
	}
	
	@Override
	public PrimitiveValue primitiveValue(PyraminxState pos) {
		if(pos.equals(SOLVED_STATE))
			return PrimitiveValue.WIN;
		return PrimitiveValue.UNDECIDED;
	}

	private static final PyraminxState SOLVED_STATE = new PyraminxState(new Integer[] { 0, 1, 2, 3, 4, 5 },
			new Boolean[] { false, false, false, false, false, false},
			new Integer[] { 0, 0, 0, 0, 0, 0 });
	
	@Override
	public Collection<PyraminxState> startingPositions() {
		return Arrays.asList(SOLVED_STATE);
	}

	@Override
	public BigInteger stateToHash(PyraminxState pos) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PyraminxState hashToState(BigInteger hash) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BigInteger lastHash() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String stateToString(PyraminxState pos) {
		return Util.join(",", pos.edgePermutation) + ";" + Util.join(",", pos.edgeOrientation) + ";" + Util.join(",", pos.centerOrientation);
	}

	@Override
	public PyraminxState stringToState(String pos) {
		String[] eperm_orient_cperm = pos.split(";");
		return new PyraminxState(Util.parseInts(eperm_orient_cperm[0].split(",")),
				Util.parseBooleans(eperm_orient_cperm[1].split(",")),
				Util.parseInts(eperm_orient_cperm[2].split(",")));
	}
	
	private static final int UP = 0, RIGHT = 1, LEFT = 2, BACK = 3;
	private static HashMap<Integer, Pair<Character, Integer[]>> EDGE_INDICES = new HashMap<Integer, Pair<Character, Integer[]>>();
	static {
		EDGE_INDICES.put(UP, new Pair<Character, Integer[]>('U', new Integer[] { 0, 1, 2 }));
		EDGE_INDICES.put(RIGHT, new Pair<Character, Integer[]>('R', new Integer[] { 3, 0, 5 }));
		EDGE_INDICES.put(LEFT, new Pair<Character, Integer[]>('L', new Integer[] { 3, 4, 1 }));
		EDGE_INDICES.put(BACK, new Pair<Character, Integer[]>('B', new Integer[] { 2, 4, 5 }));
	}
	
	private <H> void cycle(H[] arr, Integer[] indices, int offset) {
		H[] clone = arr.clone();
		for(int i=0; i<indices.length; i++)
			arr[Util.moduloAccess(indices, i+offset)] = clone[indices[i]]; 
	}
	
	@Override
	public Collection<Pair<String, PyraminxState>> validMoves(PyraminxState pos) {
		ArrayList<Pair<String, PyraminxState>> nextMoves = new ArrayList<Pair<String, PyraminxState>>();
		for(int axis : EDGE_INDICES.keySet()) {
			for(int dir : new int[] { 1, 2 }) {
				PyraminxState next = pos.clone();
				Integer[] edgeIndices = EDGE_INDICES.get(axis).cdr;
				for(int i = 0; i < dir; i++) {
					cycle(next.edgePermutation, edgeIndices, dir);
					next.centerOrientation[axis] = Util.positiveModulo(next.centerOrientation[axis] + dir, 3);
					cycle(next.edgeOrientation, edgeIndices, dir);
					//NOTE: we're updating the orientations *after* their permutations have been updated
					if(axis == LEFT || axis == RIGHT) {
						//these twists don't affect edge orientation
					} else {
						int edge1 = -1, edge2 = -1;
						if(axis == BACK) {
							edge1 = 5;
							edge2 = 2;
						} else if(axis == UP) {
							edge1 = 1;
							edge2 = 2;
						}
						next.edgeOrientation[edge1] = !next.edgeOrientation[edge1];
						next.edgeOrientation[edge2] = !next.edgeOrientation[edge2];
					}
				}
				nextMoves.add(new Pair<String, PyraminxState>("" + EDGE_INDICES.get(axis).car, next));
			}
		}
		System.out.println("***" + nextMoves);
		return nextMoves;
	}
}

class PyraminxState  {
	final Integer[] edgePermutation, centerOrientation;
	final Boolean[] edgeOrientation;
	public PyraminxState(Integer[] edgePermutation, Boolean[] edgeOrientation, Integer[] centerOrientation) {
		this.edgePermutation = edgePermutation;
		this.edgeOrientation = edgeOrientation;
		this.centerOrientation = centerOrientation;
	}

	public PyraminxState clone() {
		return new PyraminxState(edgePermutation.clone(), edgeOrientation.clone(), centerOrientation.clone());
	}
	
	public boolean equals(PyraminxState other) {
		return Arrays.equals(centerOrientation, other.centerOrientation) && 
			Arrays.equals(edgeOrientation, other.edgeOrientation) && 
			Arrays.equals(edgePermutation, other.edgePermutation);
	}
}