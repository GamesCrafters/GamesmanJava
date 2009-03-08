package edu.berkeley.gamesman.game;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Game;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.util.Pair;

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
	public PrimitiveValue primitiveValue(PyraminxState pos) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<PyraminxState> startingPositions() {
		return Arrays.asList(new PyraminxState(new byte[] { 0, 1, 2, 3, 4, 5 }, new boolean[] { false, false, false, false, false, false}, new byte[] { 0, 0, 0, 0, 0, 0 }));
	}

	@Override
	public BigInteger stateToHash(PyraminxState pos) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String stateToString(PyraminxState pos) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PyraminxState stringToState(String pos) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Pair<String, PyraminxState>> validMoves(PyraminxState pos) {
		ArrayList<Pair<String, PyraminxState>> nextMoves = new ArrayList<Pair<String, PyraminxState>>();
		for(char face : "LURB".toCharArray()) {
			for(int dir : new int[] { 1, 2 }) {
				for(int i = 0; i < dir; i++) {
					
				}
			}
		}
		return nextMoves;
	}
}

class PyraminxState {
	final byte[] edgePermutation, centerOrientation;
	final boolean[] edgeOrientation;
	public PyraminxState(byte[] edgePermutation, boolean[] edgeOrientation, byte[] centerOrientation) {
		this.edgePermutation = edgePermutation;
		this.edgeOrientation = edgeOrientation;
		this.centerOrientation = centerOrientation;
	}
}