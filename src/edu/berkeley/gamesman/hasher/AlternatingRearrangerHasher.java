package edu.berkeley.gamesman.hasher;

import java.math.BigInteger;
import java.util.Arrays;

import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.util.Util;

/**
 * The AlternatingRearrangerHash perfectly encodes boards where pieces are placed by two alternating players
 * There should be no empty spaces.  Use a tiered game if possible.
 * 
 * @author Steven Schlansker
 */
public class AlternatingRearrangerHasher extends Hasher {

	@Override
	public void setGame(Game<?,?> game, char[] p){
		Util.assertTrue(p.length == 2);
		super.setGame(game,p);
	}
	
	@Override
	public BigInteger hash(char[] board, int l) {
		int[] count = countxo(board,l);
		BigInteger val = pieceRearrange(board,0,count[0],count[1],count[2]);
		Util.debug("ARH hashes "+Arrays.toString(board)+" ("+l+") to "+val);
		return val;
	}

	@Override
	public char[] unhash(BigInteger hash, int l) {
		int x = l/2 + l%2;
		int o = l/2;
		int s = l;
		int i;

		char[] result = new char[l];

		
		
		return result;
	}
	
	protected int[] countxo(char[] board, int l){
		int ret[] = new int[3];
		for(char c : board){
			if(ret[0] == l) break;
			
			ret[0]++;
			if(c == 'X' || c == 'x') ret[1]++;
			else ret[2]++;

		}
		return ret;
	}
	
	protected BigInteger pieceRearrange(char[] board, int src, int pcs, int x, int o){
		if(pcs == 1 || x == 0) return BigInteger.ZERO;
		
		BigInteger rVal;
		
		if(board[src] == 'X'){
			Util.debug("No offset with p="+pcs+" x="+x);
			rVal = pieceRearrange(board, src+1, pcs-1, x-1, o);
		}else{
			BigInteger off = BigInteger.valueOf(Util.nCr(pcs-1, x-1));
			Util.debug("Adding offset "+off+" with p="+pcs+" x="+x);
			rVal = pieceRearrange(board, src+1, pcs-1, x, o-1);
			rVal = off.add(rVal);
		}

		return rVal;
	}

	@Override
	public BigInteger maxHash(int boardlen) {
		return BigInteger.valueOf(Util.nCr(boardlen,(boardlen+1)/2)).subtract(BigInteger.ONE);
	}

}
