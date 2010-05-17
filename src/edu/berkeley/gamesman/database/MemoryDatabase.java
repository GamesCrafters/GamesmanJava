package edu.berkeley.gamesman.database;

import java.math.BigInteger;

import edu.berkeley.gamesman.core.RecordGroup;

/**
 * Stores the entire database as an array of bytes
 * 
 * @author dnspies
 */
public class MemoryDatabase extends Database {
	/* Class Variables */

	protected byte[] memoryStorage; // byte array to store the data

	protected boolean readingOnly;

	@Override
	public void initialize(String location, boolean solve) {
		memoryStorage = new byte[(int) getByteSize()];
	}

	@Override
	public void close() {
	}

	@Override
	public void getBytes(DatabaseHandle dh, long loc, byte[] arr, int off, int len) {
		for (int i = 0; i < len; i++)
			arr[off++] = memoryStorage[(int) (loc++ - firstByte())];
	}

	@Override
	public void putBytes(DatabaseHandle dh, long loc, byte[] arr, int off, int len) {
		for (int i = 0; i < len; i++)
			memoryStorage[(int) (loc++ - firstByte())] = arr[off++];
	}

	@Override
	public long getLongRecordGroup(DatabaseHandle dh, long loc) {
		return RecordGroup.longRecordGroup(conf, memoryStorage,
				(int) (loc - firstByte()));
	}

	@Override
	public BigInteger getBigIntRecordGroup(DatabaseHandle dh, long loc) {
		return RecordGroup.bigIntRecordGroup(conf, memoryStorage,
				(int) (loc - firstByte()));
	}

	@Override
	public void putRecordGroup(DatabaseHandle dh, long loc, long rg) {
		RecordGroup.toUnsignedByteArray(conf, rg, memoryStorage,
				(int) (loc - firstByte()));
	}

	@Override
	public void putRecordGroup(DatabaseHandle dh, long loc, BigInteger rg) {
		RecordGroup.toUnsignedByteArray(conf, rg, memoryStorage,
				(int) (loc - firstByte()));
	}
}
