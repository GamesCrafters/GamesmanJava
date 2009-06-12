package edu.berkeley.gamesman.database;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

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

	byte[] rawRecord;

	long offset;

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
	public synchronized void putRecordGroup(long loc, RecordGroup value) {
		try {
			fd.seek(loc + offset);
			value.getState().outputUnsignedBytes(fd, conf.recordGroupByteLength);
		} catch (IOException e) {
			Util.fatalError("IO Error: " + e);
		}
	}

	@Override
	protected synchronized void initialize(String loc) {

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
