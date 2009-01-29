package edu.berkeley.gamesman.hasher;

import java.math.BigInteger;

import edu.berkeley.gamesman.game.Game;

/**
 * PC4Hash - the Perfect Connect4 Hash
 * 
 * TODO: abstract of how this works
 * @author Steven Schlansker
 */
public class PC4Hash extends Hasher {

	GenericHasher gh;
	
	public PC4Hash(){
		gh = new GenericHasher();
	}
	
	@Override
	public void setGame(Game<char[][],?> game){
		super.setGame(game);
		gh.setGame(game);
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
