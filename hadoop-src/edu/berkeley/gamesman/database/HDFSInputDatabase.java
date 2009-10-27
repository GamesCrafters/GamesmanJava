package edu.berkeley.gamesman.database;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.Path;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.RecordGroup;
import edu.berkeley.gamesman.util.LongIterator;
import edu.berkeley.gamesman.util.Util;
import edu.berkeley.gamesman.hadoop.TierMap;
import edu.berkeley.gamesman.util.biginteger.BigInteger;

import java.util.Iterator;
import java.io.IOException;

/**
 * The HDFSInputDatabase is a database designed to read directly from a remote file.
 * 
 * This database only implements the seek-less read() functions, so allows for random
 * access across all threads. Still, it is better to cache the database.
 * 
 * @author Steven Schlansker
 */
public class HDFSInputDatabase extends TierMap.MapReduceDatabase {

	protected Path myFile;

	protected FSDataInputStream fd;

	protected byte[] rawRecord;

	protected byte[] groups;

	protected int groupsLength;

	protected long offset;

	HDFSInputDatabase() {}
	HDFSInputDatabase(FileSystem fs) {super(fs);}

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
	}

	@Override
	public synchronized long getLongRecordGroup(long loc) {
		try {
			fd.readFully(loc + offset, rawRecord);
			long v = RecordGroup.longRecordGroup(conf, rawRecord, 0);
			return v;
		} catch (IOException e) {
			Util.fatalError("IO Error: " + e);
		}
		return 0L;
	}

	@Override
	public LongIterator getLongRecordGroups(long loc, int numGroups) {
		try {//off I go then, yes milord.
			groupsLength = numGroups * conf.recordGroupByteLength;
			if (groups == null || groups.length < groupsLength)
				groups = new byte[groupsLength];
			fd.readFully(loc + offset, groups, 0, groupsLength);
			return new LongRecordGroupByteIterator();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public synchronized void putRecordGroups(long loc,
			LongIterator recordGroups, int numGroups) {
		throw new RuntimeException("putRecordGroups called in read-only database for pos "+loc+", num "+numGroups);
	}

	@Override
	public synchronized void putRecordGroups(long loc,
			Iterator<BigInteger> recordGroups, int numGroups) {
		throw new RuntimeException("putRecordGroups called in read-only database for pos "+loc+", num "+numGroups);
	}

	@Override
	public void getBytes(long loc, byte[] arr, int off, int len) {
		try {
			fd.readFully(loc, arr, off, len);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public synchronized void seek(long position) {
		throw new RuntimeException("HDFSInputDatabase does not implement seek");
	}

	@Override
	public synchronized void getBytes(byte[] arr, int off, int len) {
		throw new RuntimeException("HDFSInputDatabase requires a position argument to getBytes");
	}

	@Override
	public synchronized void putBytes(byte[] arr, int off, int len) {
		throw new RuntimeException("putBytes unimplemented in read-only database");
	}

	protected class LongRecordGroupByteIterator implements LongIterator {
		int onByte = 0;

		public boolean hasNext() {
			return onByte < groupsLength;
		}

		public long next() {
			for (int i = 0; i < rawRecord.length; i++)
				rawRecord[i] = groups[onByte++];
			return RecordGroup.longRecordGroup(conf, rawRecord, 0);
		}
	}

	@Override
	public synchronized void putRecordGroup(long loc, long value) {
		throw new RuntimeException("putRecordGroup called in read-only database for pos "+loc);
	}

	@Override
	public synchronized void putRecordGroup(long loc, BigInteger value) {
		throw new RuntimeException("putRecordGroup (bigint) called in read-only database for pos "+loc);
	}

	@Override
	public synchronized void initialize(String loc) {
		try {
			myFile = new Path(loc);
			fd = fs.open(myFile);
			int headerLen = fd.readInt();
			byte[] header = new byte[headerLen];
			fd.readFully(header);
			if (conf == null) {
				conf = Configuration.load(header);
			}
			offset = fd.getPos();
			rawRecord = new byte[conf.recordGroupByteLength];
		} catch (IOException e) {
			e.printStackTrace();
			Util.fatalError(e.toString());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			Util.fatalError(e.toString());
		}
	}
}
