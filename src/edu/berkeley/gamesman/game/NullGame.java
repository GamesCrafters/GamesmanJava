package edu.berkeley.gamesman.game;

import java.math.BigInteger;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Game;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.hasher.NullHasher;
import edu.berkeley.gamesman.util.DependencyResolver;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * A NullGame does nothing interesting.  It just returns null.
 * You may ignore this class.
 * @author Steven Schlansker
 */
public class NullGame extends Game<Object> {

	/**
	 * Default constructor
	 * @param conf the configuration
	 */
	public NullGame(Configuration conf) {
		super(conf);
	}

	static {
		DependencyResolver.allowHasher(NullGame.class, NullHasher.class);
	}
	
	@Override
	public Collection<Object> startingPositions() {
		fail();
		return null;
	}

	@Override
	public PrimitiveValue primitiveValue(Object pos) {
		fail();
		return null;
	}

	@Override
	public Collection<Pair<String,Object>> validMoves(Object pos) {
		fail();
		return null;
	}

	@Override
	public String displayState(Object pos) {
		fail();
		return null;
	}

	@Override
	public Object hashToState(long hash) {
		fail();
		return null;
	}

	@Override
	public long stateToHash(Object state) {
		fail();
		return 0;
	}

	@Override
	public BigInteger stringToState(String pos) {
		fail();
		return null;
	}

	private void fail(){
		Util.fatalError("Please specify a game.");
	}

	@Override
	public String describe() {
		return "NullGame";
	}
	
	@Override
	public long lastHash() {
		return 0;
	}

	@Override
	public String stateToString(Object pos) {
		return null;
	}

}
