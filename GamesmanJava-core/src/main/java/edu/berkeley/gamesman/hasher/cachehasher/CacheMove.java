package edu.berkeley.gamesman.hasher.cachehasher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import edu.berkeley.gamesman.hasher.genhasher.GenState;

/**
 * @author dnspies
 * 
 */
public final class CacheMove {
	private final int[][] changes;
	public final int minPlace;
	public final int numChanges;

	/**
	 * @param changes
	 */
	public CacheMove(int... changes) {
		assert changes.length % 3 == 0;
		numChanges = changes.length / 3;
		this.changes = new int[numChanges][3];
		int minPlace = Integer.MAX_VALUE;
		for (int i = 0; i < numChanges; i++) {
			for (int k = 0; k < 3; k++) {
				this.changes[i][k] = changes[3 * i + k];
			}
			if (this.changes[i][0] < minPlace)
				minPlace = this.changes[i][0];
		}
		this.minPlace = minPlace;
		Arrays.sort(this.changes, new Comparator<int[]>() {
			@Override
			public int compare(int[] o1, int[] o2) {
				if (o1[0] < o2[0])
					return -1;
				else if (o1[0] > o2[0])
					return 1;
				else
					return 0;
			}
		});
		assert minPlace == this.changes[0][0];
	}

	/**
	 * @param myState
	 * @return
	 */
	public int nextStep(GenState myState) {
		for (int i = 0; i < changes.length; i++) {
			if (myState.get(changes[i][0]) != changes[i][1])
				return changes[i][0];
		}
		return -1;
	}

	/**
	 * @param step
	 * @return
	 */
	public int needed(int step) {
		for (int[] set : changes) {
			if (set[0] == step)
				return set[1];
		}
		throw new ArrayIndexOutOfBoundsException();
	}

	public int getChangePlace(int i) {
		return changes[i][0];
	}

	public int getChangeFrom(int i) {
		return changes[i][1];
	}

	public int getChangeTo(int i) {
		return changes[i][2];
	}

	@Override
	public String toString() {
		ArrayList<String> arrString = new ArrayList<String>(changes.length);
		for (int[] x : changes) {
			arrString.add(Arrays.toString(x));
		}
		return arrString.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof CacheMove) {
			CacheMove otherMove = (CacheMove) obj;
			return Arrays.deepEquals(changes, otherMove.changes);
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Arrays.deepHashCode(changes);
	}
}
