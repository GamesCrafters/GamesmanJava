package edu.berkeley.gamesman.database;

public class DatabaseHandle {
	private byte[] recordGroupBytes = null;

	byte[] getRecordGroupBytes(int minLength) {
		if (recordGroupBytes == null || recordGroupBytes.length < minLength)
			recordGroupBytes = new byte[minLength];
		return recordGroupBytes;
	}
}
