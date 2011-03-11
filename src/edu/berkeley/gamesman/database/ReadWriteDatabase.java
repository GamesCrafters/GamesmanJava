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

	protected void lowerPrepareReadRange(DatabaseHandle dh,
			long firstByteIndex, long numBytes) throws IOException {
		readDb.lowerPrepareReadRange(dh, firstByteIndex, numBytes);
	}

	protected void lowerPrepareWriteRange(DatabaseHandle dh,
			long firstByteIndex, long numBytes) throws IOException {
		writeDb.lowerPrepareWriteRange(dh, firstByteIndex, numBytes);
	}

	protected int lowerReadBytes(DatabaseHandle dh, byte[] array, int off,
			int len) throws IOException {
		return readDb.lowerReadBytes(dh, array, off, len);
	}

	protected int readBytes(DatabaseHandle dh, long location, byte[] array,
			int off, int len) throws IOException {
		return readDb.readBytes(dh, location, array, off, len);
	}

	protected int lowerWriteBytes(DatabaseHandle dh, byte[] array, int off,
			int len) throws IOException {
		return writeDb.lowerWriteBytes(dh, array, off, len);
	}

	protected int writeBytes(DatabaseHandle dh, long location, byte[] array,
			int off, int len) throws IOException {
		return writeDb.writeBytes(dh, location, array, off, len);
	}

	public long readRecord(DatabaseHandle dh, long recordIndex)
			throws IOException {
		return readDb.readRecord(dh, recordIndex);
	}

	protected long readRecordFromByteIndex(DatabaseHandle dh, long byteIndex)
			throws IOException {
		return readDb.readRecordFromByteIndex(dh, byteIndex);
	}

	public void writeRecord(DatabaseHandle dh, long recordIndex, long record)
			throws IOException {
		writeDb.writeRecord(dh, recordIndex, record);
	}

	protected void writeRecordFromByteIndex(DatabaseHandle dh, long byteIndex,
			long record) throws IOException {
		writeDb.writeRecordFromByteIndex(dh, byteIndex, record);
	}

	public void lowerSeek(DatabaseHandle dh, long recordIndex)
			throws IOException {
		if (dh.reading)
			readDb.lowerSeek(dh, recordIndex);
		else
			writeDb.lowerSeek(dh, recordIndex);
	}

	public long readNextRecord(DatabaseHandle dh) throws IOException {
		return readDb.readNextRecord(dh);
	}

	public void writeNextRecord(DatabaseHandle dh, long record)
			throws IOException {
		writeDb.fill(dh, record);
	}

	public void fill(DatabaseHandle dh, long record) throws IOException {
		writeDb.fill(dh, record);
	}

	@Override
	public void flush() throws IOException {
		writeDb.flush();
	}

	@Override
	public void close() throws IOException {
		readDb.close();
		writeDb.close();
	}
}
