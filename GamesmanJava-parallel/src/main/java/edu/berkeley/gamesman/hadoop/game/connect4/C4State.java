package edu.berkeley.gamesman.hadoop.game.connect4;

import edu.berkeley.gamesman.hasher.counting.CountingState;
import edu.berkeley.gamesman.hasher.genhasher.GenHasher;

public class C4State extends CountingState {
	private final int width, height;

	public C4State(GenHasher<? extends CountingState> myHasher, int width,
			int height) {
		super(myHasher, width * height);
		this.width = width;
		this.height = height;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(width * height);
		for (int row = height - 1; row >= 0; row--) {
			sb.append('|');
			for (int col = 0; col < width; col++) {
				sb.append(Connect4.charFor(get(col * height + row)));
				sb.append('|');
			}
			sb.append('\n');
		}
		return sb.toString();
	}
}
