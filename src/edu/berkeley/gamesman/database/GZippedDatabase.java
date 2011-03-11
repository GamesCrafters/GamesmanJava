package edu.berkeley.gamesman.database;

import java.io.IOException;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.database.util.GZippedDatabaseInputStream;
import edu.berkeley.gamesman.database.util.GZippedDatabaseOutputStream;
import edu.berkeley.gamesman.util.Util;
import edu.berkeley.gamesman.util.ZipChunkInputStream;
import edu.berkeley.gamesman.util.ZipChunkOutputStream;

public abstract class GZippedDatabase extends Database {
	private long currentByteIndex;
	private long remaining;
	private final int numEntries;
	private final long entrySize;
	private final long firstByteIndex;
	private final long numBytes;
	private final int tableOffset;
	private final ZipChunkOutputStream zcos;
	private ZipChunkInputStream zcis;
	private final long[] entryTable;
	private final GZippedDatabaseInputStream reader;
	private final GZippedDatabaseOutputStream writer;
	private int currentEntry;
	private long filePos;

	public GZippedDatabase(GZippedDatabaseInputStream reader,
			GZippedDatabaseOutputStream writer, String uri, Configuration conf,
			long firstRecordIndex, long numRecords, boolean reading,
			boolean writing) throws IOException {
		super(conf, firstRecordIndex, numRecords, reading, writing);
		this.reader = reader;
		this.writer = writer;
		firstByteIndex = myLogic.getByteIndex(firstRecordIndex);
		numBytes = myLogic.getNumBytes(numRecords);
		entrySize = conf.getNumBytes("entry.bytes", 1 << 16);
		numEntries = (int) (numBytes / entrySize + 1);
		entryTable = new long[numEntries];
		if (writing) {
			remaining = entrySize;
			tableOffset = this.writeHeader(writer);
			assert writer.getFilePointer() == tableOffset;
			for (int i = 0; i < numEntries; i++)
				writer.writeLong(0);
			filePos = tableOffset + 8 * numEntries;
			assert filePos == writer.getFilePointer();
			currentEntry = 0;
			entryTable[0] = filePos;
			zcos = new ZipChunkOutputStream(writer);
			currentByteIndex = firstByteIndex;
		} else {
			tableOffset = skipHeader(reader);
			for (int i = 0; i < numEntries; i++)
				entryTable[i] = reader.readLong();
			zcis = new ZipChunkInputStream(reader);
			zcos = null;
			currentByteIndex = -1L;
		}
	}

	@Override
	protected synchronized int readBytes(DatabaseHandle dh, long location,
			byte[] array, int off, int len) throws IOException {
		if (writing)
			return len;
		if (location != currentByteIndex) {
			int readEntry = (int) ((location - firstByteIndex) / entrySize);
			long entryStartByteIndex = firstByteIndex + readEntry * entrySize;
			reader.seek(entryTable[readEntry]);
			zcis = new ZipChunkInputStream(reader);
			Util.skipFully(zcis, location - entryStartByteIndex);
			currentByteIndex = location;
		}
		int bytesRead = zcis.read(array, off, len);
		currentByteIndex += bytesRead;
		return bytesRead;
	}

	@Override
	protected synchronized int writeBytes(DatabaseHandle dh, long location,
			byte[] array, int off, int len) throws IOException {
		if (location != currentByteIndex)
			throw new UnsupportedOperationException(
					"Can only write sequentially");
		int atTime = (int) Math.min(len, remaining);
		zcos.write(array, off, atTime);
		len -= atTime;
		remaining -= atTime;
		currentByteIndex += atTime;
		if (remaining == 0) {
			filePos += zcos.nextChunk();
			zcos.flush();
			assert filePos == writer.getFilePointer();
			entryTable[++currentEntry] = filePos;
			remaining = entrySize;
		}
		return atTime;
	}

	@Override
	public void close() throws IOException {
		if (writing) {
			zcos.finish();
			writer.seek(tableOffset);
			for (int i = 0; i < numEntries; i++) {
				writer.writeLong(entryTable[i]);
			}
			writer.close();
		} else
			reader.close();
	}
}
