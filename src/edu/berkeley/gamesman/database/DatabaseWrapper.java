package edu.berkeley.gamesman.database;

import edu.berkeley.gamesman.core.Configuration;

/**
 * A database which wraps another database
 * 
 * @author dnspies
 */
public abstract class DatabaseWrapper extends Database {
	Database db;

	/**
	 * The required constructor for a database wrapper.
	 * 
	 * @param db
	 *            The database being wrapped.
	 * @param uri
	 *            The file path of the database.
	 * @param config
	 *            The configuration object
	 * @param solve
	 *            Whether this database is being solved
	 * @param firstRecord
	 *            The index of the first record this WRAPPER contains (must be
	 *            >=the index of the first record in the database)
	 * @param numRecords
	 *            The number of records this wrapper contains
	 *            (firstRecord+numRecords<=db.firstRecord()+db.numRecords()), or
	 *            -1 to mean the entire range
	 */
	public DatabaseWrapper(Database db, String uri, Configuration config,
			boolean solve, long firstRecord, long numRecords) {
		super(uri, config, solve, firstRecord, numRecords, db.getHeader());
		this.db = db;
	}
}
