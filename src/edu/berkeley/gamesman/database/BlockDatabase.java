package edu.berkeley.gamesman.database;

import java.math.BigInteger;

import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Database;

/**
 * A simple BlockDatabase that stores records in a local file
 * It should pack records better than FileDatabase but otherwise be very simple.
 * May be removed for a better DB later.
 * @author Steven Schlansker
 */
public class BlockDatabase extends Database {

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void flush() {
		// TODO Auto-generated method stub
	}

	@Override
	public Record getValue(BigInteger loc) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void initialize(String url) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setValue(BigInteger loc, Record value) {
		// TODO Auto-generated method stub
		
	}

}
