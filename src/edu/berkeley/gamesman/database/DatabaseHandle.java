package edu.berkeley.gamesman.database;

import edu.berkeley.gamesman.util.qll.Factory;
import edu.berkeley.gamesman.util.qll.Pool;

/**
 * Each thread reading from a database should use a separate instance of this
 * class
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

	public DatabaseHandle(final int recordGroupByteLength) {
		recordGroupBytes = new Pool<byte[]>(new Factory<byte[]>() {
			public byte[] newObject() {
				return new byte[recordGroupByteLength];
			}

			public void reset(byte[] t) {
			}
		});
	}

	protected DatabaseHandle(Pool<byte[]> bytePool) {
		recordGroupBytes = bytePool;
	}

	public byte[] getRecordGroupBytes() {
		return recordGroupBytes.get();
	}

	public void releaseBytes(byte[] b) {
		recordGroupBytes.release(b);
	}
}
