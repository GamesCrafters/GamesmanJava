package edu.berkeley.gamesman.database;

import java.io.IOException;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.database.util.SeekableInputStream;
import edu.berkeley.gamesman.database.util.SeekableOutputStream;

/**
 * The most basic database. Simply writes the bytes out to a file
 * 
 * @author dnspies
 */
public abstract class SeekableDatabase extends Database {

	public SeekableDatabase(SeekableInputStream in, SeekableOutputStream out,
			String uri, Configuration conf, long firstRecordIndex,
			long numRecords, boolean reading, boolean writing)
			throws IOException {
		this(in, out, uri, conf, firstRecordIndex, numRecords, reading,
				writing, true);
	}

	protected SeekableDatabase(SeekableInputStream in,
			SeekableOutputStream out, String uri, Configuration conf,
			long firstRecordIndex, long numRecords, boolean reading,
			boolean writing, boolean overwrite) throws IOException {
		super(conf, firstRecordIndex, numRecords, reading, writing);
		this.in = in;
		this.out = out;
		if (overwrite ? writing : (!reading)) {
			headerLen = writeHeader(out);
		} else {
			headerLen = skipHeader(in);
		}
		firstByteIndex = myLogic.getByteIndex(firstRecordIndex);
	}

	private SeekableDatabase(SeekableInputStream in, SeekableOutputStream out,
			DatabaseArgs args) throws IOException {
		super(args.conf, args.firstRecordIndex, args.numRecords, true, true);
		this.in = in;
		this.out = out;
		headerLen = skipHeader(in);
		firstByteIndex = myLogic.getByteIndex(firstRecordIndex);
	}

	private final SeekableOutputStream out;
	private final SeekableInputStream in;

	private final long headerLen;

	private final long firstByteIndex;

	private long lastByteIndex = -1L;

	@Override
	protected synchronized int readBytes(DatabaseHandle dh, long location,
			byte[] array, int off, int len) throws IOException {
		if (lastByteIndex != location) {
			in.seek(location - firstByteIndex + headerLen);
		}
		int bytesRead = in.read(array, off, len);
		lastByteIndex = location + bytesRead;
		return bytesRead;
	}

	@Override
	protected synchronized int writeBytes(DatabaseHandle dh, long location,
			byte[] array, int off, int len) throws IOException {
		if (lastByteIndex != location) {
			out.seek(location - firstByteIndex + headerLen);
		}
		out.write(array, off, len);
		lastByteIndex = location + len;
		return len;
	}

	@Override
	public synchronized void close() throws IOException {
		if (out != null)
			out.close();
		if (in != null)
			in.close();
	}
}
