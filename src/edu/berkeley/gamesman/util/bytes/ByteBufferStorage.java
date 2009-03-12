package edu.berkeley.gamesman.util.bytes;

import java.nio.ByteBuffer;

public final class ByteBufferStorage implements ByteStorage {
	
	private final ByteBuffer buf;

	public ByteBufferStorage(ByteBuffer buf) {
		this.buf = buf;
	}

	public void put(int idx, byte b) {
		buf.put(idx, b);
	}

	public void putLong(int idx, long l) {
		buf.putLong(idx,l);
	}

	public byte get(int index) {
		return buf.get(index);
	}

	public long getLong(int idx) {
		return buf.getLong(idx);
	}

}
