package edu.berkeley.gamesman.database;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.RecordGroup;
import edu.berkeley.gamesman.util.LongIterator;
import edu.berkeley.gamesman.util.Util;
import edu.berkeley.gamesman.util.biginteger.BigInteger;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.Map;
import java.util.HashMap;
import java.io.EOFException;
import java.io.IOException;

public class HadoopSplitDatabase extends Database {
	FileSystem fs;
	Configuration conf;
	public HadoopSplitDatabase (FileSystem fs, Configuration conf) {
		this.conf = conf;
		this.fs = fs;
	}
	@Override
	public void initialize(String splitfilename) {
		databaseTree = new TreeMap<Long, HDFSInputDatabase>();
		databaseEnd = new HashMap<Long, Long>();
		try {
			FSDataInputStream fd = fs.open(new Path(splitfilename));
			long next = 0;
			while (true) {
				long start = fd.readLong();
				assert next == start;
				long len = fd.readLong();
				if (len <= 0) {
					Util.fatalError("Invalid length "+start+" in split database "+splitfilename);
				}
				String filename = fd.readUTF();
				HDFSInputDatabase db = new HDFSInputDatabase(fs, conf);
				db.initialize(filename);
				databaseTree.put(start, db);
				databaseEnd.put(start, start+len);
			}
		} catch (EOFException e) {
			// Nothing left in our list of databases, stop the loop.
		} catch (IOException e) {
			Util.fatalError("IOException in loading split database", e);
		}
	}

	public void close() {
		for (Map.Entry<Long,HDFSInputDatabase> dbpair : databaseTree.entrySet()) {
			dbpair.getValue().close();
		}
		databaseTree = null;
		databaseEnd = null;
	}

	private NavigableMap<Long, HDFSInputDatabase> databaseTree;
	private Map<Long, Long> databaseEnd;

	@Override
	public synchronized void putRecordGroup(long loc, long value) {
		throw new RuntimeException("putRecordGroup (long) unimplemented in read-only database");
	}

	@Override
	public synchronized void putRecordGroup(long loc, BigInteger value) {
		throw new RuntimeException("putRecordGroup (bigint) unimplemented in hadoop database");
	}

	@Override
	public synchronized void putRecordGroups(long loc,
			LongIterator recordGroups, int numGroups) {
		throw new RuntimeException("putRecordGroups (longiter) unimplemented in read-only split database");
	}

	@Override
	public synchronized void putRecordGroups(long loc,
			Iterator<BigInteger> recordGroups, int numGroups) {
		throw new RuntimeException("putRecordGroups (bigintiter) unimplemented in hadoop database");
	}

	private final HDFSInputDatabase getDatabaseFor(long loc) {
		return databaseTree.get(databaseTree.floorKey(loc));
	}

	@Override
	public synchronized long getLongRecordGroup(long loc) {
		Database db = getDatabaseFor(loc);
		return db.getLongRecordGroup(loc);
	}

	@Override
	public synchronized BigInteger getBigIntRecordGroup(long loc) {
		Util.fatalError("BigInteger HadoopDatabase not implemented");
		return BigInteger.ZERO;
	}

	@Override
	public synchronized void flush() {
	}

	@Override
	public synchronized void seek(long position) {
		throw new RuntimeException("HadoopSplitDatabase does not implement seek");
	}

	@Override
	public synchronized void getBytes(long loc, byte[]arr, int offset, int length) {
		while (length > 0) {
			long currentDbStart = databaseTree.floorKey(loc);
			long currentDbEnd = databaseEnd.get(loc);
			HDFSInputDatabase db = databaseTree.get(currentDbStart);
			int amtRead;
			if (length > currentDbEnd - loc) {
				amtRead = (int)(currentDbEnd - loc);
			} else {
				amtRead = length;
			}
			assert amtRead > 0;
			db.getBytes(loc, arr, offset, amtRead);
			length -= amtRead;
			loc += amtRead;
			offset += amtRead;
		}
	}

	@Override
	public synchronized void getBytes(byte[]arr, int offset, int length) {
		throw new RuntimeException("HadoopSplitDatabase requires a position argument to getBytes");
	}

	@Override
	public synchronized void putBytes(byte[] arr, int off, int len) {
		throw new RuntimeException("putBytes unimplemented in read-only database");
	}

	@Override
	public LongIterator getLongRecordGroups(long loc, int numGroups) {
		return new LongIterator () {
			private long currentDbStart = 0;
			private long currentDbEnd = 0;
			private HDFSInputDatabase db;
			private long loc;
			private int numGroups;
			public boolean hasNext() {
				return numGroups > 0;
			}
			public long next() {
				if (loc >= this.currentDbEnd) {
					currentDbStart = databaseTree.floorKey(loc);
					currentDbEnd = databaseEnd.get(loc);
					HDFSInputDatabase db = databaseTree.get(currentDbStart);
				}
				long recordGroup = db.getLongRecordGroup(loc);
				loc ++;
				return recordGroup;
			}
			public void remove() {
				throw new RuntimeException("Cannot remove from HadoopDatabase");
			}
		};
	}
}

