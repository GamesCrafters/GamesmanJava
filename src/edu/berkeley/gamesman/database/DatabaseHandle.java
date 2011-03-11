package edu.berkeley.gamesman.database;

/**
 * Each thread reading from a database should use a separate instance of this
 * class. This allows databases to be accessed in parallel without having to
 * synchronize every access
 * 
 * @author dnspies
 */
public class DatabaseHandle {
	public static final int KEEP_GOING = -1;
	public static final int UNPREPARED = -2;
	protected long location, remainingBytes;
	protected long firstByteIndex, numBytes = UNPREPARED;
	protected final byte[] currentRecord;
	protected final boolean reading;

	public DatabaseHandle(int numBytes, boolean reading) {
		currentRecord = new byte[numBytes];
		this.reading = reading;
	}
}
