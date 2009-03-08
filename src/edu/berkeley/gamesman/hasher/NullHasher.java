package edu.berkeley.gamesman.hasher;

import java.math.BigInteger;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Hasher;

/**
 * NullHasher is a placeholder for games that do not allow use of a custom hasher.
 * It does nothing interesting.
 * @author Steven Schlansker
 * @param <Board> 
 */
public class NullHasher extends Hasher<Object> {
	/**
	 * Default constructor
	 * @param conf the configuration
	 */
	public NullHasher(Configuration conf) {
		super(conf);
	}

	@Override
	public String describe() {
		return "NullHasher";
	}

	@Override
	public BigInteger hash(Object board, int l) {
		return null;
	}

	@Override
	public BigInteger maxHash(int boardlen) {
		return null;
	}

	@Override
	public Object unhash(BigInteger hash, int l) {
		return null;
	}
}
