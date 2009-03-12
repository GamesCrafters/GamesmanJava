package edu.berkeley.gamesman.util.bytes;

public interface ByteConsumer {

	public void put(int idx, byte b);
	public void putLong(int idx, long l);
}
