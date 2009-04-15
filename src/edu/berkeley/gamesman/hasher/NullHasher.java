package edu.berkeley.gamesman.hasher;

import java.math.BigInteger;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Hasher;

/**
 * NullHasher is a placeholder for games that do not allow use of a custom hasher.
 * It does nothing interesting.
 * @author Steven Schlansker
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
	public BigInteger hash(Object board) {
		return null;
	}

	@Override
	public BigInteger maxHash() {
		return null;
	}

	@Override
	public Object unhash(BigInteger hash) {
		return null;
	}
}
