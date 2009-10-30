package edu.berkeley.gamesman.database;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.Path;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.util.LongIterator;
import edu.berkeley.gamesman.util.Util;
import edu.berkeley.gamesman.util.biginteger.BigInteger;
import edu.berkeley.gamesman.hadoop.TierMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map;
import java.util.HashMap;
import java.io.EOFException;
import java.io.IOException;

/**
 * HadoopSplitDatabase contains a NavigableMap of read databases, sorted by
 * a range of hash values, so that it is possible to read from a previous
 * tier without combining all the mini-databases.
 * 
 * For writing databases, it uses beginWrite() and endWrite() in order to
 * create a mini-database only used for one thread of the solve.
 * 
 * @author Steven Schlansker
 */
public class HadoopSplitDatabase extends TierMap.MapReduceDatabase {

	public HadoopSplitDatabase() {
	}

	public HadoopSplitDatabase(FileSystem fs) {
		super(fs);
	}

	@Override
	public void initialize(String splitfilename) {
		databaseTree = new TreeMap<Long, Database>();
		databaseEnd = new HashMap<Long, Long>();
		int lastslash = splitfilename.lastIndexOf('/');
		inputFilenameBase = splitfilename.substring(0, lastslash + 1);
		if (fs==null) {
			Util.fatalError("Filesystem null in HadoopSplitDatabase.");
		}
		try {
			FSDataInputStream fd = fs.open(new Path(splitfilename));
			long next = 0;
			while (true) {
				long start = fd.readLong();
				assert next == start;
				long len = fd.readLong();
				if (len <= 0) {
					Util.fatalError("Invalid length " + start
							+ " in split database " + splitfilename);
				}
				String filename = fd.readUTF();
				Database db = new HDFSInputDatabase(fs);
				db.initialize(inputFilenameBase + filename, conf);
				databaseTree.put(start, db);
				databaseEnd.put(start, start + len);
			}
		} catch (EOFException e) {
			// Nothing left in our list of databases, stop the loop.
		} catch (IOException e) {
			Util.fatalError("IOException in loading split database", e);
		}
	}

	public void close() {
		for (Map.Entry<Long, Database> dbpair : databaseTree.entrySet()) {
			dbpair.getValue().close();
		}
		databaseTree = null;
		databaseEnd = null;
	}

	Configuration conf;

	String inputFilenameBase;

	private SortedMap<Long, Database> databaseTree;

	private Map<Long, Long> databaseEnd;

	protected final Long getDatabaseKeyFor(long loc) {
		return databaseTree.headMap(loc).firstKey();
	}

	protected final Database getDatabaseFor(long loc) {
		return databaseTree.get(getDatabaseKeyFor(loc));
	}

	@Override
	public Database beginWrite(int tier, long startRecord, long stopRecord) {
		HDFSOutputDatabase db = new HDFSOutputDatabase(fs);
		String name = new Path(tier + ".hdb." + startRecord, outputFilenameBase)
				.toString();
		db.initialize(name, conf);

		if (delegate != null) {
			delegate.started(tier, name, startRecord, stopRecord);
		}
		return db;
	}

	public void endWrite(int tier, Database db, long start, long end) {
		HDFSOutputDatabase hdb = (HDFSOutputDatabase) db;
		String fileName = hdb.myFile.getName();
		db.close();

		if (delegate != null) {
			delegate.finished(tier, fileName, start, end);
		}
	}

	@Override
	public synchronized void putBytes(byte[] arr, int offset, int length) {
		throw new RuntimeException(
				"HadoopSplitDatabase requires a position argument to putBytes");
	}

	@Override
	public synchronized void putBytes(long loc, byte[] arr, int offset,
			int length) {
		throw new RuntimeException(
			"putBytes can only be called from beginWrite()'s return value");
	}

	@Override
	public long getLongRecordGroup(long loc) {
		Database db = getDatabaseFor(loc);
		return db.getLongRecordGroup(loc);
	}

	@Override
	public BigInteger getBigIntRecordGroup(long loc) {
		Util.fatalError("BigInteger HadoopDatabase not implemented");
		return BigInteger.ZERO;
	}

	@Override
	public void flush() {
	}

	@Override
	public synchronized void seek(long position) {
		throw new RuntimeException(
				"HadoopSplitDatabase does not implement seek");
	}

	@Override
	public void getBytes(long loc, byte[] arr, int offset,
			int length) {
		while (length > 0) {
			long currentDbStart = getDatabaseKeyFor(loc);
			long currentDbEnd = databaseEnd.get(loc);
			Database db = databaseTree.get(currentDbStart);
			int amtRead;
			if (length > currentDbEnd - loc) {
				amtRead = (int) (currentDbEnd - loc);
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
	public synchronized void getBytes(byte[] arr, int offset, int length) {
		throw new RuntimeException(
				"HadoopSplitDatabase requires a position argument to getBytes");
	}

	@Override
	public LongIterator getLongRecordGroups(long loc, int numGroups) {
		return new LongIterator() {
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
					currentDbStart = getDatabaseKeyFor(loc);
					currentDbEnd = databaseEnd.get(loc);
					db = databaseTree.get(currentDbStart);
				}
				long recordGroup = db.getLongRecordGroup(loc);
				loc++;
				return recordGroup;
			}
		};
	}
}
