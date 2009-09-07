package edu.berkeley.gamesman.database;

import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.RecordGroup;

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
	public Record getRecord(long loc) {
		return recordArray[(int) loc].clone();
	}

	@Override
	public void putRecord(long loc, Record r) {
		recordArray[(int) loc] = r.clone();
	}

	@Override
	public void close() {
	}

	@Override
	public void flush() {
	}

	@Override
	public RecordGroup getRecordGroup(long loc) {
		return null;
	}

	@Override
	public void initialize(String uri) {
		recordArray = new Record[(int) (conf.getGame().lastHash()) + 1];
	}

	@Override
	public void putRecordGroup(long loc, RecordGroup rg) {
	}

}
