package edu.berkeley.gamesman.parallel.game.tootandotto;

import edu.berkeley.gamesman.hasher.counting.CountingState;

public class TootNOttoState extends CountingState {
	private TOHasher myHasher;

	public TootNOttoState(TOHasher myHasher, int countTo) {
		super(myHasher, countTo);
		this.myHasher = myHasher;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int row = myHasher.height - 1; row >= 0; row--) {
			for (int col = 0; col < myHasher.width; col++) {
				sb.append("|");
				sb.append(TootAndOtto.charFor(TootAndOtto.get(this, row, col,
						myHasher.height)));
			}
			sb.append("|");
			sb.append("\n");
		}
		sb.append("P1 T's: " + get(myHasher.boardSize + 3) + "\n");
		sb.append("P1 O's: " + get(myHasher.boardSize + 2) + "\n");
		sb.append("P2 T's: " + get(myHasher.boardSize + 1) + "\n");
		sb.append("P2 O's: " + get(myHasher.boardSize) + "\n");
		return sb.toString();
	}
}
