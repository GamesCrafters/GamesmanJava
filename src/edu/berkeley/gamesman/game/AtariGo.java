package edu.berkeley.gamesman.game;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Value;

public class AtariGo extends RectangularDartboardGame {

	public AtariGo(Configuration conf) {
		super(conf, NO_TIE);
	}

	@Override
	public Value primitiveValue() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String describe() {
		return gameWidth + "x" + gameHeight + " Atari Go";
	}
}
