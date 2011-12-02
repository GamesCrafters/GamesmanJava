package edu.berkeley.gamesman.hasher.genhasher;

/**
 * A wrapper for the symmetry interface to use
 * 
 * @author dnspies
 * 
 * @param <S>
 *            The state type associated with this hasher
 */
public class SymmetryWrapper<S extends GenState> {
	private final GenHasher<S> myHasher;

	/**
	 * @param hasher
	 *            The hasher to wrap
	 */
	public SymmetryWrapper(GenHasher<S> hasher) {
		myHasher = hasher;
	}

	/**
	 * This will return the number of positions which match the passed suffix
	 * 
	 * @param suffix
	 *            The suffix to count
	 * @return The number of positions for this suffix
	 */
	public long numPositions(int[] suffix) {
		S pref = myHasher.getPoolPref();
		pref.clear();
		for (int i = suffix.length - 1; i >= 0; i--) {
			if (!myHasher.validPref(pref))
				break;
			myHasher.addLS(pref, suffix[i]);
		}
		long numCompletions = myHasher.countCompletions(pref);
		myHasher.releasePref(pref);
		return numCompletions;
	}

	/**
	 * Returns the hash ignoring the suffix (ie the hash of the pieces only up
	 * to suffixStart exclusive)
	 * 
	 * @param state
	 *            The state to hash
	 * @param suffixStart
	 *            The place where the suffix starts
	 * @return The hash
	 */
	public long subHash(S state, int suffixStart) {
		return myHasher.hash(state, suffixStart);
	}
}
