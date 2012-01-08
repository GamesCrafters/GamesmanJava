package edu.berkeley.gamesman.hadoop.game.connect4;

import edu.berkeley.gamesman.hadoop.ranges.Range;

public class C4Range extends Range<C4ModState> {
	private final int width, height;

	public C4Range(int width, int height) {
		super(new C4ModState(width, height), new C4ModState(width, height));
		this.width = width;
		this.height = height;
	}

	@Override
	public C4ModState newKey() {
		return new C4ModState(width, height);
	}

}
