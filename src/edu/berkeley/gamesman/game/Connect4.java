package edu.berkeley.gamesman.game;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import edu.berkeley.gamesman.hasher.AlternatingRearrangerHasher;
import edu.berkeley.gamesman.hasher.NullHasher;
import edu.berkeley.gamesman.hasher.C4UniformPieceHasher;
import edu.berkeley.gamesman.util.DependencyResolver;
import edu.berkeley.gamesman.util.OptionProcessor;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * Connect 4!
 * Boards are stored in row-major format
 * e.g
 * O
 * XX
 * 
 * is [[xo][x ]]
 * @author Steven Schlansker
 */
public class Connect4 extends TieredGame<char[][],Values> {

	C4UniformPieceHasher uh = new C4UniformPieceHasher();
	AlternatingRearrangerHasher ah = new AlternatingRearrangerHasher();
	
	final char[] pieces = {'X','O'};
	
	int piecesToWin=4;
	
	static {
		OptionProcessor.acceptOption("p", "pieces", true, "The number of pieces in a row to win (default 4)", "4");
		OptionProcessor.nextGroup();
		DependencyResolver.allowHasher(Connect4.class, NullHasher.class);
	}

	/**
	 * Connect4 Constructor
	 * Creates the hashers we use (does not use the command-line specified one, needs special hasher)
	 */
	public Connect4(){
		super();

		char[] arr = new char[getGameHeight()+1];
		
		for(char i = 0; i < arr.length; i++){
			arr[i] = Character.forDigit(i, Character.MAX_RADIX);
		}
		
		uh.setGame(null, arr);
		ah.setGame(null, pieces);
		
		piecesToWin = Integer.parseInt(OptionProcessor.checkOption("pieces"));
	}

	@Override
	public Collection<char[][]> startingPositions() {
		ArrayList<char[][]> boards = new ArrayList<char[][]>();
		boards.add(new char[gameWidth][gameHeight]);
		return boards;
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
	public Pair<Integer, BigInteger> tierIndexForState(char[][] state) {
		int index = 0;
		final int w = getGameWidth(), h = getGameHeight();
		char[] linear = new char[w*h];
		char[] colheights = new char[w];
		
		Util.debug("Connect4 has board "+Arrays.deepToString(state));
		
		for(int x = 0; x < w; x++){
			int colheight = 0;
			for(int y = 0; y < h; y++){
				if(state[x][y] == 'O' || state[x][y] == 'X'){
					colheight++;
					linear[index++] = state[x][y];
				}
			}
			colheights[x] = Character.forDigit(colheight, Character.MAX_RADIX);
			Util.debug("Height of col "+x+" is "+colheight);
			colheight = 0;
		}
		
		int uhh = uh.hash(colheights).intValue();
		BigInteger arh = ah.hash(linear,index);
		
		Util.debug("Connect4 shows UPH = "+Arrays.toString(colheights)+" ("+uhh+") ARH ("+arh+") = "+Arrays.toString(linear));
		
		return new Pair<Integer,BigInteger>(uhh,arh);
	}

	@Override
	public BigInteger numHashesForTier(int tier) {
		char[] colh;
		colh = uh.unhash(BigInteger.valueOf(tier),gameWidth);
		
		int sum = 0;
		for(char h : colh)
			sum += Character.digit(h, Character.MAX_RADIX);
		BigInteger mh = ah.maxHash(sum);
		//Util.debug("UPH says "+Arrays.toString(colh)+" for tier "+tier+" mh = "+mh);
		return mh;
	}

	@Override
	public char[][] gameStateForTierIndex(int tier, BigInteger index) {
		char[] colheights = uh.unhash(BigInteger.valueOf(tier),gameWidth);
		
		int sum = 0;
		for(char h : colheights)
			sum += Character.digit(h, Character.MAX_RADIX);
		
		Util.debug("Tier = "+tier+" Unhash says "+sum+" pieces placed as UPH = "+Arrays.toString(colheights));
		
		char[] linpieces = ah.unhash(index,sum);
		
		Util.debug("ARH("+index+") gives us "+Arrays.toString(linpieces));
		
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
		return ret;
	}
	
	@Override
	public int numberOfTiers() {
		return uh.maxHash(gameWidth).intValue()+1;
	}

	@Override
	public Values positionValue(char[][] pos) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public final String stateToString(char[][] pos) {
		StringBuilder str = new StringBuilder((pos.length+1)*(pos[0].length+1));
		for(int y = pos[0].length-1; y >= 0; y--){
			str.append("|");
			for(int x = 0; x < pos.length; x++){
				str.append(pos[x][y]);
			}
			str.append("|\n");
		}
		return str.toString();
	}

	@Override
	public char[][] stringToState(String pos) {
		char[][] board = new char[gameWidth][gameHeight];
		for(int x = 0 ; x < gameWidth; x++){
			for(int y = 0 ; y < gameHeight; y++){
				board[x][y] = pos.charAt(Util.index(x, y, gameWidth));
			}
		}
		Util.debug("stringToState yields "+Arrays.deepToString(board));
		return board;
	}

	@Override
	public Collection<char[][]> validMoves(char[][] pos) {
		// TODO Auto-generated method stub
		return null;
	}
}
