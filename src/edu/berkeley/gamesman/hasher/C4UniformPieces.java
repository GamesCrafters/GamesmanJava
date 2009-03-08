package edu.berkeley.gamesman.hasher;


import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Hasher;
import edu.berkeley.gamesman.util.Task;
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
 * @see AlternatingRearranger
 */
public final class C4UniformPieces {

	/**
	 * Default constructor
	 * @param conf the configuration
	 */
	public C4UniformPieces(Configuration conf, int l, char[] pieces) {
		lookup = null;
		table = null;
		idx = 0;
		init(new char[l],0,pieces,l*pieces.length);
	}
	
	Map<String,BigInteger> lookup;
	String[] table;
	
	public BigInteger hash(char[] board, int l) {
		return lookup.get(new String(board));
	}

	public char[] unhash(BigInteger hash, int l) {
		return table[hash.intValue()].toCharArray();
	}

	public BigInteger maxHash(int boardlen) {
		return BigInteger.valueOf(table.length-1);
	}
	
	private int idx;
	transient private Task task;
	
	protected void init(char[] board,int off, char[] mypcs, int sum){
		if(lookup == null){
			lookup = new ConcurrentHashMap<String,BigInteger>();
			table = new String[(int)Util.longpow(mypcs.length,board.length)];
			idx = 0;
			task = Task.beginTask("Initializing C4 Column Hash");
			
			task.setTotal(table.length-1);
			
			for(int s = 0; s < sum; s++)
				init(board,off,mypcs,s);
			task.complete();
			return;
		}
		
		
		for(int cur = 0; cur < mypcs.length; cur++){
			board[off] = mypcs[cur];
			if(off == board.length-1){
				if(sum == cur){
					String str = new String(board);
					table[idx] = str;
					lookup.put(str, BigInteger.valueOf(idx));
					idx++;
					if(idx % 1000 == 0)
						task.setProgress(idx);
				}
			}else{
				init(board,off+1,mypcs,sum-cur);
			}
		}
		
		//if(off == 0)
		//	Util.debug("CUPH finished building table: "+Arrays.toString(table));
	}
}
