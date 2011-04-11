package edu.berkeley.gamesman.database;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.game.TierGame;
import edu.berkeley.gamesman.game.util.TierState;
import edu.berkeley.gamesman.util.Pair;

public final class TierCutDatabase extends Database {

	private final TierGame myTierGame;
	private final boolean deleteLastRow;
	private final int numTiersCut;
	private SplitLocalDatabase inner;

	public TierCutDatabase(Configuration conf, long firstRecordIndex,
			long numRecords, boolean reading, boolean writing)
			throws IOException {
		super(conf, firstRecordIndex, numRecords, reading, writing);
		myTierGame = (TierGame) conf.getGame();
		deleteLastRow = true;
		numTiersCut = conf.getInteger("gamesman.database.tiers.cut", 2);
		String dbClass = SplitLocalDatabase.class.getName();
		String uri = conf.getProperty("gamesman.db.uri");
		DataInputStream dis;
		inner = (SplitLocalDatabase) Database.openDatabase(dbClass, uri, conf,
				firstRecordIndex, numRecords, reading, writing);
	}

	@Override
	public long readRecord(DatabaseHandle dh, long recordIndex)
			throws IOException {
		int tier = myTierGame.hashToTier(recordIndex);
		if (shouldBeInDatabase(tier)) {
			//return super.readRecord(dh, recordIndex);
			return inner.readRecord(dh, recordIndex);
		} else {
			if (inLastRow(recordIndex)) {
				TierState pos = myTierGame.hashToState(recordIndex);
				myTierGame.setState(pos);
				Record myRecord = myTierGame.newRecord();
				myRecord.value = myTierGame.primitiveValue();
				myRecord.remoteness = 0;
				long val = myTierGame.recordToLong(myRecord);
				return val;
			} else {
				return missingTierSolve(dh, recordIndex);
			}

		}
	}

	private long missingTierSolve(DatabaseHandle dh, long recordIndex)
			throws IOException {
		long hash = recordIndex;
		TierState pos = myTierGame.hashToState(hash);
		Collection<Pair<String, TierState>> moves = myTierGame.validMoves(pos);
		Record myRecord = null;
		for (Pair<String, TierState> move : moves) {
			TierState newState = move.cdr;
			long newHash = myTierGame.stateToHash(newState);
			Record curRecord = myTierGame.newRecord();
			myTierGame.longToRecord(newState, readRecord(dh, newHash),
					curRecord);
			curRecord.previousPosition();
			if (myRecord == null || curRecord.compareTo(myRecord) > 0)
				myRecord = curRecord;
		}
		long val = myTierGame.recordToLong(myRecord);
		return val;
	}

	@Override
	public void writeRecord(DatabaseHandle dh, long recordIndex, long r)
			throws IOException {
		int tier = myTierGame.hashToTier(recordIndex);
		if (shouldBeInDatabase(tier)) {
			//super.writeRecord(dh, recordIndex, r);
			inner.writeRecord(dh, recordIndex, r);
		}
	}

	private final boolean shouldBeInDatabase(int tier) {
		// Unsure if this is right, only for last row
		if (tier == myTierGame.numberOfTiers() - 1) {
			return false;
		} else {
			return tier % (numTiersCut + 1) == 0;
		}

	}

	private boolean inLastRow(long curByte) {
		return deleteLastRow
				&& curByte < (myTierGame.numHashes() - myTierGame
						.numHashesForTier(myTierGame.numberOfTiers() - 1));

	}

	@Override
	protected int readBytes(DatabaseHandle dh, long location, byte[] array,
			int off, int len) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	protected int writeBytes(DatabaseHandle dh, long location, byte[] array,
			int off, int len) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public DatabaseHandle getHandle(boolean reading) {
		return new TierCutDatabaseHandle(myLogic.recordBytes, reading);
	}

	private class TierCutDatabaseHandle extends DatabaseHandle {
		private final DatabaseHandle innerHandle;

		public TierCutDatabaseHandle(int numBytes, boolean reading) {
			super(numBytes, reading);
			innerHandle = inner.getHandle(reading);
		}
	}

	@Override
	protected void lowerPrepareReadRange(DatabaseHandle dh,
			long firstByteIndex, long numBytes) throws IOException {
		inner.lowerPrepareReadRange(((TierCutDatabaseHandle) dh).innerHandle,
				firstByteIndex, numBytes);
	}
	
	@Override
	protected void lowerPrepareWriteRange(DatabaseHandle dh,
			long firstByteIndex, long numBytes) throws IOException {
		inner.lowerPrepareWriteRange(((TierCutDatabaseHandle) dh).innerHandle, firstByteIndex, numBytes);

	}
	
	@Override
	protected int lowerReadBytes(DatabaseHandle dh, byte[] array, int off,
			int maxLen) throws IOException {
		return inner.lowerReadBytes(((TierCutDatabaseHandle) dh).innerHandle, array, off, maxLen);
	}
	
	@Override
	protected int lowerWriteBytes(DatabaseHandle dh, byte[] array, int off,
			int maxLen) throws IOException {
		return inner.lowerWriteBytes(((TierCutDatabaseHandle) dh).innerHandle, array, off, maxLen);
	}
	
	@Override
	public void close() throws IOException {
		inner.close();
	}
}
