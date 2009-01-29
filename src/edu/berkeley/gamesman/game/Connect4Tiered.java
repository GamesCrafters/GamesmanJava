/**
 * 
 */
package edu.berkeley.gamesman.game;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import edu.berkeley.gamesman.OptionProcessor;
import edu.berkeley.gamesman.Pair;
import edu.berkeley.gamesman.Util;

/**
 * 
 * Tiered Connect-4 implementation
 * Supports arbitrary sized boards and unlimited hash size
 * Slightly inefficient in its representation of boards
 * @author Steven Schlansker
 *
 */
public final class Connect4Tiered extends TieredGame<BigInteger,Values> {

	static {
		OptionProcessor.acceptOption("p", "pieces", true, "The number of pieces in a row to win (default 4)", "4");
		OptionProcessor.nextGroup();
	}
	

	private BigInteger tierOffsets[];
	
	protected int winPieces;
	
	/**
	 * New game of Connect4
	 */
	public Connect4Tiered(){
		super();
		tierOffsets = new BigInteger[numberOfTiers()];
		tierOffsets[0] = BigInteger.ZERO;
		for(int t = 1; t < tierOffsets.length; t++)
			tierOffsets[t] = tierOffsets[t-1].add(lastHashValueForTier(t-1));
		System.out.println(Arrays.toString(tierOffsets));
		
		winPieces = Integer.parseInt(OptionProcessor.checkOption("pieces"));
		
		if(winPieces > gameWidth || winPieces > gameHeight)
			Util.fatalError("You can't win if you need more pieces in a row than fit on the board!");
	}
	
	@Override
	public Values positionValue(BigInteger pos) {
		char[][] board = board(pos);
		if(board == null) return Values.Invalid;
		Util.fatalError(Arrays.deepToString(board) + pos);
		return null;
	}

	@Override
	public Collection<BigInteger> startingPositions() {
		ArrayList<BigInteger> a = new ArrayList<BigInteger>();
		a.add(BigInteger.ZERO);
		return a;
	}

	@Override
	public Collection<BigInteger> validMoves(BigInteger pos) {		
		Pair<Integer, BigInteger> ti = tierIndexForState(pos);
		
		int oldTier = ti.car,newTier = ti.car+1;
		BigInteger oldRearrange = ti.cdr;
		
		if(newTier >= numberOfTiers()) return new ArrayList<BigInteger>();
		
		int numDrops[] = new int[gameWidth];
		
		BigInteger col = oldRearrange;
		
		int drops[] = new int[oldTier];
		
		for(int pieceno = 0; pieceno < oldTier; pieceno++){
			BigInteger[] divrem = col.divideAndRemainder(BigInteger.valueOf(gameWidth));
			int dropcol = divrem[1].intValue();
			col = divrem[0];
			if(numDrops[dropcol] >= gameHeight)
				return null;
			numDrops[dropcol]++;
			drops[pieceno] = dropcol;
		}
		
		ArrayList<BigInteger> validMoves = new ArrayList<BigInteger>(gameWidth);
		
		BigInteger newBase = hashOffestForTier(newTier).add(oldRearrange);
		
		for(int column = 0; column < gameWidth; column++){
			if(numDrops[column] < gameHeight)
				validMoves.add(newBase.add(BigInteger.valueOf(column).multiply((BigInteger.valueOf(gameWidth).pow(oldTier)))));
		}
		
		return validMoves;
	}

	public BigInteger gameStateForTierIndex(int tier, BigInteger index) {
		return tierOffsets[tier].add(index);
	}

	public BigInteger lastHashValueForTier(int tier) {
		return BigInteger.valueOf(gameWidth).pow(tier);
	}

	public int numberOfTiers() {
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
		char[][] print = board(pos);
		if(print == null) return "Invalid board "+pos;
		Pair<Integer, BigInteger> ti = tierIndexForState(pos);
		String str = "";
		
		for(char[] row : print){
			str += "|";
			for(char cell : row){
				str += (cell > 0 ? cell : ' ');
			}
			str += "|\n";
		}
		
		return str+"\nNum pieces: "+ti.car+", rearr: "+ti.cdr+", hash = "+pos;
	}
	
	private char[][] board(BigInteger pos){
		Pair<Integer, BigInteger> ti = tierIndexForState(pos);
		char[][] print = new char[gameHeight][gameWidth];
		int[] dropplace = new int[gameWidth];
		
		BigInteger gW = BigInteger.valueOf(gameWidth);
		
		BigInteger rearr = ti.cdr;
		int numPieces = ti.car;
		
		BigInteger col = rearr;
		
		for(int pieceno = 0; pieceno < numPieces; pieceno++){
			BigInteger[] divrem = col.divideAndRemainder(gW);
			int dropcol = divrem[1].intValue();
			//str += "Placed piece "+pieceno+" in column "+dropcol+"\n";
			col = divrem[0];
			if(dropplace[dropcol] > gameHeight-1)
				return null;
			print[(gameHeight-1)-(dropplace[dropcol]++)][dropcol] = (pieceno%2 == 0 ? 'X' : 'O');
		}
		return print;
	}
	
	public BigInteger stringToState(String board){
		Util.fatalError("Not Implemented :("); // FIXME
		return null;
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
