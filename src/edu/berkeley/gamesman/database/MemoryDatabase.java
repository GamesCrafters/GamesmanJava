package edu.berkeley.gamesman.database;

import java.math.BigInteger;

import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Util;

/**
 * A database that operates on memory and loses all changes made if not copied to 
 * a different database before being closed.
 * @author Steven Schlansker
 */
public class MemoryDatabase extends Database {

	@Override
	public void close() {}

	@Override
	public void flush() {}

	@Override
	public Record getValue(BigInteger loc) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void initialize(String url) {
		Util.debug(DebugFacility.Database,"Using MemoryDatabase, if you don't explicitly copy its contents somewhere they will be lost on close.");
	}

	@Override
	public void setValue(BigInteger loc, Record value) {
		// TODO Auto-generated method stub
		
	}

}
