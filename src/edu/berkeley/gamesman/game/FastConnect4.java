package edu.berkeley.gamesman.game;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.game.connect4.OneTierC4Board;
import edu.berkeley.gamesman.hasher.TieredIteratorHasher;
import edu.berkeley.gamesman.util.DependencyResolver;
import edu.berkeley.gamesman.util.Pair;

/**
 * @author DNSpies
 * For connect 4 hashing
 */
public class FastConnect4 extends TieredIteratorGame {

	private final int piecesToWin;
	private OneTierC4Board otc4b;

	static {
		DependencyResolver.allowHasher(FastConnect4.class,
				TieredIteratorHasher.class);
	}

	/**
	 * @param conf The configuration object
	 */
	public FastConnect4(Configuration conf) {
		super(conf);
		piecesToWin = Integer.parseInt(conf.getProperty("connect4.pieces", "4"));
	}

	@Override
	public int getDefaultBoardHeight() {
		return 6;
	}

	@Override
	public int getDefaultBoardWidth() {
		return 7;
	}

	@Override
	public String describe() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public char[] pieces() {
		return new char[] {'X','O'};
	}
	
	public PrimitiveValue primitiveValue(){
		return otc4b.primitiveValue();
	}

	@Override
	public void setStartingPosition(int n) {
		setTier(0);
	}
	
	@Override
	public int numStartingPositions(){
		return 1;
	}

	@Override
	public String stateToString() {
		return toString();
	}

	@Override
	public Collection<Pair<String, IteratorState>> validMoves() {
		TieredIteratorHasher h = (TieredIteratorHasher) conf.getHasher();
		ArrayList<Pair<String,IteratorState>> al = new ArrayList<Pair<String,IteratorState>>();
		Iterator<Pair<Integer, IteratorState>> moves = otc4b.validMoves().iterator();
		
		while(moves.hasNext()){
			Pair<Integer,IteratorState> m = moves.next();
			IteratorState ob = h.gameStateForTierAndOffset(m.cdr.tier(), m.cdr.hash());
			al.add(new Pair<String,IteratorState>("c"+m.car,ob));
		}
		
		return al;
	}

	@Override
	public FastConnect4 clone() {
		FastConnect4 fc4 = new FastConnect4(conf);
		if(otc4b!=null)
			fc4.otc4b=otc4b.clone();
		return fc4;
	}

	@Override
	public IteratorState getState() {
		return otc4b.getState();
	}

	@Override
	public boolean hasNextHashInTier() {
		return otc4b.getHash().compareTo(
				otc4b.numHashesForTier().subtract(BigInteger.ONE)) < 0;
	}

	@Override
	public void nextHashInTier() {
		otc4b.next();
	}

	@Override
	public void setState(IteratorState pos) {
		setTier(pos.tier());
		otc4b.unhash(pos.hash());
	}

	@Override
	public void setToString(String pos) {
		setTier(getTier(pos));
		otc4b.setToString(pos);
	}

	private int getTier(String pos) {
		int tier=0;
		int multiplier=1;
		for(int col=0;col<gameWidth;col++){
			for(int row=0;row<gameHeight;row++){
				if(pos.charAt(gameWidth*row+col)==' ')
					break;
				else
					tier+=multiplier;
			}
			multiplier*=(gameHeight+1);
		}
		return tier;
	}

	@Override
	public String displayState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BigInteger numHashesForTier() {
		return otc4b.numHashesForTier();
	}

	@Override
	public void setTier(int tier) {
		otc4b=new OneTierC4Board(gameWidth, gameHeight, piecesToWin, tier);
	}

	@Override
	public int getTier() {
		return otc4b.getTier();
	}
	
	@Override
	public String toString(){
		if(otc4b==null)
			return "";
		else
			return otc4b.toString();
	}
}