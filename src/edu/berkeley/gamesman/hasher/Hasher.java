package edu.berkeley.gamesman.hasher;

import edu.berkeley.gamesman.game.Game;

public abstract class Hasher {

	Game<?, ?> game;
	
	public void setGame(Game g){
		game = g;
	}
	
}
