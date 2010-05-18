package edu.berkeley.gamesman.database;

public abstract class DatabaseWrapper extends Database {
	Database db;

	public DatabaseWrapper(Database db) {
		this.db = db;
	}
}
