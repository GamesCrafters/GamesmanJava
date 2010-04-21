package edu.berkeley.gamesman.database;

import java.util.Random;

import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.Record;

public class DummyDatabase extends Database {
	Random rand = new Random();

	@Override
	public void close() {

	}

	@Override
	public void flush() {

	}

	@Override
	public void getBytes(byte[] arr, int off, int len) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void initialize(String uri, boolean solve) {

	}

	@Override
	public void putBytes(byte[] arr, int off, int len) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void seek(long loc) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void getRecord(long recordIndex, Record r) {
		r.value = PrimitiveValue.values[rand.nextInt(conf.valueStates)];
		r.remoteness = rand.nextInt(conf.remotenessStates);
	}
}
