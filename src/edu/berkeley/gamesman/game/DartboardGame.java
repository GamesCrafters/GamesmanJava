package edu.berkeley.gamesman.game;

import java.math.BigInteger;
import java.util.ArrayList;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.ItergameState;
import edu.berkeley.gamesman.core.TieredIterGame;
import edu.berkeley.gamesman.game.util.PieceRearranger;
import edu.berkeley.gamesman.game.util.SpaceRearranger;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * A game in which there are a fixed number of spaces in which either player can
 * move and each time a space is played, that move becomes no longer available.<br />
 * Examples: Tic Tac Toe, Hex, Five In A Row
 * 
 * @author DNSpies
 */
public abstract class DartboardGame extends TieredIterGame {
	int numPieces;
	SpaceRearranger piece_empty;
	final BigInteger[] moveAdds;
	PieceRearranger o_x;

	/**
	 * @param conf The configuration object
	 */
	public DartboardGame(Configuration conf) {
		super(conf);
		moveAdds = new BigInteger[gameWidth*gameHeight];
	}

	private void setOXs() {
		try {
			o_x = new PieceRearranger(piece_empty.toString(), numPieces / 2,
					(numPieces + 1) / 2);
			ArrayList<Pair<Integer, BigInteger>> children = piece_empty
					.getChildren();
			for (Pair<Integer, BigInteger> child : children)
				moveAdds[child.car] = child.cdr.multiply(o_x.arrangements);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String displayState() {
		String s = stateToString();
		StringBuffer str = new StringBuffer((gameWidth+3)*gameHeight);
		for(int row=gameHeight-1;row>=0;row--)
			str.append("|"+s.substring(row*gameWidth,row*(gameWidth+1)-1)+"|\n");
		return str.toString();
	}

	@Override
	public ItergameState getState() {
		return new ItergameState(getTier(), piece_empty.getHash().multiply(
				o_x.arrangements).add(o_x.getHash()));
	}

	@Override
	public int getTier() {
		return numPieces;
	}

	@Override
	public boolean hasNextHashInTier() {
		return (o_x.hasNext() || piece_empty.hasNext());
	}

	@Override
	public void nextHashInTier() {
		if (o_x.hasNext())
			o_x.next();
		else {
			piece_empty.next();
			setOXs();
		}
	}

	@Override
	public BigInteger numHashesForTier() {
		return piece_empty.arrangements.multiply(o_x.arrangements);
	}
	
	@Override
	public BigInteger numHashesForTier(int tier) {
		return BigInteger.valueOf(Util.nCr(gameWidth * gameHeight, tier))
				.multiply(BigInteger.valueOf(Util.nCr(tier, tier / 2)));
	}

	@Override
	public int numStartingPositions() {
		return 1;
	}

	/**
	 * @param row The row
	 * @param col The column
	 * @return Whether this place is on a board of this size
	 */
	public boolean exists(int row, int col) {
		return row >= 0 && row < gameHeight && col >= 0 && col < gameWidth;
	}

	/**
	 * @param row The row
	 * @param col The column
	 * @return 'O' 'X' or ' '
	 */
	public char get(int row, int col) {
		return o_x.get(row*gameWidth+col);
	}

	@Override
	public void setStartingPosition(int n) {
		setTier(0);
	}

	@Override
	public void setState(ItergameState pos) {
		setTier(pos.tier());
		BigInteger[] hashes = pos.hash().divideAndRemainder(o_x.arrangements);
		piece_empty.unHash(hashes[0]);
		setOXs();
		o_x.unHash(hashes[1]);
	}

	@Override
	public void setTier(int tier) {
		numPieces = tier;
		piece_empty = new SpaceRearranger(numPieces, gameHeight * gameWidth
				- numPieces);
		setOXs();
	}

	@Override
	public void setToString(String pos) {
		piece_empty = new SpaceRearranger(pos);
		try {
			o_x = new PieceRearranger(pos);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String stateToString() {
		return o_x.toString();
	}
	
	private Pair<Integer, Integer> rowCol(int piece){
		return new Pair<Integer, Integer>(piece/gameWidth,piece%gameWidth);
	}

	@Override
	public ArrayList<Pair<String, ItergameState>> validMoves() {
		ArrayList<Pair<Integer, BigInteger>> moves = o_x
				.getChildren((numPieces % 2 == 1) ? 'O' : 'X');
		ArrayList<Pair<String, ItergameState>> retMoves = new ArrayList<Pair<String, ItergameState>>(
				moves.size());
		for (Pair<Integer, BigInteger> move : moves) {
			Pair<Integer, Integer> rowCol = rowCol(move.car);
			String s = "r" + rowCol.car + "c" + rowCol.cdr;
			BigInteger hashVal = moveAdds[move.car].add(move.cdr);
			retMoves.add(new Pair<String, ItergameState>(s, new ItergameState(
					getTier(), hashVal)));
		}
		return retMoves;
	}

	@Override
	public char[] pieces() {
		return new char[] { 'X', 'O' };
	}
}
