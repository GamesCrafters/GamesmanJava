package edu.berkeley.gamesman.database;

import java.math.BigInteger;

import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.Record;

public final class HDFSDatabase extends Database {

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	public void flush() {
		// TODO Auto-generated method stub

	}

	@Override
	public Record getRecord(BigInteger loc) {
		System.out.println("read "+loc);
		return new Record(conf,PrimitiveValue.LOSE);
	}

	@Override
	protected void initialize(String uri) {
		// TODO Auto-generated method stub

	}

	@Override
	public void putRecord(BigInteger loc, Record value) {
		System.out.println("write "+loc);
	}

}
