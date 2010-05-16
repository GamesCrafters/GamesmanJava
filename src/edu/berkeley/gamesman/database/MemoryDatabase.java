package edu.berkeley.gamesman.database;

import java.math.BigInteger;

import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.RecordGroup;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Util;

/**
 * Stores the entire database as an array of bytes
 * 
 * @author dnspies
 */
public class MemoryDatabase extends Database {
	/* Class Variables */

	protected byte[] memoryStorage; // byte array to store the data

	protected boolean readingOnly;

	private int nextPlace = 0;

	@Override
	public void initialize(String location, boolean solve) {
		memoryStorage = new byte[(int) getByteSize()];
	}

	@Override
	public void flush() {
		assert Util.debug(DebugFacility.DATABASE,
				"Flushing Memory DataBase. Does Nothing.");
	}

	@Override
	public void close() {
	}

	@Override
	public void getBytes(long loc, byte[] arr, int off, int len) {
		for (int i = 0; i < len; i++)
			arr[off++] = memoryStorage[(int) (loc++ - firstByte())];
	}

	@Override
	public void getBytes(byte[] arr, int off, int len) {
		for (int i = 0; i < len; i++)
			arr[off++] = memoryStorage[nextPlace++];
	}

	@Override
	public void putBytes(long loc, byte[] arr, int off, int len) {
		for (int i = 0; i < len; i++)
			memoryStorage[(int) (loc++ - firstByte())] = arr[off++];
	}

	@Override
	public void putBytes(byte[] arr, int off, int len) {
		for (int i = 0; i < len; i++)
			memoryStorage[nextPlace++] = arr[off++];
	}

	@Override
	public void seek(long loc) {
		nextPlace = (int) (loc - firstByte());
	}

	@Override
	public long getLongRecordGroup(long loc) {
		return RecordGroup.longRecordGroup(conf, memoryStorage,
				(int) (loc - firstByte()));
	}

	@Override
	public BigInteger getBigIntRecordGroup(long loc) {
		return RecordGroup.bigIntRecordGroup(conf, memoryStorage,
				(int) (loc - firstByte()));
	}

	@Override
	public void putRecordGroup(long loc, long rg) {
		RecordGroup.toUnsignedByteArray(conf, rg, memoryStorage,
				(int) (loc - firstByte()));
	}

	@Override
	public void putRecordGroup(long loc, BigInteger rg) {
		RecordGroup.toUnsignedByteArray(conf, rg, memoryStorage,
				(int) (loc - firstByte()));
	}
}
