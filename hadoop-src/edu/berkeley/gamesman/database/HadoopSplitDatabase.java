package edu.berkeley.gamesman.database;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.Path;

import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.util.LongIterator;
import edu.berkeley.gamesman.util.Util;
import edu.berkeley.gamesman.util.biginteger.BigInteger;
import edu.berkeley.gamesman.hadoop.util.HadoopUtil;
import edu.berkeley.gamesman.hadoop.util.SplitDatabaseWritable;
import edu.berkeley.gamesman.hadoop.util.SplitDatabaseWritableList;

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
public class HadoopSplitDatabase extends HadoopUtil.MapReduceDatabase {

	/** Default constructor. Must by followed by calls to setFilesystem(),
	 * setOutputDirectory(), setDelegate(), and initialize().
	 */
	public HadoopSplitDatabase() {
	}

	/**
	 * Equivalent to default constructor, followed by setFilesystem().
	 * @param fs FileSystem, if known already
	 */
	public HadoopSplitDatabase(FileSystem fs) {
		super(fs);
	}

	@Override
	public void initialize(String splitfilename) {
		if (splitfilename == null || splitfilename.length()==0) {
			return;
		}
		databaseTree = new TreeMap<Long, Database>();
		databaseEnd = new HashMap<Long, Long>();
		int lastslash = splitfilename.lastIndexOf('/');
		inputFilenameBase = splitfilename.substring(0, lastslash + 1);
		if (fs==null) {
			Util.fatalError("Filesystem null in HadoopSplitDatabase.");
		}
		FSDataInputStream fd = null;
		try {
			fd = fs.open(new Path(splitfilename));
			long next = 0;
			SplitDatabaseWritableList list = new SplitDatabaseWritableList();
			list.readFields(fd);
			for (SplitDatabaseWritable sdw : list) {
				long start = sdw.getStart();
				assert next == start;
				long len = sdw.getLength();
				if (len <= 0) {
					Util.fatalError("Invalid length " + start
							+ " in split database " + splitfilename);
				}
				String filename = sdw.getFilename();

				Database db = new HDFSInputDatabase(fs);
				db.initialize(inputFilenameBase + filename, conf);
				databaseTree.put(start, db);
				databaseEnd.put(start, start + len);
			}
		} catch (EOFException e) {
			// Nothing left in our list of databases, stop the loop.
		} catch (IOException e) {
			Util.fatalError("IOException in loading split database", e);
		} finally {
			if (fd != null) {
				try {
					fd.close();
				} catch (IOException e) {
				}
			}
		}
	}

	public void close() {
		if (databaseTree != null) {
			for (Map.Entry<Long, Database> dbpair : databaseTree.entrySet()) {
				dbpair.getValue().close();
			}
		}
		databaseTree = null;
		databaseEnd = null;
	}

	String inputFilenameBase;

	private SortedMap<Long, Database> databaseTree;

	private Map<Long, Long> databaseEnd;

	protected final Long getDatabaseKeyFor(long loc) {
		if (databaseTree.containsKey(loc)) {
			return loc;
		}
		return databaseTree.headMap(loc).firstKey();
	}

	protected final Database getDatabaseFor(long loc) {
		return databaseTree.get(getDatabaseKeyFor(loc));
	}

	@Override
	public Database beginWrite(int tier, long startRecord, long stopRecord) {
		HDFSOutputDatabase db = new HDFSOutputDatabase(fs);
		String name = new Path(outputFilenameBase, tier + ".hdb." + startRecord)
				.toString();
		System.out.println(name);
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
	public void putBytes(byte[] arr, int offset, int length) {
		throw new RuntimeException(
				"HadoopSplitDatabase requires a position argument to putBytes");
	}

	@Override
	public void putBytes(long loc, byte[] arr, int offset,
			int length) {
		throw new RuntimeException(
			"putBytes can only be called from beginWrite()'s return value");
	}

	@Override
	public long getLongRecordGroup(long loc) {
		Database db = getDatabaseFor((loc/conf.recordGroupByteLength)*conf.recordsPerGroup);
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
	public void seek(long position) {
		throw new RuntimeException(
				"HadoopSplitDatabase does not implement seek");
	}

	@Override
	public void getBytes(long recordGroupByteLocation, byte[] arr, int offset,
			int length) {
		while (length > 0) {
			long recordNumber = conf.recordsPerGroup-1+
			((recordGroupByteLocation+(conf.recordGroupByteLength-1))/
					conf.recordGroupByteLength)*conf.recordsPerGroup;
			long currentDbStart = getDatabaseKeyFor(recordNumber);
			long currentDbEnd = databaseEnd.get(currentDbStart);
			Database db = databaseTree.get(currentDbStart);
			int amtRead;
			long startRecordGroupByte = (currentDbStart/conf.recordsPerGroup)*conf.recordGroupByteLength;
			long endRecordGroupByte = ((currentDbEnd+(conf.recordsPerGroup-1))/conf.recordsPerGroup)*conf.recordGroupByteLength;
			if (length > endRecordGroupByte - recordGroupByteLocation) {
				amtRead = (int) (endRecordGroupByte - recordGroupByteLocation);
				if (amtRead == 0) {
					return; // Page.extendUp may call this with one record group past the end in the case that the end is exactly aligned.
				}
			} else {
				amtRead = length;
			}
			assert amtRead > 0;
			db.getBytes(recordGroupByteLocation-startRecordGroupByte, arr, offset, amtRead);
			length -= amtRead;
			recordGroupByteLocation += amtRead;
			offset += amtRead;
		}
	}

	@Override
	public void getBytes(byte[] arr, int offset, int length) {
		throw new RuntimeException(
				"HadoopSplitDatabase requires a position argument to getBytes");
	}

	@Override
	public LongIterator getLongRecordGroups(long recordGroupByteLocation, int numGroups) {
		throw new RuntimeException("getLongRecordGroups is not yet implemented.");
	}
}
