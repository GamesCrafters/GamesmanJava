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
}
