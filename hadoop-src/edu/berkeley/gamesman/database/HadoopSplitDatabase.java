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
import edu.berkeley.gamesman.hadoop.TierMap;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.TreeMap;
import edu.berkeley.gamesman.util.Pair;
import java.util.Map;
import java.util.HashMap;
import java.io.EOFException;
import java.io.IOException;

public class HadoopSplitDatabase extends TierMap.MapReduceDatabase {

	HadoopSplitDatabase() {}
	HadoopSplitDatabase(FileSystem fs) {super(fs);}

	@Override
	public void initialize(String splitfilename) {
		databaseTree = new TreeMap<Long, Database>();
		databaseEnd = new HashMap<Long, Long>();
		int lastslash = splitfilename.lastIndexOf('/');
		inputFilenameBase = splitfilename.substring(0, lastslash+1);
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
				Database db = new HDFSInputDatabase(fs, conf);
				db.initialize(inputFilenameBase+filename);
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
		assert (taskToWritableDatabase.size() == 0);
		for (Map.Entry<Long,Database> dbpair : databaseTree.entrySet()) {
			dbpair.getValue().close();
		}
		databaseTree = null;
		databaseEnd = null;
	}

	FileSystem fs;
	Configuration conf;
	String inputFilenameBase;

	// In byte offset from start of database.
	private Map<Pair<Integer, Long>, Pair<Long, String>> taskToWritableDatabase;
	private NavigableMap<Long, Database> databaseTree;
	private Map<Long, Long> databaseEnd;

	public final Database getDatabaseFor(long loc) {
		return databaseTree.get(databaseTree.floorKey(loc));
	}

	public void beginWrite(int tier, long task, long startRecord, long stopRecord) {
		Pair<Integer, Long> tierTaskPair = new Pair<Integer, Long>(tier, task);
		Database db = new HDFSOutputDatabase(fs);
		String name = new Path(outputFilenameBase, tier+".hdb."+task).toString();
		db.initialize(name, conf);
		assert (taskToWritableDatabase.get(tierTaskPair) == null);
		taskToWritableDatabase.put(tierTaskPair, new Pair<Long, String>(startRecord, name));
		databaseTree.put(startRecord, db);
		databaseEnd.put(startRecord, stopRecord);

		if (delegate != null) {
			delegate.started(tier, task, name, startRecord, stopRecord);
		}
	}
	public void endWrite(int tier, long task) {
		Pair<Integer, Long> tierTaskPair = new Pair<Integer, Long>(tier, task);
		Pair<Long,String> startFilenamePair = taskToWritableDatabase.remove(tierTaskPair);
		long start = startFilenamePair.car;
		String filename = startFilenamePair.cdr;
		long end = databaseEnd.remove(start);
		databaseTree.remove(start).close();

		if (delegate != null) {
			delegate.finished(tier, task, filename, start, end);
		}
	}

	@Override
	public synchronized void putRecordGroup(long loc, long value) {
		Database db = getDatabaseFor(loc);
		db.putRecordGroup(loc, value);
	}

	@Override
	public synchronized void putRecordGroup(long loc, BigInteger value) {
		throw new RuntimeException("putRecordGroup (bigint) unimplemented in hadoop database");
	}

	@Override
	public synchronized void putRecordGroups(long loc,
			LongIterator recordGroups, int numGroups) {
		while (numGroups > 0) {
			long currentDbStart = databaseTree.floorKey(loc);
			long currentDbEnd = databaseEnd.get(loc);
			Database db = databaseTree.get(currentDbStart);
			int numWritten = numGroups;
			if (numWritten * conf.recordGroupByteLength > currentDbEnd - loc) {
				numWritten = (int)((currentDbEnd - loc) / conf.recordGroupByteLength);
			}
			db.putRecordGroups(loc, recordGroups, numWritten);
			numGroups -= numWritten;
		}
	}

	@Override
	public synchronized void putRecordGroups(long loc,
			Iterator<BigInteger> recordGroups, int numGroups) {
		throw new RuntimeException("putRecordGroups (bigintiter) unimplemented in hadoop database");
	}

	@Override
	public synchronized void putBytes(byte[]arr, int offset, int length) {
		throw new RuntimeException("HadoopSplitDatabase requires a position argument to putBytes");
	}

	@Override
	public synchronized void putBytes(long loc, byte[] arr, int offset, int length) {
		while (length > 0) {
			long currentDbStart = databaseTree.floorKey(loc);
			long currentDbEnd = databaseEnd.get(loc);
			Database db = databaseTree.get(currentDbStart);
			int amtRead;
			if (length > currentDbEnd - loc) {
				amtRead = (int)(currentDbEnd - loc);
			} else {
				amtRead = length;
			}
			assert amtRead > 0;
			db.putBytes(loc, arr, offset, amtRead);
			length -= amtRead;
			loc += amtRead;
			offset += amtRead;
		}
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
		//throw new RuntimeException("HadoopSplitDatabase does not implement seek");
	}

	@Override
	public synchronized void getBytes(long loc, byte[]arr, int offset, int length) {
		while (length > 0) {
			long currentDbStart = databaseTree.floorKey(loc);
			long currentDbEnd = databaseEnd.get(loc);
			Database db = databaseTree.get(currentDbStart);
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
	public LongIterator getLongRecordGroups(long loc, int numGroups) {
		return new LongIterator () {
			private long currentDbStart = 0;
			private long currentDbEnd = 0;
			private Database db;
			private long loc;
			private int numGroups;
			public boolean hasNext() {
				return numGroups > 0;
			}
			public long next() {
				if (loc >= this.currentDbEnd) {
					currentDbStart = databaseTree.floorKey(loc);
					currentDbEnd = databaseEnd.get(loc);
					Database db = databaseTree.get(currentDbStart);
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

