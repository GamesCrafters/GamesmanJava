package edu.berkeley.gamesman.hasher;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Hasher;
import edu.berkeley.gamesman.core.State;

/**
 * NullHasher is a placeholder for games that do not allow use of a custom hasher.
 * It does nothing interesting.
 * @author Steven Schlansker
 */
public class NullHasher extends Hasher<State> {
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
	public long hash(State board) {
		return 0;
	}

	@Override
	public long numHashes() {
		return 0;
	}

	@Override
	public State unhash(long hash) {
		return null;
	}
}
