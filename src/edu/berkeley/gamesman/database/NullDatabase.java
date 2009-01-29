package edu.berkeley.gamesman.database;

import java.math.BigInteger;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Values;
import edu.berkeley.gamesman.util.Util;

public final class NullDatabase extends Database {

	@Override
	public void close() {}

	@Override
	public void flush() {}

	@Override
	public Record getValue(BigInteger loc) {
		return Values.Win;
	}

	@Override
	public void initialize(String url, Configuration config, Record exampleValue) {
		Util.warn("Using NullDatabase, answers will be incorrect and nothing will be saved.");
	}

	@Override
	public void setValue(BigInteger loc, Record value) {}

}
