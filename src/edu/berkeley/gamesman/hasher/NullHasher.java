package edu.berkeley.gamesman.hasher;

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
	public long hash(Object board) {
		return 0;
	}

	@Override
	public long maxHash() {
		return 0;
	}

	@Override
	public Object unhash(long hash) {
		return null;
	}
}
