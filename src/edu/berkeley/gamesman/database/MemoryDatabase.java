package edu.berkeley.gamesman.database;

import java.io.EOFException;
import java.io.IOException;

import edu.berkeley.gamesman.core.Configuration;

public class MemoryDatabase extends DatabaseWrapper {
	private final RecordRangeCache recordCache;
	private final boolean writing;

	public MemoryDatabase(Database db, Configuration config, long firstRecord,
			long numRecords, boolean reading, boolean writing) throws IOException {
		super(db, config, firstRecord, numRecords, reading, writing);
		recordCache = new RecordRangeCache(myLogic);
		long numBytes = myLogic.getNumBytes(numRecords);
		if (numBytes > Integer.MAX_VALUE)
			throw new ArrayIndexOutOfBoundsException(
					"MemoryDatabase cannot hold more than 2 GB of records");
		recordCache.setRange(firstRecord, (int) numRecords);
		if (!writing) {
			DatabaseHandle dbHandle = db.getHandle(true);
			recordCache.readFromDatabase(db, dbHandle,
					myLogic.getByteIndex(firstRecord), (int) numBytes);
		}
		this.writing = writing;
	}

	@Override
	protected int readBytes(DatabaseHandle dh, long location, byte[] array,
			int off, int len) {
		return recordCache.readBytes((int) location, array, off, len);
	}

	@Override
	protected int writeBytes(DatabaseHandle dh, long location, byte[] array,
			int off, int len) {
		return recordCache.writeBytes((int) location, array, off, len);
	}

	@Override
	public long readNextRecord(DatabaseHandle dh) throws EOFException,
			UnpreparedHandleException {
		if (dh.numBytes == DatabaseHandle.UNPREPARED)
			throw new UnpreparedHandleException(dh);
		else if (dh.numBytes == DatabaseHandle.KEEP_GOING
				|| dh.numBytes >= myLogic.recordBytes) {
			long record = readRecordFromByteIndex(dh, dh.location);
			dh.location += myLogic.recordBytes;
			return record;
		} else {
			throw new EOFException();
		}
	}

	@Override
	protected long readRecordFromByteIndex(DatabaseHandle dh, long byteIndex) {
		return recordCache.readRecordFromByteIndex(byteIndex);
	}

	@Override
	public void writeNextRecord(DatabaseHandle dh, long record)
			throws EOFException, UnpreparedHandleException {
		if (dh.numBytes == DatabaseHandle.UNPREPARED)
			throw new UnpreparedHandleException(dh);
		else if (dh.numBytes == DatabaseHandle.KEEP_GOING
				|| dh.numBytes >= myLogic.recordBytes) {
			writeRecordFromByteIndex(dh, dh.location, record);
			dh.location += myLogic.recordBytes;
		} else {
			throw new EOFException();
		}
	}

	@Override
	protected void writeRecordFromByteIndex(DatabaseHandle dh, long byteIndex,
			long record) {
		recordCache.writeRecordFromByteIndex(byteIndex, record);
	}

	@Override
	public void close() throws IOException {
		if (writing) {
			DatabaseHandle dh = db.getHandle(false);
			recordCache.writeBytesToDatabase(db, dh,
					recordCache.getFirstByteIndex(), recordCache.getNumBytes());
			db.close();
		}
	}
}
