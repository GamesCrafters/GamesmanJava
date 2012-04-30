package edu.berkeley.gamesman.hasher.genhasher;

import java.util.Arrays;

/**
 * Move consists of a set of triplets (place, from, to) which specify what
 * changes when this move is made, what it changes from, and what it changes to.
 * 
 * @author dnspies
 * 
 */
public class Move {
	private static class Triplet implements Comparable<Triplet> {
		int place, from, to;

		@Override
		public String toString() {
			return Arrays.toString(new int[] { place, from, to });
		}

		@Override
		public boolean equals(Object other) {
			Triplet tw = (Triplet) other;
			return place == tw.place && from == tw.from && to == tw.to;
		}

		@Override
		public int compareTo(Triplet o) {
			return place - o.place;
		}
	}

	private final Triplet[] changeList;
	private final int hashCode;

	public Move(int... changes) {
		changeList = new Triplet[changes.length / 3];
		int lastPlace = -1;
		for (int i = 0; i < changeList.length; i++) {
			Triplet writ = new Triplet();
			writ.place = changes[3 * i];
			lastPlace = writ.place;
			writ.from = changes[3 * i + 1];
			writ.to = changes[3 * i + 2];
			changeList[i] = writ;
		}
		Arrays.sort(changeList);
		hashCode = Arrays.hashCode(changes);
	}

	/**
	 * @return The number of changes made to the sequence by this move
	 */
	public int numChanges() {
		return changeList.length;
	}

	/**
	 * @param i
	 *            Which change
	 * @return The place in the sequence at which the ith change occurs
	 */
	public int getChangePlace(int i) {
		return changeList[i].place;
	}

	/**
	 * @param i
	 *            Which change
	 * @return The initial value of the piece at the place where the ith change
	 *         occurs
	 */
	public int getChangeFrom(int i) {
		return changeList[i].from;
	}

	/**
	 * @param i
	 *            Which change
	 * @return The final value of the piece at the place where the ith change
	 *         occurs after the move is made.
	 */
	public int getChangeTo(int i) {
		return changeList[i].to;
	}

	@Override
	public String toString() {
		return changeList.toString();
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Move
				&& Arrays.equals(changeList, ((Move) other).changeList);
	}

	public int matches(GenState s) {
		for (int i = changeList.length - 1; i >= s.getStart(); i--) {
			int place = changeList[i].place;
			if (changeList[i].from != s.get(place))
				return place;
		}
		return -1;
	}

}
