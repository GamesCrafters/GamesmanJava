package edu.berkeley.gamesman.hadoop.game.connect4;

import edu.berkeley.gamesman.hadoop.ranges.GenKey;

public class C4ModState extends GenKey<C4State, C4ModState> {

	public C4ModState(int width, int height) {
		super(new C4Hasher(width, height));
		// TODO Auto-generated constructor stub
	}

}
