package edu.berkeley.gamesman.database;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Values;
import edu.berkeley.gamesman.util.Util;

public final class NullDatabase extends Database {

	@Override
	public void close() {}

	@Override
	public void flush() {}

	@Override
	public DBValue getValue(Number loc) {
		return Values.Win;
	}

	@Override
	public void initialize(String url, Configuration config, DBValue exampleValue) {
		Util.warn("Using NullDatabase, answers will be incorrect and nothing will be saved.");
	}

	@Override
	public void setValue(Number loc, DBValue value) {}

}
