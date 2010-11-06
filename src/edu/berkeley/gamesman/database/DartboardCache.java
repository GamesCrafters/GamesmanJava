package edu.berkeley.gamesman.database;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.hasher.DartboardHasher;

public class DartboardCache extends DatabaseWrapper {

	public DartboardCache(Database db, String uri, Configuration config, long firstRecord, long numRecords, DartboardHasher dh) {
		super(db, uri, config, true, firstRecord, numRecords);
		//TODO Write constructor
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void putBytes(DatabaseHandle dh, long loc, byte[] arr, int off,
			int len) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void getBytes(DatabaseHandle dh, long loc, byte[] arr, int off,
			int len) {
		// TODO Auto-generated method stub

	}

	@Override
	public long getSize() {
		// TODO Auto-generated method stub
		return 0;
	}

}
