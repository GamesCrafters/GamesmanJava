package edu.berkeley.gamesman.hasher;


import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;

import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.util.Util;

/**
 * The UniformPieceHasher is a perfect hash given a 1-dimensional board with a fixed list of possible pieces
 * Note that this is only a perfect hash if every location can contain any piece
 * As an example, tic-tac-toe is /not/ a UniformPieceHasher-style game as (|X|-|O|) E {0,1}
 * but this hasher could represent the board XXO-XXO-XXO
 * Use AlternatingRearrangerHasher instead
 * 
 * Few games are like this, so it is mostly a utility hasher used by other hashers piecewise.
 * @author Steven Schlansker
 * @see AlternatingRearrangerHasher
 */
public final class C4UniformPieceHasher extends Hasher {
	
	HashMap<String,BigInteger> lookup;
	String[] table;
	protected char[] pcs;
	
	public void setGame(Game<?,?> game, char[] p){
		super.setGame(game,p);
		pcs = p;
		lookup = null;
		table = null;
		idx = 0;
	}
	
	@Override
	public BigInteger hash(char[] board, int l) {
		if(lookup == null) init(new char[l],0,pcs,l*pcs.length);
		return lookup.get(new String(board));
	}

	@Override
	public char[] unhash(BigInteger hash, int l) {
		if(lookup == null) init(new char[l],0,pcs,l*pcs.length);
		return table[hash.intValue()].toCharArray();
	}

	@Override
	public BigInteger maxHash(int boardlen) {
		if(lookup == null) init(new char[boardlen],0,pcs,boardlen*pcs.length);
		return BigInteger.valueOf(table.length-1);
	}
	
	private int idx;
	
	protected void init(char[] board,int off, char[] pcs, int sum){
		if(lookup == null){
			lookup = new HashMap<String,BigInteger>();
			table = new String[Util.intpow(pcs.length,board.length)];
			idx = 0;
			
			for(int s = 0; s < sum; s++)
				init(board,off,pcs,s);
			return;
		}
		
		
		for(int cur = 0; cur < pcs.length; cur++){
			board[off] = pcs[cur];
			if(off == board.length-1){
				if(sum == cur){
					String str = new String(board);
					table[idx] = str;
					lookup.put(str, BigInteger.valueOf(idx));
					idx++;
				}
			}else{
				init(board,off+1,pcs,sum-cur);
			}
		}
		
		if(off == 0)
			Util.debug("CUPH finished building table: "+Arrays.toString(table));
	}
	
}
