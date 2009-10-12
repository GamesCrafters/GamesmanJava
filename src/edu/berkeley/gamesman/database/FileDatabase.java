package edu.berkeley.gamesman.database;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Iterator;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.RecordGroup;
import edu.berkeley.gamesman.util.LongIterator;
import edu.berkeley.gamesman.util.Util;
import edu.berkeley.gamesman.util.biginteger.BigInteger;

/**
 * The FileDatabase is a database designed to write directly to a local file.
 * The file format is not well defined at the moment, perhaps this should be
 * changed later.
 * 
 * @author Steven Schlansker
 */
public class FileDatabase extends Database {

	protected File myFile;

	protected RandomAccessFile fd;

	protected int groupsLength;

	protected long offset;

	protected byte[] rawRecord;

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
			fd.getFD().sync();
			fd.getChannel().force(true);
		} catch (IOException e) {
			Util.fatalError("Error while writing to database: " + e);
		}
	}

	@Override
	public synchronized long getLongRecordGroup(long loc) {
		try {
			fd.seek(loc + offset);
			long v = RecordGroup.longRecordGroup(conf, fd);
			return v;
		} catch (IOException e) {
			Util.fatalError("IO Error: " + e);
		}
		return 0L;
	}

	@Override
	public synchronized BigInteger getBigIntRecordGroup(long loc) {
		try {
			fd.seek(loc + offset);
			if (rawRecord == null)
				rawRecord = new byte[conf.recordGroupByteLength];
			fd.read(rawRecord);
			BigInteger v = RecordGroup.bigIntRecordGroup(conf, rawRecord);
			return v;
		} catch (IOException e) {
			Util.fatalError("IO Error: " + e);
		}
		return null;
	}

	@Override
	public LongIterator getLongRecordGroups(long loc, int numGroups) {
		try {
			fd.seek(loc + offset);
			return new LongRecordGroupByteIterator();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public synchronized void putRecordGroups(long loc,
			LongIterator recordGroups, int numGroups) {
		try {
			fd.seek(loc + offset);
			for (int i = 0; i < numGroups; i++)
				RecordGroup.outputUnsignedBytes(conf, recordGroups.next(), fd);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public synchronized void putRecordGroups(long loc,
			Iterator<BigInteger> recordGroups, int numGroups) {
		try {
			fd.seek(loc + offset);
			for (int i = 0; i < numGroups; i++)
				RecordGroup.outputUnsignedBytes(conf, recordGroups.next(), fd);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected class LongRecordGroupByteIterator implements LongIterator {
		int onByte = 0;

		public boolean hasNext() {
			return onByte < groupsLength;
		}

		public long next() {
			try {
				return RecordGroup.longRecordGroup(conf, fd);
			} catch (IOException e) {
				Util.fatalError("IO Error", e);
				return 0L;
			}
		}
	}

	protected class BigIntRecordGroupByteIterator implements
			Iterator<BigInteger> {
		int onByte = 0;

		public boolean hasNext() {
			return onByte < groupsLength;
		}

		public BigInteger next() {
			try {
				if (rawRecord == null)
					rawRecord = new byte[conf.recordGroupByteLength];
				fd.read(rawRecord);
				return RecordGroup.bigIntRecordGroup(conf, rawRecord);
			} catch (IOException e) {
				Util.fatalError("IO Error", e);
				return null;
			}
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public synchronized void putRecordGroup(long loc, long value) {
		try {
			fd.seek(loc + offset);
			RecordGroup.outputUnsignedBytes(conf, value, fd);
		} catch (IOException e) {
			Util.fatalError("IO Error: " + e);
		}
	}

	@Override
	public synchronized void putRecordGroup(long loc, BigInteger value) {
		try {
			fd.seek(loc + offset);
			RecordGroup.outputUnsignedBytes(conf, value, fd);
		} catch (IOException e) {
			Util.fatalError("IO Error: " + e);
		}
	}

	@Override
	public synchronized void initialize(String loc) {
		boolean previouslyExisted;
		try {
			myFile = new File(loc);
			previouslyExisted = myFile.exists();
			fd = new RandomAccessFile(myFile, "rw");
			if (previouslyExisted) {
				int headerLen = fd.readInt();
				byte[] header = new byte[headerLen];
				fd.readFully(header);
				if (conf == null) {
					conf = Configuration.load(header);
				}
			} else {
				if (conf == null)
					Util
							.fatalError("You must specify a configuration if the database is to be created");
				byte[] b = conf.store();
				fd.writeInt(b.length);
				fd.write(b);
			}
			offset = fd.getFilePointer();
			fd.setLength(offset + getByteSize());
		} catch (IOException e) {
			e.printStackTrace();
			Util.fatalError(e.toString());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			Util.fatalError(e.toString());
		}
	}
}
