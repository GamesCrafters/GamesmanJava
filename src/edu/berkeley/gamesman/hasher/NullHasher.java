package edu.berkeley.gamesman.hasher;

import java.math.BigInteger;

/**
 * NullHasher is a placeholder for games that do not allow use of a custom hasher.
 * It does nothing interesting.
 * @author Steven Schlansker
 */
public class NullHasher extends Hasher {

	@Override
	public BigInteger hash(char[] board) {
		return null;
	}

	@Override
	public char[] unhash(BigInteger hash, int l) {
		return null;
	}

	@Override
	public BigInteger hash(char[] board, int l) {
		return null;
	}

	@Override
	public BigInteger maxHash(int boardlen) {
		return null;
	}

}
