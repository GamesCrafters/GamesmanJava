package edu.berkeley.gamesman.hadoop.game.reversi;

public class ReversiState55 extends ReversiState<ReversiState55> {
	public ReversiState55() {
		super(5, 5);
	}

	static {
		Reversi.<ReversiState55> defineComparator(ReversiState55.class);
	}
}
