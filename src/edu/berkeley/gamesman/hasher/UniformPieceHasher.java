// THIS CLASS WAS NOT USED BY ANYONE, SO IT IS COMMENTED OUT
// PLEASE VERIFY CORRECTNESS BEFORE REUSING
// - scs 03/07/09


package edu.berkeley.gamesman.hasher;

public final class UniformPieceHasher {}

//
//
//import java.math.BigInteger;
//import java.util.Arrays;
//import java.util.HashMap;
//
//import edu.berkeley.gamesman.core.Configuration;
//import edu.berkeley.gamesman.core.Hasher;
//import edu.berkeley.gamesman.util.DebugFacility;
//import edu.berkeley.gamesman.util.Util;
//
///**
// * The UniformPieceHasher is a perfect hash given a 1-dimensional board with a fixed list of possible pieces
// * Note that this is only a perfect hash if every location can contain any piece
// * As an example, tic-tac-toe is /not/ a UniformPieceHasher-style game as (|X|-|O|) E {0,1}
// * but this hasher could represent the board XXO-XXO-XXO
// * Use AlternatingRearrangerHasher instead
// * 
// * Few games are like this, so it is mostly a utility hasher used by other hashers piecewise.
// * @author Steven Schlansker
// * @see AlternatingRearranger
// */
//public final class UniformPieceHasher extends Hasher<char[]> {
//	
//	/**
//	 * Default constructor
//	 * @param conf the configuration
//	 */
//	public UniformPieceHasher(Configuration conf) {
//		super(conf);
//		lookup = new HashMap<Character,BigInteger>();
//		for(int i = 0; i < pieces.length; i++)
//			lookup.put(pieces[i], BigInteger.valueOf(i));
//		plen = BigInteger.valueOf(pieces.length);
//	}
//	
////	/**
////	 * Default constructor
////	 * @param conf the configuration
////	 * @param p the piece array
////	 */
////	public UniformPieceHasher(Configuration conf,char[] p){
////		super(conf,p);
////		lookup = new HashMap<Character,BigInteger>();
////		for(int i = 0; i < p.length; i++)
////			lookup.put(p[i], BigInteger.valueOf(i));
////		plen = BigInteger.valueOf(p.length);
////	}
//
//	private static final long serialVersionUID = 5625295202888543943L;
//	HashMap<Character, BigInteger> lookup;
//	BigInteger plen;
//	
//	@Override
//	public BigInteger hash(char[] board, int l) {
//		BigInteger hash = BigInteger.ZERO;
//		for(int i = 0; i < l; i++)
//				hash = hash.multiply(plen).add(lookup.get(board[i]));
//		Util.debug(DebugFacility.HASHER,"UPH hashes ",Arrays.toString(board)," to ",hash);
//		return hash;
//	}
//
//	@Override
//	public char[] unhash(BigInteger ahash, int l) {
//		BigInteger hash = ahash;
//		char[] ret = new char[l];
//		
//		for(int i = l-1; i >= 0;i--){
//				ret[i] = pieces[hash.mod(plen).intValue()];
//				hash = hash.divide(plen);
//		}
//		return ret;
//	}
//
//	@Override
//	public BigInteger maxHash(int boardlen) {
//		BigInteger hash = BigInteger.ZERO;
//		for(int i = 0; i < boardlen; i++)
//				hash = hash.multiply(plen).add(lookup.get(pieces[pieces.length-1]));
//		return hash;
//	}
//
//	@Override
//	public String describe() {
//		return "UPH"+Arrays.toString(pieces);
//	}
//	
//}
