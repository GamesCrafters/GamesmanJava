package edu.berkeley.gamesman.database;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;

import edu.berkeley.gamesman.core.RecordGroup;
import edu.berkeley.gamesman.util.LongIterator;
import edu.berkeley.gamesman.hadoop.TierMap;
import edu.berkeley.gamesman.util.Util;
import edu.berkeley.gamesman.util.biginteger.BigInteger;

import java.util.Iterator;
import java.io.IOException;

/**
 * The HDFSOutputDatabase is a database designed to write directly to a remote file.
 * 
 * This database only implements the sequential writes, so you must have one
 * HDFSOutputDatabase for each concurrent writing process at a time.
 * 
 * Create an HDFSOuptutDatabase by calling HadoopSplitDatabase.beginWrite()
 * and close/add to the reduce queue using HadoopSplitDatabase.endWrite()
 * 
 * @author Steven Schlansker
 */
public class HDFSOutputDatabase extends TierMap.MapReduceDatabase {

	protected Path myFile;

	protected FSDataOutputStream fd;

	protected byte[] rawRecord;

	protected byte[] groups;

	protected int groupsLength;

	protected long offset;

	HDFSOutputDatabase() {}
	HDFSOutputDatabase(FileSystem fs) {super(fs);}

	@Override
	public synchronized void close() {
		try {
			fd.close();
		} catch (IOException e) {
			Util.warn("Error while closing input stream for database: " + e);
		}
	}

	@Override
	public synchronized void flush() {
		try {
			fd.flush();
			fd.sync();
		} catch (IOException e) {
			Util.fatalError("Error while writing to database: " + e);
		}
	}

	@Override
	public synchronized long getLongRecordGroup(long loc) {
		throw new RuntimeException("getLongRecordGroup called in write-only database for pos "+loc);
	}

	@Override
	public synchronized BigInteger getBigIntRecordGroup(long loc) {
		throw new RuntimeException("getBigIntRecordGroup called in write-only database for pos "+loc);
	}

	@Override
	public LongIterator getLongRecordGroups(long loc, int numGroups) {
		throw new RuntimeException("getLongRecordGroups called in write-only database for pos "+loc+", num "+numGroups);
	}

	@Override
        public synchronized void getBytes(byte[] arr, int off, int len) {
		throw new RuntimeException("getBytes called in write-only database for len "+len);
	}

	/**
	 * @return the current file pointer of the sequential write database.
	 */
	public final long getPosition() {
		try {
			return fd.getPos();
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
	}

	@Override
	public final void seek(long loc) {
		assert (loc + offset == getPosition());
	}

	@Override
	public void putBytes(byte[] arr, int off, int len) {
		try {
			fd.write(arr, off, len);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void putRecordGroups(long loc,
			LongIterator recordGroups, int numGroups) {
		try {
			groupsLength = numGroups * conf.recordGroupByteLength;
			if (groups == null || groups.length < groupsLength)
				groups = new byte[groupsLength];
			int onByte = 0;
			for (int i = 0; i < numGroups; i++) {
				RecordGroup.toUnsignedByteArray(conf, recordGroups.next(),
						groups, onByte);
				onByte += conf.recordGroupByteLength;
			}
			seek(loc);
			fd.write(groups, 0, groupsLength);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void putRecordGroups(long loc,
			Iterator<BigInteger> recordGroups, int numGroups) {
		try {
			groupsLength = numGroups * conf.recordGroupByteLength;
			if (groups == null || groups.length < groupsLength)
				groups = new byte[groupsLength];
			int onByte = 0;
			for (int i = 0; i < numGroups; i++) {
				RecordGroup.toUnsignedByteArray(conf, recordGroups.next(),
						groups, onByte);
				onByte += conf.recordGroupByteLength;
			}
			assert (loc + offset == fd.getPos());
			fd.write(groups, 0, groupsLength);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void putRecordGroup(long loc, long value) {
		try {
			assert (loc + offset == fd.getPos());
			RecordGroup.outputUnsignedBytes(conf, value, (java.io.DataOutput)fd);
		} catch (IOException e) {
			Util.fatalError("IO Error: " + e);
		}
	}

	@Override
	public void putRecordGroup(long loc, BigInteger value) {
		try {
			assert (loc + offset == fd.getPos());
			RecordGroup.outputUnsignedBytes(conf, value, (java.io.DataOutput)fd);
		} catch (IOException e) {
			Util.fatalError("IO Error: " + e);
		}
	}

	@Override
	public void initialize(String loc) {
		try {
			myFile = new Path(loc);
			boolean previouslyExisted = fs.exists(myFile);
			if (previouslyExisted) {
				Util.fatalError("Not overwriting existing output file "+myFile);
			}
			if (conf == null) {
				Util.fatalError("No configuration, but the database is to be created");
			}
			fd = fs.create(myFile);
			byte[] b = conf.store();
			fd.writeInt(b.length);
			fd.write(b);
			offset = fd.getPos();
			rawRecord = new byte[conf.recordGroupByteLength];
		} catch (IOException e) {
			e.printStackTrace();
			Util.fatalError(e.toString());
		}
	}
}