package edu.berkeley.gamesman.database;

import edu.berkeley.gamesman.core.Record;

/**
 * A testing database
 * 
 * @author dnspies
 */
public class RecordDatabase extends Database {
	/**
	 * When instantiated this variable is set to the instantiated RecordDatabase
	 * so it can be called by a testing class later
	 */
	public static RecordDatabase rd;

	/**
	 * Sets rd to be this
	 */
	public RecordDatabase() {
		rd = this;
	}

	private Record[] recordArray;

	@Override
	public void getRecord(DatabaseHandle dh, long loc, Record r) {
		r.set(recordArray[(int) loc]);
	}

	@Override
	public void putRecord(DatabaseHandle dh, long loc, Record r) {
		recordArray[(int) loc] = r.clone();
	}

	@Override
	public void close() {
	}

	@Override
	public void initialize(String uri, boolean solve) {
		recordArray = new Record[(int) (conf.getGame().numHashes())];
	}

	@Override
	public void getBytes(DatabaseHandle dh, long loc, byte[] arr, int off,
			int len) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putBytes(DatabaseHandle dh, long loc, byte[] arr, int off,
			int len) {
		throw new UnsupportedOperationException();
	}
}
