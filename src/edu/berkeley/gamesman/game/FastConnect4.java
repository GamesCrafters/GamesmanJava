package edu.berkeley.gamesman.game;

import java.math.BigInteger;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.ItergameState;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.TieredIterGame;
import edu.berkeley.gamesman.game.connect4.OneTierC4Board;
import edu.berkeley.gamesman.hasher.TieredItergameHasher;
import edu.berkeley.gamesman.util.DependencyResolver;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * @author DNSpies
 * For connect 4 hashing
 */
public class FastConnect4 extends TieredIterGame {

	private final int piecesToWin, gameWidth, gameHeight;
	private OneTierC4Board otc4b;

	static {
		DependencyResolver.allowHasher(FastConnect4.class,
				TieredItergameHasher.class);
	}

	/**
	 * @param conf The configuration object
	 */
	public FastConnect4(Configuration conf) {
		super(conf);
		piecesToWin = Integer.parseInt(conf.getProperty("gamesman.game.pieces", "4"));
		gameWidth = Integer.parseInt(conf.getProperty("gamesman.game.width", "7"));
		gameHeight = Integer.parseInt(conf.getProperty("gamesman.game.height", "6"));
	}

	@Override
	public String describe() {
		return String.format("FastConnect4: %dx%d %d to win", gameWidth, gameHeight, piecesToWin);
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
	public Collection<Pair<String, ItergameState>> validMoves() {
		return otc4b.validMoves();
	}

	@Override
	public FastConnect4 clone() {
		FastConnect4 fc4 = new FastConnect4(conf);
		if(otc4b!=null)
			fc4.otc4b=otc4b.clone();
		return fc4;
	}

	@Override
	public ItergameState getState() {
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
	public void setState(ItergameState pos) {
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
		String s = otc4b.toString();
		StringBuilder str = new StringBuilder(s.length()+gameHeight*2+1);
		for(int row = gameHeight - 1;row>=0; row--){
			str.append('|');
			str.append(s.substring(row*gameWidth, (row+1)*gameWidth));
			str.append("|\n");
		}
		return str.toString();
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

	@Override
	public int numberOfTiers() {
		return (int) Util.longpow(gameHeight+1, gameWidth);
	}
}