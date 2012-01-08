package edu.berkeley.gamesman.hasher.symmetry;

import edu.berkeley.gamesman.hasher.genhasher.GenState;
import edu.berkeley.gamesman.hasher.genhasher.SymmetryWrapper;

public class SymNode<S extends GenState> {
	// private final int index;
	// private final long offset;
	private final long numPositions;
	private final int[] pieces;
	private final SymNode<S>[] children;

	// TODO Add parameter which discriminates based on whether state is a
	// rotation of a lower energy-state.
	@SuppressWarnings("unchecked")
	public SymNode(SymmetryWrapper<S> wrapper, int index, int[] pieces,
			long offset) {
		// this.index = index;
		if (index == 0) {
			this.pieces = new int[pieces.length];
			System.arraycopy(pieces, 0, this.pieces, 0, pieces.length);
			children = null;
			numPositions = wrapper.numPositions(pieces);
		} else {
			this.pieces = null;
			children = new SymNode[wrapper.myHasher.baseFor(index)];
			long numPositions = 0;
			for (int d = 0; d < wrapper.myHasher.baseFor(index); d++) {
				pieces[index - 1] = d;
				children[d] = new SymNode<S>(wrapper, index - 1, pieces, offset
						+ numPositions);
				numPositions += children[d].numPositions;
			}
			this.numPositions = numPositions;
		}
		// this.offset = offset;
	}
}
