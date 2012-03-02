package edu.berkeley.gamesman.hasher.genhasher;

public final class Moves {
	private Moves() {
	}

	public static int matches(Move m, GenState s) {
		for (int i = m.numChanges() - 1; i >= s.getStart(); i--) {
			int place = m.getChangePlace(i);
			if (m.getChangeFrom(i) != s.get(place))
				return place;
		}
		return -1;
	}

	public static int moveHashCode(Move m) {
		int hashCode = 1;
		for (int i = 0; i < m.numChanges(); i++) {
			hashCode *= 31;
			hashCode += m.getChangePlace(i);
			hashCode *= 31;
			hashCode += m.getChangeFrom(i);
			hashCode *= 31;
			hashCode += m.getChangeTo(i);
		}
		return hashCode;
	}

	public static boolean equals(Move first, Move second) {
		if (first.numChanges() != second.numChanges())
			return false;
		for (int i = 0; i < first.numChanges(); i++) {
			if (first.getChangePlace(i) != second.getChangePlace(i))
				return false;
			if (first.getChangeFrom(i) != second.getChangeFrom(i))
				return false;
			if (first.getChangeTo(i) != second.getChangeTo(i))
				return false;
		}
		return true;
	}
}
