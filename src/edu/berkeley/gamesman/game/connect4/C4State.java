package edu.berkeley.gamesman.game.connect4;

import java.math.BigInteger;

import edu.berkeley.gamesman.util.Pair;

/**
 * Holds the tier and hash of a position in Connect 4.
 * 
 * @author DNSpies
 * 
 */
public class C4State extends Pair<Integer, BigInteger> {
	private final int width;
	private final int height;
	private final int piecesToWin;

	C4State(int width, int height, int piecesToWin, int tier,
			BigInteger hash) {
		super(tier, hash);
		this.width = width;
		this.height = height;
		this.piecesToWin = piecesToWin;
	}

	/**
	 * @return The tier of this position
	 */
	public int tier() {
		return car;
	}

	/**
	 * @return The hash of this position
	 */
	public BigInteger hash() {
		return cdr;
	}

	/**
	 * This method is slow and shouldn't be necessary.
	 * @return A OneTierC4Board initialized to this position
	 */
	public OneTierC4Board getBoard() {
		OneTierC4Board otc4b = new OneTierC4Board(width, height, piecesToWin,
				car);
		otc4b.unhash(cdr);
		return otc4b;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 6105268460110264054L;

}
