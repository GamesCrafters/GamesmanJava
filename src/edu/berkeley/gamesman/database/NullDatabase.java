package edu.berkeley.gamesman.database;

import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.RecordGroup;
import edu.berkeley.gamesman.util.biginteger.BigInteger;

/**
 * The NullDatabase is a database that simply throws away its results and
 * returns bogus records when you query it.
 * 
 * @author Steven Schlansker
 */
public final class NullDatabase extends Database {

	@Override
	public void close() {
	}

	@Override
	public void flush() {
	}

	@Override
	public RecordGroup getRecordGroup(long loc) {
		return new RecordGroup(null, BigInteger.ZERO);
	}

	@Override
	public void initialize(String url) {
		// Util.warn("Using NullDatabase, answers will be incorrect and nothing will be saved.");
	}

	@Override
	public void putRecordGroup(long loc, RecordGroup value) {
	}

}
