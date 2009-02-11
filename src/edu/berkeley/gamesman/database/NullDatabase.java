package edu.berkeley.gamesman.database;

import java.math.BigInteger;

import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.util.Util;

/**
 * The NullDatabase is a database that simply throws away its results and returns
 * bogus records when you query it.
 * @author Steven Schlansker
 */
public final class NullDatabase extends Database {

	@Override
	public void close() {}

	@Override
	public void flush() {}

	@Override
	public Record getValue(BigInteger loc) {
		return new Record(conf,PrimitiveValue.Undecided);
	}

	@Override
	public void initialize(String url) {
		Util.warn("Using NullDatabase, answers will be incorrect and nothing will be saved.");
	}

	@Override
	public void setValue(BigInteger loc, Record value) {}

}
