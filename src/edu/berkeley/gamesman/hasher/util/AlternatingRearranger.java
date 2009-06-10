package edu.berkeley.gamesman.hasher.util;

import edu.berkeley.gamesman.util.Util;

/**
 * The AlternatingRearrangerHash perfectly encodes boards where pieces are
 * placed by two alternating players There should be no empty spaces. Use a
 * tiered game if possible.
 * 
 * @author Steven Schlansker
 */
public final class AlternatingRearranger {

	private AlternatingRearranger() {
	}

	public static long hash(char[] board, int w) {
		int[] count = countxo(board, w);
		long val = pieceRearrange(board, 0, count[0], count[1], count[2]);
		return val;
	}

	public static char[] unhash(long hash, int l) {
		int x = l / 2 + l % 2;
		int o = l / 2;

		char[] result = new char[l];

		pieceUnrearrange(hash, result, 0, l, x, o);

		return result;
	}

	protected static int[] countxo(char[] board, int l) {
		int ret[] = new int[3];
		for (char c : board) {
			if (ret[0] == l)
				break;

			ret[0]++;
			if (c == 'X' || c == 'x')
				ret[1]++;
			else
				ret[2]++;

		}
		return ret;
	}

	protected static long pieceRearrange(char[] board, int src, int pcs,
			int x, int o) {
		if (pcs == 1 || x == 0)
			return 0;
		boolean cache = false;
		String cachestr = null;

		long rVal;

		// if(board.length - src <= cacheLevel){
		// cachestr = new String(board).substring(src)+pcs+""+x+""+o;
		// if((rVal = rearrCache.get(cachestr)) != null)
		// return rVal;
		// cache = true;
		// }

		if (board[src] == 'X') {
			rVal = pieceRearrange(board, src + 1, pcs - 1, x - 1, o);
		} else {
			long off = Util.nCr_arr[pcs - 1][x - 1];
			rVal = pieceRearrange(board, src + 1, pcs - 1, x, o - 1);
			rVal = off+rVal;
		}

		// if(cache){
		// rearrCache.put(cachestr, rVal);
		// }

		return rVal;
	}

	protected static void pieceUnrearrange(long hash, char[] board, int src,
			int pcs, int x, int o) {
		if (pcs == 0)
			return;

		long off;
		boolean cache = false;

		// if(board.length - src == cacheLevel){
		// String str;
		// if((str = unrearrCache.get(hash)) != null){
		// str.getChars(0, str.length()-1, board, src);
		// return;
		// }
		// cache = true;
		// }

		if (x == 0)
			off = 0;
		else
			off = Util.nCr_arr[pcs - 1][x - 1];
		if ((hash < off || o == 0) && x > 0) {
			board[src] = 'X';
			pieceUnrearrange(hash, board, src + 1, pcs - 1, x - 1, o);
		} else {
			board[src] = 'O';
			pieceUnrearrange(hash - off, board, src + 1, pcs - 1, x, o - 1);
		}

		// if(cache){
		// // unrearrCache.put(hash, new String(board,src,board.length-src));
		// }
	}

	public static long maxHash(int boardlen) {
		return Util.nCr(boardlen, (boardlen + 1) / 2);// .subtract(BigInteger.ONE);
	}

}
