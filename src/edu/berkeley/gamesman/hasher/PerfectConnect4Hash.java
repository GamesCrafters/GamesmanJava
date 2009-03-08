package edu.berkeley.gamesman.hasher;

import java.math.BigInteger;
import java.util.Arrays;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.TieredHasher;
import edu.berkeley.gamesman.game.connect4.C4Board;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * A Hasher designed specifically for use with the Connect4 game.  It is "perfect"
 * in that every hash [0..n] is a valid Connect4 game board.
 * Valid here is defined as the proper number of pieces are on the board (up to 1
 * more black than red) and that there are no "floating" pieces.
 * We estimate that 6x7 Connect4 will use approx. 2^42 hash states for the complete
 * solve is the current estimate.
 * 
 * @author Steven Schlansker
 */
public class PerfectConnect4Hash extends TieredHasher<C4Board> {

	C4UniformPieceHasher uh;
	AlternatingRearrangerHasher ah;
	
	
	/**
	 * Default constructor
	 * @param conf the configuration
	 */
	public PerfectConnect4Hash(Configuration conf) {
		super(conf);
		
		gameWidth = conf.getGame().getGameWidth();
		gameHeight = conf.getGame().getGameHeight();
		
		Util.nCr_prefill(gameWidth*gameHeight, gameWidth*gameHeight);

		char[] arr = new char[gameHeight+1];
		
		for(char i = 0; i < arr.length; i++){
			arr[i] = Character.forDigit(i, Character.MAX_RADIX);
		}

		uh = new C4UniformPieceHasher(conf,arr);
		ah = new AlternatingRearrangerHasher(conf,pieces);
		//uh.initialize(arr);
		//ah.initialize(pieces);
	}

	private static final long serialVersionUID = -5681133082461042797L;


	private int gameWidth,gameHeight;
	
	@Override
	public BigInteger numHashesForTier(int tier) {
		char[] colh;
		colh = uh.unhash(BigInteger.valueOf(tier),gameWidth);
		
		int sum = 0;
		for(char h : colh)
			sum += Character.digit(h, Character.MAX_RADIX);
		BigInteger mh = ah.maxHash(sum);
		Util.debug(DebugFacility.HASHER,"UPH says ",Arrays.toString(colh)," for tier ",tier," sum = ",sum," maxhash = ",mh);
		return mh;
	}

	@Override
	public C4Board gameStateForTierAndOffset(int tier, BigInteger index) {
		char[] colheights = uh.unhash(BigInteger.valueOf(tier),gameWidth);
		
		int sum = 0;
		for(char h : colheights)
			sum += Character.digit(h, Character.MAX_RADIX);
		
		//Util.debug("Tier = "+tier+" Unhash says "+sum+" pieces placed as UPH = "+Arrays.toString(colheights));
		
		char[] linpieces = ah.unhash(index,sum);
		
		//Util.debug("ARH("+index+") gives us "+Arrays.toString(linpieces));
		
		char[][] ret = new char[gameWidth][gameHeight];
		
		int lidx = 0;
		
		for(int x = 0; x < gameWidth; x++){
			for(int y = 0; y < gameHeight; y++){
				if(Character.digit(colheights[x],Character.MAX_RADIX) <= y)
					ret[x][y] = ' ';
				else
					ret[x][y] = linpieces[lidx++];
			}
		}
		return new C4Board(ret);
	}
	
	@Override
	public int numberOfTiers() {
		return uh.maxHash(gameWidth).intValue()+1;
	}

	@Override
	public Pair<Integer, BigInteger> tierIndexForState(C4Board st) {
		int index = 0;
		final int w = game.getGameWidth(), h = game.getGameHeight();
		char[] linear = new char[w*h];
		char[] colheights = new char[w];
		char[][] state = st.getCharBoard();
		
		//Util.debug("Connect4 has board "+Arrays.deepToString(state));
		
		for(int x = 0; x < w; x++){
			int colheight = 0;
			for(int y = 0; y < h; y++){
				if(state[x][y] == 'O' || state[x][y] == 'X'){
					colheight++;
					linear[index++] = state[x][y];
				}
			}
			colheights[x] = Character.forDigit(colheight, Character.MAX_RADIX);
			//Util.debug("Height of col "+x+" is "+colheight);
			colheight = 0;
		}
		
		int uhh = uh.hash(colheights,colheights.length).intValue();
		BigInteger arh = ah.hash(linear,index);
		
		//Util.debug("Connect4 shows UPH = "+Arrays.toString(colheights)+" ("+uhh+") ARH ("+arh+") = "+Arrays.toString(linear));
		
		return new Pair<Integer,BigInteger>(uhh,arh);
	}

	@Override
	public String describe() {
		return "PC4H";
	}
}
