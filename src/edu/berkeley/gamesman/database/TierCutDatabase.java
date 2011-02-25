package edu.berkeley.gamesman.database;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.game.TierGame;

public class TierCutDatabase extends DatabaseWrapper {

	private long myNumRecords;
	TierGame myTierGame;
	static boolean deleteLastRow;
	public TierCutDatabase(Database db, String uri, Configuration config,
			boolean solve, long firstRecord, long numRecords) {
		super(db, uri, config, solve, firstRecord, numRecords);
		myNumRecords = numRecords;
	}
	
	public TierCutDatabase(Database db, String uri, Configuration config,
			boolean solve, long firstRecord, long numRecords, TierGame myGame) {
		super(db, uri, config, solve, firstRecord, numRecords);
		myNumRecords = numRecords;
		myTierGame = myGame;
		deleteLastRow = true;
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
		if(!shouldBeInDatabase(numBytes)) {
			//Get Primitive Value instead
		} else {
			//Get from database
		}
		return numBytes;
	}

	@Override
	protected synchronized int putBytes(DatabaseHandle dh, byte[] arr, int off,
			int maxLen, boolean edgesAreCorrect) {
		if (!edgesAreCorrect)
			return super.putBytes(dh, arr, off, maxLen, false);
		int numBytes = (int) Math.min(maxLen, dh.lastByteIndex - dh.location);
		// TODO Finish method
		if(!shouldBeInDatabase(numBytes)) {
			//Put in database as normal
		}
		return numBytes;
	}

	@Override
	public long getSize() {
		// TODO Auto-generated method stub
		return myNumRecords;
	}
	

	public static long getNumRecords(long firstRecord, long numRecords,
			TierGame game) {
		int numOfTiers = game.numberOfTiers();
		long lastTierHashes = game.numHashesForTier(numOfTiers-1);
		// TODO Auto-generated method stub
		if (deleteLastRow) {
			return numRecords + lastTierHashes;
		} else {
			return numRecords;
		}
	}
	
	private boolean shouldBeInDatabase(int curByte) {
		return curByte < myNumRecords;
	}

}
