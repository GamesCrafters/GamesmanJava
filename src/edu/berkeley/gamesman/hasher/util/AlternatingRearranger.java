package edu.berkeley.gamesman.hasher.util;

import java.math.BigInteger;

import edu.berkeley.gamesman.util.Util;

/**
 * The AlternatingRearrangerHash perfectly encodes boards where pieces are placed by two alternating players
 * There should be no empty spaces.  Use a tiered game if possible.
 * 
 * @author Steven Schlansker
 */
public final class AlternatingRearranger {
	
	private AlternatingRearranger(){}

	
	public static BigInteger hash(char[] board, int w) {
		int[] count = countxo(board,w);
		BigInteger val = pieceRearrange(board,0,count[0],count[1],count[2]);
		return val;
	}

	public static char[] unhash(BigInteger hash, int l) {
		int x = l/2 + l%2;
		int o = l/2;

		char[] result = new char[l];

		pieceUnrearrange(hash, result, 0, l, x, o);
		
		return result;
	}
	
	protected static int[] countxo(char[] board, int l){
		int ret[] = new int[3];
		for(char c : board){
			if(ret[0] == l) break;
			
			ret[0]++;
			if(c == 'X' || c == 'x') ret[1]++;
			else ret[2]++;

		}
		return ret;
	}
	
	protected static BigInteger pieceRearrange(char[] board, int src, int pcs, int x, int o){
		if(pcs == 1 || x == 0) return BigInteger.ZERO;
		boolean cache = false;
		String cachestr = null;
		
		BigInteger rVal;
		
//		if(board.length - src <= cacheLevel){
//			cachestr = new String(board).substring(src)+pcs+""+x+""+o;
//			if((rVal = rearrCache.get(cachestr)) != null)
//				return rVal;
//			cache = true;
//		}
		
		if(board[src] == 'X'){
			rVal = pieceRearrange(board, src+1, pcs-1, x-1, o);
		}else{
			BigInteger off = BigInteger.valueOf(Util.nCr_arr[pcs-1][x-1]);
			rVal = pieceRearrange(board, src+1, pcs-1, x, o-1);
			rVal = off.add(rVal);
		}
		
//		if(cache){
//			rearrCache.put(cachestr, rVal);
//		}

		return rVal;
	}
	
	protected static void pieceUnrearrange(BigInteger hash,char[] board, int src, int pcs, int x, int o){
		if(pcs == 0) return;

		BigInteger off;
		boolean cache = false;
		
//		if(board.length - src == cacheLevel){
//			String str;
//			if((str = unrearrCache.get(hash)) != null){
//				str.getChars(0, str.length()-1, board, src);
//				return;
//			}
//			cache = true;
//		}
		
		if(x == 0)
			off = BigInteger.ZERO;
		else
			off = BigInteger.valueOf(Util.nCr_arr[pcs-1][x-1]);
		if((hash.compareTo(off) < 0 || o == 0) && x > 0){
			board[src] = 'X';
			pieceUnrearrange(hash, board, src+1, pcs-1, x-1, o);
		}else{
			board[src] = 'O';
			pieceUnrearrange(hash.subtract(off), board, src+1, pcs-1, x, o-1);
		}
		
//		if(cache){
//		//	unrearrCache.put(hash, new String(board,src,board.length-src));
//		}
	}

	public static BigInteger maxHash(int boardlen) {
		return BigInteger.valueOf(Util.nCr(boardlen,(boardlen+1)/2));//.subtract(BigInteger.ONE);
	}

}
