package edu.berkeley.gamesman.database;

import java.io.IOException;

import edu.berkeley.gamesman.core.Configuration;

/**
 * A database which wraps another database
 * 
 * @author dnspies
 */
public abstract class DatabaseWrapper extends Database {
	final Database db;

	public DatabaseWrapper(Database db, Configuration config, long firstRecord,
			long numRecords, boolean reading, boolean writing) {
		super(config, firstRecord, numRecords, reading, writing);
		this.db = db;
	}

	@Override
	public void close() throws IOException {
		db.close();
	}
}
