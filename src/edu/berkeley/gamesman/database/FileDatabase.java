package edu.berkeley.gamesman.database;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Iterator;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.RecordGroup;
import edu.berkeley.gamesman.util.Util;

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

	protected byte[] rawRecord;

	protected byte[] groups;

	protected int groupsLength;

	protected long offset;

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
	public synchronized RecordGroup getRecordGroup(long loc) {
		try {
			fd.seek(loc + offset);
			fd.read(rawRecord);
			RecordGroup v = new RecordGroup(conf, rawRecord);
			return v;
		} catch (IOException e) {
			Util.fatalError("IO Error: " + e);
		}
		return null;
	}

	@Override
	public Iterator<RecordGroup> getRecordGroups(long loc, int numGroups) {
		try {
			groupsLength = numGroups * conf.recordGroupByteLength;
			if (groups == null || groups.length < groupsLength)
				groups = new byte[groupsLength];
			fd.seek(loc + offset);
			fd.read(groups,0,groupsLength);
			RecordGroupByteIterator rgi = new RecordGroupByteIterator();
			return rgi;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void putRecordGroups(long loc, Iterator<RecordGroup> recordGroups,
			int numGroups) {
		try {
			groupsLength = numGroups * conf.recordGroupByteLength;
			if (groups == null || groups.length < groupsLength)
				groups = new byte[groupsLength];
			int onByte = 0;
			for (int i = 0; i < numGroups; i++) {
				recordGroups.next().getState().toUnsignedByteArray(groups,
						onByte, conf.recordGroupByteLength);
				onByte+=conf.recordGroupByteLength;
			}
			fd.seek(loc + offset);
			fd.write(groups,0,groupsLength);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected class RecordGroupByteIterator implements Iterator<RecordGroup> {
		int onByte = 0;

		public boolean hasNext() {
			return onByte < groupsLength;
		}

		public RecordGroup next() {
			for (int i = 0; i < rawRecord.length; i++)
				rawRecord[i] = groups[onByte++];
			return new RecordGroup(conf, rawRecord);
		}

		public void remove() {
			throw new UnsupportedOperationException("remove() not supported");
		}
	}

	@Override
	public synchronized void putRecordGroup(long loc, RecordGroup value) {
		try {
			fd.seek(loc + offset);
			value.getState()
					.outputUnsignedBytes(fd, conf.recordGroupByteLength);
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
			rawRecord = new byte[conf.recordGroupByteLength];
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
