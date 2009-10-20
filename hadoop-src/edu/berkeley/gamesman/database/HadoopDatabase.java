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

public class HadoopDatabase extends Database {

	protected Database previousTier; // CachedDatabase or HadoopSplitDatabase

	protected HDFSOutputDatabase output;

	protected Path myFile;

	protected FileSystem fs;

	public HadoopDatabase(FileSystem fs, Configuration conf) {
		this.conf = conf;
		this.fs = fs;
	}

	@Override
	public synchronized void flush() {
	}

	@Override
	public synchronized long getLongRecordGroup(long loc) {
		return previousTier.getLongRecordGroup(loc);
	}

	@Override
	public synchronized BigInteger getBigIntRecordGroup(long loc) {
		throw new RuntimeException("getBigIntRecordGroup unimplemented in hadoop database");
	}

	@Override
	public LongIterator getLongRecordGroups(long loc, int numGroups) {
		return previousTier.getLongRecordGroups(loc, numGroups);
	}

	@Override
	public synchronized void getBytes(long loc, byte[]arr, int offset, int length) {
		previousTier.getBytes(loc, arr, offset, length);
	}

	@Override
	public synchronized void getBytes(byte[]arr, int offset, int length) {
		throw new RuntimeException("HadoopDatabase requiiers a position argument to getBytes");
	}

	@Override
	public synchronized void seek(long position) {
		assert(position == output.getPosition());
	}

	@Override
	public synchronized void putBytes(byte[]arr, int offset, int length) {
		output.putBytes(arr, offset, length);
	}

	@Override
	public synchronized void putRecordGroups(long loc,
			LongIterator recordGroups, int numGroups) {
		output.putRecordGroups(loc, recordGroups, numGroups);
	}

	@Override
	public synchronized void putRecordGroups(long loc,
			Iterator<BigInteger> recordGroups, int numGroups) {
		throw new RuntimeException("putRecordGroups (bigintiter) unimplemented in hadoop database");
	}

	@Override
	public synchronized void putRecordGroup(long loc, long value) {
		output.putRecordGroup(loc, value);
	}

	@Override
	public synchronized void putRecordGroup(long loc, BigInteger value) {
		throw new RuntimeException("putRecordGroup (bigint) unimplemented in hadoop database");
	}

	@Override
	public synchronized void initialize(String loc) {
		int prevousTier = 100;
		String[] filenames = loc.split(">");
		String inputLoc = filenames[0];
		String outputLoc = filenames[1];
		previousTier = new HadoopSplitDatabase(fs, conf);
		previousTier.initialize(inputLoc);
		output = new HDFSOutputDatabase(fs, conf);
		output.initialize(outputLoc);
	}

	@Override
	public void close() {
		previousTier.close();
		output.close();
	}
}
