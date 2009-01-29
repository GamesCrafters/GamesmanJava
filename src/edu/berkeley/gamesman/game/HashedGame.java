package edu.berkeley.gamesman.game;

import java.math.BigInteger;
import java.util.Collection;

import edu.berkeley.gamesman.util.Util;

public abstract class HashedGame<Value> extends Game<char[][], Value> {

	@Override
	public final char[][] hashToState(BigInteger hash) {
		return hasher.unhash(hash);
	}

	@Override
	public final BigInteger stateToHash(char[][] pos) {
		return hasher.hash(pos);
	}

	@Override
	public final String stateToString(char[][] pos) {
		StringBuilder str = new StringBuilder((pos.length+1)*(pos[0].length+1));
		for(int y = 0; y < pos.length; y++){
			str.append("|");
			for(int x = 0; x < pos[0].length; x++){
				str.append(pos[x][y]);
			}
			str.append("|\n");
		}
		return str.toString();
	}

	@Override
	public final BigInteger stringToState(String pos) {
		// TODO Auto-generated method stub
		Util.assertTrue(false);
		return null;
	}

}
