package edu.berkeley.gamesman.database;

import java.math.BigInteger;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.DBRecord;
import edu.berkeley.gamesman.core.Database;

public class MemoryDatabase extends Database {

	@Override
	public void close() {}

	@Override
	public void flush() {}

	@Override
	public DBRecord getValue(BigInteger loc) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void initialize(String url, Configuration config, DBRecord exampleValue) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setValue(BigInteger loc, DBRecord value) {
		// TODO Auto-generated method stub
		
	}

}
