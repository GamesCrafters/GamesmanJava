package edu.berkeley.gamesman.hadoop.game.reversi;

public class ReversiState44 extends ReversiState<ReversiState44> {
	public ReversiState44() {
		super(4, 4);
	}

	static {
		Reversi.<ReversiState44> defineComparator(ReversiState44.class);
	}
}
