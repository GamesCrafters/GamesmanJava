package edu.berkeley.gamesman.game.util;

/**
 * A rearranger for dartboard top-down solves
 * 
 * @author dnspies
 */
public class TopDownSpaceRearranger {
	private class Space {
		boolean isFilled;
		int countPieces;
		long hash;

		public Space() {
			isFilled = false;
		}
	}

	private final int numPlaces;
	private final Space[] arrangement;
	private int numPieces;
	private long hash;

	/**
	 * @param n
	 *            The number of spaces on the board
	 */
	public TopDownSpaceRearranger(int n) {
		numPlaces = n;
		arrangement = new Space[n];
		for (int i = 0; i < numPieces; i++)
			arrangement[i] = new Space();
	}

	/**
	 * @param k
	 *            A list of board-places to change
	 * @param b
	 *            Whether to fill or remove them
	 */
	public void makePlaces(int[] k, boolean[] b) {
		int c = 0;
		int prevDiff = 0;
		long lastHash = 0L;
		for (int i = 0; i < numPlaces; i++) {
			if (prevDiff == 0) {
				if (c >= b.length)
					break;
				else {
					i = k[c];
					if (i != 0)
						lastHash = arrangement[i - 1].hash;
				}
			}
			if (i == k[c]) {
				arrangement[i].isFilled = b[c];
				if (b[c]) {
					prevDiff++;
					arrangement[i].hash = 0L;
				} else {
					prevDiff--;
					hash -= arrangement[i].hash;
				}
				arrangement[i].countPieces += prevDiff;
				c++;
			}
			arrangement[i].countPieces += prevDiff;
			if (arrangement[i].isFilled) {
				hash -= arrangement[i].hash;
				if (lastHash == 0L)
					arrangement[i].hash = 0L;
				else
					arrangement[i].hash = lastHash * i
							/ arrangement[i].countPieces;
				hash += arrangement[i].hash;
			} else {
				if (lastHash == 0L)
					arrangement[i].hash = 1L;
				else
					arrangement[i].hash = lastHash * i
							/ (i - arrangement[i].countPieces);
			}
			lastHash = arrangement[i].hash;
		}
	}
}