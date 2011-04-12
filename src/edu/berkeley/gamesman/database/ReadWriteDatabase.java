package edu.berkeley.gamesman.database;

import java.io.IOException;

import edu.berkeley.gamesman.core.Configuration;

public class ReadWriteDatabase extends Database {
	private final Database readDb;
	private final Database writeDb;

	public ReadWriteDatabase(Database readDb, Database writeDb,
			Configuration conf, long firstRecordIndex, long numRecords) {
		super(conf, firstRecordIndex, numRecords, true, true);
		this.readDb = readDb;
		this.writeDb = writeDb;
	}

	public ReadWriteDatabase(Database readDb, Database writeDb,
			Configuration conf) {
		this(readDb, writeDb, conf, 0L, conf.getGame().numHashes());
	}

	@Override
	public DatabaseHandle getHandle(boolean reading) {
		if (reading) {
			if (readDb == null)
				return null;
			else
				return readDb.getHandle(true);
		} else {
			if (writeDb == null)
				return null;
			else
				return writeDb.getHandle(false);
		}
	}

	@Override
	protected void lowerPrepareReadRange(DatabaseHandle dh,
			long firstByteIndex, long numBytes) throws IOException {
		readDb.lowerPrepareReadRange(dh, firstByteIndex, numBytes);
	}

	@Override
	protected void lowerPrepareWriteRange(DatabaseHandle dh,
			long firstByteIndex, long numBytes) throws IOException {
		writeDb.lowerPrepareWriteRange(dh, firstByteIndex, numBytes);
	}

	@Override
	protected int lowerReadBytes(DatabaseHandle dh, byte[] array, int off,
			int len) throws IOException {
		return readDb.lowerReadBytes(dh, array, off, len);
	}

	@Override
	protected int readBytes(DatabaseHandle dh, long location, byte[] array,
			int off, int len) throws IOException {
		return readDb.readBytes(dh, location, array, off, len);
	}

	@Override
	protected int lowerWriteBytes(DatabaseHandle dh, byte[] array, int off,
			int len) throws IOException {
		return writeDb.lowerWriteBytes(dh, array, off, len);
	}

	@Override
	protected int writeBytes(DatabaseHandle dh, long location, byte[] array,
			int off, int len) throws IOException {
		return writeDb.writeBytes(dh, location, array, off, len);
	}

	@Override
	public long readRecord(DatabaseHandle dh, long recordIndex)
			throws IOException {
		return readDb.readRecord(dh, recordIndex);
	}

	@Override
	protected long readRecordFromByteIndex(DatabaseHandle dh, long byteIndex)
			throws IOException {
		return readDb.readRecordFromByteIndex(dh, byteIndex);
	}

	@Override
	public void writeRecord(DatabaseHandle dh, long recordIndex, long record)
			throws IOException {
		writeDb.writeRecord(dh, recordIndex, record);
	}

	@Override
	protected void writeRecordFromByteIndex(DatabaseHandle dh, long byteIndex,
			long record) throws IOException {
		writeDb.writeRecordFromByteIndex(dh, byteIndex, record);
	}

	@Override
	public void prepareReadRecordRange(DatabaseHandle dh, long recordIndex,
			long numRecords) throws IOException {
		readDb.prepareReadRecordRange(dh, recordIndex, numRecords);
	}

	@Override
	public void prepareWriteRecordRange(DatabaseHandle dh, long recordIndex,
			long numRecords) throws IOException {
		writeDb.prepareWriteRecordRange(dh, recordIndex, numRecords);
	}

	@Override
	public long readNextRecord(DatabaseHandle dh) throws IOException {
		return readDb.readNextRecord(dh);
	}

	@Override
	public void writeNextRecord(DatabaseHandle dh, long record)
			throws IOException {
		writeDb.fill(dh, record);
	}

	@Override
	public void close() throws IOException {
		readDb.close();
		writeDb.close();
	}

	@Override
	protected int writeBytes(DatabaseHandle dh, byte[] array, int off,
			int maxLen) throws IOException {
		return writeDb.writeBytes(dh, array, off, maxLen);
	}

	@Override
	protected int readBytes(DatabaseHandle dh, byte[] array, int off, int maxLen)
			throws IOException {
		return readDb.readBytes(dh, array, off, maxLen);
	}

	@Override
	public void prepareWriteRange(DatabaseHandle dh, long firstByteIndex,
			long numBytes) throws IOException {
		writeDb.prepareWriteRange(dh, firstByteIndex, numBytes);
	}

	@Override
	public void prepareReadRange(DatabaseHandle dh, long firstByteIndex,
			long numBytes) throws IOException {
		readDb.prepareReadRange(dh, firstByteIndex, numBytes);
	}
}
