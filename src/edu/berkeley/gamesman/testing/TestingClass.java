package edu.berkeley.gamesman.testing;

import java.util.Arrays;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.ItergameState;
import edu.berkeley.gamesman.game.Connect4;

public class TestingClass {
	public static void main(String[] args) throws ClassNotFoundException {
		Configuration conf = new Configuration(Configuration
				.readProperties("jobs/Connect4_54.job"));
		Connect4 game = (Connect4) conf.getGame();
		ItergameState[] states = new ItergameState[5];
		for (int i = 0; i < 5; i++)
			states[i] = new ItergameState();
		game.setState(game.hashToState(4318920));
		game.lastMoves(states);
		System.out.println(Arrays.toString(states));
		game.setState(game.hashToState(4391851));
		game.lastMoves(states);
		System.out.println(Arrays.toString(states));
		game.setState(game.hashToState(4343263));
		game.lastMoves(states);
		System.out.println(Arrays.toString(states));
	}
}
