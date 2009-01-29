package edu.berkeley.gamesman.hasher;

import java.math.BigInteger;

import edu.berkeley.gamesman.game.Game;

public final class GenericHasher extends Hasher {
	
	public void setGame(Game<char[][],?> game){
		super.setGame(game);
		
	}
	
	@Override
	public BigInteger hash(char[][] board) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public char[][] unhash(BigInteger hash) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
