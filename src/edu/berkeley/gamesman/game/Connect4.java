/**
 * 
 */
package edu.berkeley.gamesman.game;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import sun.security.util.BigInt;

import edu.berkeley.gamesman.OptionProcessor;
import edu.berkeley.gamesman.Pair;
import edu.berkeley.gamesman.Util;

/**
 * @author Steven Schlansker
 *
 */
public final class Connect4 extends TieredGame<BigInteger,Values> {

	static {
		OptionProcessor.acceptOption("p", "pieces", true, "The number of pieces in a row to win (default 4)", "4");
		OptionProcessor.nextGroup();
	}
	

	private BigInteger tierOffsets[];
	
	/**
	 * New game of Connect4
	 */
	public Connect4(){
		super();
		tierOffsets = new BigInteger[lastTier()];
		tierOffsets[0] = BigInteger.ZERO;
		for(int t = 1; t < tierOffsets.length; t++)
			tierOffsets[t] = tierOffsets[t-1].add(lastHashValueForTier(t-1));
		System.out.println(Arrays.toString(tierOffsets));
	}
	
	@Override
	public Values positionValue(BigInteger pos) {
		return Values.Undecided;
	}

	@Override
	public Collection<BigInteger> startingPositions() {
		ArrayList<BigInteger> a = new ArrayList<BigInteger>();
		a.add(BigInteger.ZERO);
		return a;
	}

	@Override
	public Iterator<BigInteger> validMoves(BigInteger pos) {
		Pair<Integer, BigInteger> ti = tierIndexForState(pos);
		
		int newTier = ti.car+1;
		BigInteger oldRearrange = ti.cdr;
		
		return null; //TODO
	}

	public BigInteger gameStateForTierIndex(int tier, BigInteger index) {
		return tierOffsets[tier].add(index);
	}

	public BigInteger lastHashValueForTier(int tier) {
		return BigInteger.valueOf(gameWidth).pow(tier);
	}

	public int lastTier() {
		return (gameWidth*gameHeight)+1;
	}

	public BigInteger hashOffestForTier(int tier) {
		return tierOffsets[tier];
	}
	
	public Pair<Integer, BigInteger> tierIndexForState(BigInteger state) {
		if(state.compareTo(lastHashValueForTier(tierOffsets.length)) >= 0)
			Util.fatalError("State out of range");
		for(int t = tierOffsets.length - 1; t >= 0; t--)
			if(state.compareTo(tierOffsets[t]) >= 0)
				return new Pair<Integer,BigInteger>(t,state.subtract(tierOffsets[t]));
		Util.fatalError("Invaild state index");
		return null;
	}

	@Override
	public String stateToString(BigInteger pos) {
		Pair<Integer, BigInteger> ti = tierIndexForState(pos);
		char[][] print = new char[gameHeight][gameWidth];
		int[] dropplace = new int[gameWidth];
		
		BigInteger gW = BigInteger.valueOf(gameWidth);
		
		String str = "";
		
		BigInteger rearr = ti.cdr;
		int numPieces = ti.car;
		
		BigInteger col = rearr;
		
		for(int pieceno = 0; pieceno < ti.car; pieceno++){
			int dropcol = col.mod(gW).intValue();
			str += "Placed piece "+pieceno+" in column "+dropcol+"\n";
			col = col.divide(gW);
			if(dropplace[dropcol] > gameHeight-1)
				Util.fatalError("Bad state - too many drops in one column");
			print[(gameHeight-1)-(dropplace[dropcol]++)][dropcol] = (pieceno%2 == 0 ? 'X' : 'O');
		}
		
		for(char[] row : print){
			str += "|";
			for(char cell : row){
				str += (cell > 0 ? cell : ' ');
			}
			str += "|\n";
		}
		
		return str+"\nNum pieces: "+ti.car+", rearr: "+ti.cdr;
	}

	@Override
	public BigInteger hashToState(BigInteger hash) {
		return hash;
	}

	@Override
	public BigInteger stateToHash(BigInteger state) {
		return state;
	}
	
}
