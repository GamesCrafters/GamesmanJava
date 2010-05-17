package edu.berkeley.gamesman.database;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Random;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.Record;

/**
 * A testing database
 * 
 * @author dnspies
 */
public class RandomDatabase extends Database {
	Random rand;

	@Override
	public Record getRecord(long loc) {
		Record r = conf.getGame().newRecord();
		r.remoteness = rand.nextInt(conf.remotenessStates);
		r.value = PrimitiveValue.values[rand.nextInt(conf.valueStates)];
		return r;
	}

	@Override
	public void close() {
	}

	@Override
	public void initialize(String uri, boolean solve) {
	}

	/**
	 * @param args
	 *            The job file
	 * @throws ClassNotFoundException
	 *             If Configuration cannot find a class
	 * @throws IOException
	 *             If there's an IOException writing to the database file
	 */
	public static void main(String[] args) throws ClassNotFoundException,
			IOException {
		Properties props = Configuration.readProperties(args[0]);
		Configuration conf = new Configuration(props);
		byte[] confBytes = conf.store();
		File f = new File(conf.getProperty("gamesman.db.uri"));
		FileOutputStream fos = new FileOutputStream(f);
		for (int i = 24; i >= 0; i -= 8) {
			fos.write(confBytes.length >>> i);
		}
		fos.write(confBytes);
		fos.close();
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
