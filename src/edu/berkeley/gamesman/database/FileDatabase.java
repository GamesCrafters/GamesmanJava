package edu.berkeley.gamesman.database;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.hasher.TieredHasher;
import edu.berkeley.gamesman.util.Util;

/**
 * The FileDatabase is a database designed to write directly to a local file.
 * The file format is not well defined at the moment, perhaps this should be
 * changed later.
 * 
 * @author Steven Schlansker
 */
public final class FileDatabase extends Database {

	protected File myFile;

	protected RandomAccessFile fd;

	protected int groupsLength;

	protected long offset;

	private long numBytes = -1;

	private long firstByte = 0;

	@Override
	public void close() {
		try {
			fd.close();
		} catch (IOException e) {
			Util.warn("Error while closing input stream for database: " + e);
		}
	}

	@Override
	public void flush() {
		// try {
		// fd.getFD().sync();
		// fd.getChannel().force(true);
		// } catch (IOException e) {
		// Util.fatalError("Error while writing to database: " + e);
		// }
	}

	public void seek(long loc) {
		try {
			fd.seek(loc + offset);
		} catch (IOException e) {
			Util.fatalError("IO Error", e);
		}
	}

	@Override
	public void getBytes(byte[] arr, int off, int len) {
		try {
			fd.read(arr, off, len);
		} catch (IOException e) {
			Util.fatalError("IO Error", e);
		}
	}

	@Override
	public void putBytes(byte[] arr, int off, int len) {
		try {
			fd.write(arr, off, len);
		} catch (IOException e) {
			Util.fatalError("IO Error", e);
		}
	}

	@Override
	public void initialize(String loc, boolean solve) {
		try {
			myFile = new File(loc);
			fd = new RandomAccessFile(myFile, "rw");
			if (solve) {
				if (conf == null)
					Util
							.fatalError("You must specify a configuration if the database is to be created");
				if (numBytes >= 0)
					fd.writeInt(0);
				else {
					byte[] b = conf.store();
					fd.writeInt(b.length);
					fd.write(b);
				}
			} else {
				int headerLen = fd.readInt();
				byte[] header = new byte[headerLen];
				fd.readFully(header);
				if (conf == null) {
					conf = Configuration.load(header);
				}
			}
			offset = fd.getFilePointer();
			fd.setLength(offset + getByteSize());
			offset -= firstByte;
		} catch (IOException e) {
			e.printStackTrace();
			Util.fatalError(e.toString());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			Util.fatalError(e.toString());
		}
	}

	@Override
	public long getByteSize() {
		if (numBytes < 0)
			return super.getByteSize();
		else {
			return numBytes;
		}
	}

	/**
	 * If this database only covers a single tier of a tiered game, call this
	 * method before calling initialize
	 * 
	 * @param tier
	 *            The tier
	 */
	public void setSingleTier(Configuration conf, int tier) {
		TieredHasher<?> hasher = (TieredHasher<?>) conf.getHasher();
		setRange(conf, hasher.hashOffsetForTier(tier), hasher
				.numHashesForTier(tier));
	}

	public void setRange(Configuration conf, long firstRecord, long numRecords) {
		if (this.conf != null)
			Util.fatalError("This must be called before initialize");
		long endRecord = firstRecord + numRecords;
		firstByte = firstRecord / conf.recordsPerGroup
				* conf.recordGroupByteLength;
		numBytes = (endRecord + conf.recordsPerGroup - 1)
				/ conf.recordsPerGroup * conf.recordGroupByteLength - firstByte;
	}
}
