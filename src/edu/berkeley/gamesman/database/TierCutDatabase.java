package edu.berkeley.gamesman.database;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.game.TierGame;

public class TierCutDatabase extends DatabaseWrapper {

	public TierCutDatabase(Database db, String uri, Configuration config,
			boolean solve, long firstRecord, long numRecords) {
		super(db, uri, config, solve, firstRecord, numRecords);
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void putBytes(DatabaseHandle dh, long loc, byte[] arr, int off,
			int len) {
		putRecordsAsBytes(dh, loc, 0, arr, off, len, 0, true);
	}

	@Override
	protected void getBytes(DatabaseHandle dh, long loc, byte[] arr, int off,
			int len) {
		getRecordsAsBytes(dh, loc, 0, arr, off, len, 0, true);
	}

	@Override
	protected synchronized void prepareRange(DatabaseHandle dh, long byteIndex,
			int firstNum, long numBytes, int lastNum) {
		super.prepareRange(dh, byteIndex, firstNum, numBytes, lastNum);
		// TODO Finish method
	}

	@Override
	protected synchronized int getBytes(DatabaseHandle dh, byte[] arr, int off,
			int maxLen, boolean overwriteEdgesOk) {
		if (!overwriteEdgesOk)
			return super.getBytes(dh, arr, off, maxLen, false);
		int numBytes = (int) Math.min(maxLen, dh.lastByteIndex - dh.location);
		// TODO Finish method
		return numBytes;
	}

	@Override
	protected synchronized int putBytes(DatabaseHandle dh, byte[] arr, int off,
			int maxLen, boolean edgesAreCorrect) {
		if (!edgesAreCorrect)
			return super.putBytes(dh, arr, off, maxLen, false);
		int numBytes = (int) Math.min(maxLen, dh.lastByteIndex - dh.location);
		// TODO Finish method
		return numBytes;
	}

	@Override
	public long getSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	public static long getNumRecords(long firstRecord, long numRecords,
			TierGame game) {
		// TODO Auto-generated method stub
		return 0;
	}

}
