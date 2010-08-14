package edu.berkeley.gamesman.database;

import edu.berkeley.gamesman.util.qll.Factory;
import edu.berkeley.gamesman.util.qll.Pool;

/**
 * Each thread reading from a database should use a separate instance of this
 * class. This allows databases to be accessed in parallel without having to
 * synchronize every access
 * 
 * @author dnspies
 */
public class DatabaseHandle {
	private final Pool<byte[]> recordGroupBytes;
	protected long location;
	protected long byteIndex;
	protected int firstNum;
	protected long lastByteIndex;
	protected int lastNum;
	protected byte[] firstGroup;
	protected byte[] lastGroup;
	protected DatabaseHandle innerHandle;

	/**
	 * Creates a new database handle with a pool for creating byte arrays to
	 * read in and out of record groups
	 * 
	 * @param recordGroupByteLength
	 *            The number of bytes in a record group
	 */
	protected DatabaseHandle(final int recordGroupByteLength) {
		recordGroupBytes = new Pool<byte[]>(new Factory<byte[]>() {
			public byte[] newObject() {
				return new byte[recordGroupByteLength];
			}

			public void reset(byte[] t) {
			}
		});
	}

	/**
	 * Creates a new database handle with the provided byte pool
	 * 
	 * @param bytePool
	 *            A pool for creating byte arrays to read in and out of record
	 *            groups
	 */
	protected DatabaseHandle(Pool<byte[]> bytePool) {
		recordGroupBytes = bytePool;
	}

	/**
	 * @return A byte array of length recordGroupByteLength
	 */
	public byte[] getRecordGroupBytes() {
		return recordGroupBytes.get();
	}

	/**
	 * Returns a byte array to the pool
	 * 
	 * @param b
	 *            A byte array of length recordGroupByteLength
	 */
	public void releaseBytes(byte[] b) {
		recordGroupBytes.release(b);
	}
}
