package edu.berkeley.gamesman.database;

import edu.berkeley.gamesman.core.Configuration;

public final class SplitDatabase extends Database {
	public SplitDatabase(String uri, Configuration conf, boolean solve,
			long firstRecord, long numRecords) {
		super(uri, conf, solve, firstRecord, numRecords);
		String[] dbs = conf.getProperty("gamesman.db.uris").split(";");
		databases = new Database[dbs.length];
		long location = firstRecord();
		for (int d = 0; d < databases.length; d++) {
			String[] dString = dbs[d].split("-");
			databases[d] = Database.openDatabase(dString[0], false, location,
					Long.parseLong(dString[1]));
			location += databases[d].numRecords();
		}
		location = firstRecord();
	}

	private final Database[] databases;

	@Override
	protected void closeDatabase() {
		for (int i = 0; i < databases.length; i++) {
			databases[i].close();
		}
	}

	@Override
	public void getBytes(DatabaseHandle dh, long location, byte[] arr, int off,
			int len) {
		long nextStart;
		int database = getDatabase(location);
		while (len > 0) {
			if (database < databases.length - 1)
				nextStart = Math.min(location + len, databases[database + 1]
						.firstRecord());
			else
				nextStart = location + len;
			databases[database].getBytes(((SplitHandle) dh).handles[database],
					location, arr, off, (int) (nextStart - location));
			off += nextStart - location;
			len -= nextStart - location;
			location = nextStart;
			database++;
		}
	}

	private int getDatabase(long location) {
		int low = 0, high = databases.length;
		int guess;
		while (high - low > 1) {
			guess = (low + high) / 2;
			if (location < databases[guess].firstRecord())
				high = guess;
			else
				low = guess;
		}
		return low;
	}

	private class SplitHandle extends DatabaseHandle {
		private final DatabaseHandle[] handles;

		private SplitHandle() {
			super(null);
			handles = new DatabaseHandle[databases.length];
			for (int i = 0; i < databases.length; i++) {
				handles[i] = databases[i].getHandle();
			}
		}
	}

	@Override
	public DatabaseHandle getHandle() {
		return new SplitHandle();
	}

	@Override
	public void closeHandle(DatabaseHandle dh) {
		SplitHandle sh = (SplitHandle) dh;
		for (int i = 0; i < sh.handles.length; i++)
			databases[i].closeHandle(sh.handles[i]);
	}

	@Override
	public void putBytes(DatabaseHandle dh, long loc, byte[] arr, int off,
			int len) {
		throw new UnsupportedOperationException();
	}
}
