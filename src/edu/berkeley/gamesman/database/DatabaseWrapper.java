package edu.berkeley.gamesman.database;

import edu.berkeley.gamesman.core.Configuration;

public abstract class DatabaseWrapper extends Database {
	Database db;

	public DatabaseWrapper(Database db, String uri, Configuration config,
			boolean solve, long firstRecord, long numRecords) {
		super(uri, config, solve, firstRecord, numRecords, db.getHeader());
		this.db = db;
	}
}
