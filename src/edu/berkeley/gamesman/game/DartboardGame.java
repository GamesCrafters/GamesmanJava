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
	protected final int gameWidth, gameHeight;

	final long[] moveAdds;

	int numPieces;

	SpaceRearranger piece_empty;

	PieceRearranger o_x;

	/**
	 * @param conf
	 *            The configuration object
	 */
	public DartboardGame(Configuration conf) {
		super(conf);
		gameWidth = conf.getInteger("gamesman.game.width", getDefaultWidth());
		gameHeight = conf
				.getInteger("gamesman.game.height", getDefaultHeight());
		moveAdds = new long[gameWidth * gameHeight];
	}

	/**
	 * @return The default width of the board
	 */
	public abstract int getDefaultWidth();

	/**
	 * @return The default height of the board
	 */
	public abstract int getDefaultHeight();

	private void setOXs() {
		try {
			o_x = new PieceRearranger(piece_empty.toString(), numPieces / 2,
					(numPieces + 1) / 2);
			ArrayList<Pair<Integer, Long>> children = piece_empty.getChildren();
			for (Pair<Integer, Long> child : children)
				moveAdds[child.car] = child.cdr*o_x.arrangements;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String displayState() {
		String s = stateToString();
		StringBuffer str = new StringBuffer((gameWidth + 3) * gameHeight);
		for (int row = gameHeight - 1; row >= 0; row--)
			str.append("|"
					+ s.substring(row * gameWidth, row * (gameWidth + 1) - 1)
					+ "|\n");
		return str.toString();
	}

	@Override
	public ItergameState getState() {
		return new ItergameState(getTier(), piece_empty.getHash()
				* o_x.arrangements + o_x.getHash());
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
		return BigInteger.valueOf(piece_empty.arrangements*o_x.arrangements);
	}

	@Override
	public BigInteger numHashesForTier(int tier) {
		return BigInteger.valueOf(Util.nCr(gameWidth * gameHeight, tier)*Util.nCr(tier, tier / 2));
	}

	@Override
	public int numStartingPositions() {
		return 1;
	}

	/**
	 * @param row
	 *            The row
	 * @param col
	 *            The column
	 * @return Whether this place is on a board of this size
	 */
	public boolean exists(int row, int col) {
		return row >= 0 && row < gameHeight && col >= 0 && col < gameWidth;
	}

	/**
	 * @param row
	 *            The row
	 * @param col
	 *            The column
	 * @return 'O' 'X' or ' '
	 */
	public char get(int row, int col) {
		return o_x.get(row * gameWidth + col);
	}

	@Override
	public void setStartingPosition(int n) {
		setTier(0);
	}

	@Override
	public void setState(ItergameState pos) {
		setTier(pos.tier());
		long hash0 = pos.hash()/o_x.arrangements;
		long hash1 = pos.hash()%o_x.arrangements;
		piece_empty.unHash(hash0);
		setOXs();
		o_x.unHash(hash1);
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

	private Pair<Integer, Integer> rowCol(int piece) {
		return new Pair<Integer, Integer>(piece / gameWidth, piece % gameWidth);
	}

	@Override
	public ArrayList<Pair<String, ItergameState>> validMoves() {
		ArrayList<Pair<Integer, Long>> moves = o_x
				.getChildren((numPieces % 2 == 1) ? 'O' : 'X');
		ArrayList<Pair<String, ItergameState>> retMoves = new ArrayList<Pair<String, ItergameState>>(
				moves.size());
		for (Pair<Integer, Long> move : moves) {
			Pair<Integer, Integer> rowCol = rowCol(move.car);
			String s = "r" + rowCol.car + "c" + rowCol.cdr;
			long hashVal = moveAdds[move.car]+move.cdr;
			retMoves.add(new Pair<String, ItergameState>(s, new ItergameState(
					getTier(), hashVal)));
		}
		return retMoves;
	}

	@Override
	public int numberOfTiers() {
		return gameHeight * gameWidth + 1;
	}

}
