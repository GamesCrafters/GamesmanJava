package edu.berkeley.gamesman.database;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.game.TierGame;
import edu.berkeley.gamesman.game.util.TierState;
import edu.berkeley.gamesman.util.Pair;

public class TierCutDatabase extends DatabaseWrapper {
	
	TierGame myTierGame;
	private static boolean deleteLastRow;
	private static int numTiersCut;

	public TierCutDatabase(Database db, Configuration config, long firstRecord,
			long numRecords, boolean reading, boolean writing) {
		super(db, config, firstRecord, numRecords, reading, writing);
		myTierGame = (TierGame) config.getGame();
		deleteLastRow = true;
		numTiersCut = 1;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	protected int writeBytes(DatabaseHandle dh, long location, byte[] array,
			int off, int len) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	protected int readBytes(DatabaseHandle dh, long location, byte[] array,
			int off, int len) throws IOException {
		throw new UnsupportedOperationException();
	}

	/*
	 * @Override protected synchronized void prepareRange(DatabaseHandle dh,
	 * long byteIndex, int firstNum, long numBytes, int lastNum) {
	 * super.prepareRange(dh, byteIndex, firstNum, numBytes, lastNum); // TODO
	 * Finish method }
	 * 
	 * @Override protected synchronized int getBytes(DatabaseHandle dh, byte[]
	 * arr, int off, int maxLen, boolean overwriteEdgesOk) { if
	 * (!overwriteEdgesOk) return super.getBytes(dh, arr, off, maxLen, false);
	 * int numBytes = (int) Math.min(maxLen, dh.lastByteIndex - dh.location); //
	 * TODO Finish method if (!shouldBeInDatabase(numBytes)) { // Get Primitive
	 * Value instead int hash = off; //get hash from input is off? TierState pos
	 * = new TierState(); myTierGame.hashToState(hash, pos);
	 * myTierGame.setState(pos); Record myRecord = new Record(conf);
	 * myRecord.value = myTierGame.primitiveValue(); myRecord.remoteness = 0;
	 * long val = myTierGame.recordToLong(new TierState(), myRecord);
	 * toUnsignedByteArray(val,arr, off); // is off right? } else { // Get from
	 * database as normal return super.getBytes(dh, arr, off, maxLen,
	 * overwriteEdgesOk); //? } return numBytes; }
	 * 
	 * @Override protected synchronized int putBytes(DatabaseHandle dh, byte[]
	 * arr, int off, int maxLen, boolean edgesAreCorrect) { if
	 * (!edgesAreCorrect) return super.putBytes(dh, arr, off, maxLen, false);
	 * int numBytes = (int) Math.min(maxLen, dh.lastByteIndex - dh.location); if
	 * (shouldBeInDatabase(numBytes)) { return super.putBytes(dh, arr, off,
	 * maxLen, false); } return numBytes; }
	 */

	@Override
	public long readRecord(DatabaseHandle dh, long recordIndex)
			throws IOException {
		long byteIndex = myLogic.getByteIndex(recordIndex);
		TierState curPos = myTierGame.hashToState(recordIndex);
		myTierGame.setState(curPos);
		int tier = myTierGame.getTier();
		if (shouldBeInDatabase(tier)) {
			return readRecordFromByteIndex(dh, byteIndex);
		} else {
			if (inLastRow(byteIndex)) {
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
		TierState pos = myTierGame.hashToState(recordIndex);
		Collection<Pair<String, TierState>> moves = myTierGame.validMoves(pos);
		boolean hasTie = false;
		boolean hasWin = false;
		Record myRecord = new Record(conf);
		int currentRemoteness = Integer.MAX_VALUE;
		boolean gameHasRemoteness = false;
		Iterator<Pair<String, TierState>> moveIter = moves.iterator();
		for (int i = 0; i < moves.size(); i++) {
			Pair<String, TierState> move = moveIter.next();
			TierState newState = myTierGame.doMove(pos, move.car);
			long newHash = myTierGame.stateToHash(newState);
			Record curRecord = new Record(conf);
			myTierGame.longToRecord(new TierState(), readRecord(dh, newHash),
					curRecord);
			Value curValue = curRecord.value;
			int curRemoteness = Integer.MAX_VALUE;
			if (curValue.hasRemoteness) {
				gameHasRemoteness = true;
				curRemoteness = curRecord.remoteness;
			}
			if (curValue.equals(Value.WIN)) {
				if (hasWin) {
					currentRemoteness = Math.min(currentRemoteness,
							curRemoteness);
				} else {
					hasWin = true;
					currentRemoteness = curRemoteness;
				}
			} else if ((curValue.equals(Value.TIE))) {
				if (hasTie) {
					currentRemoteness = Math.min(currentRemoteness,
							curRemoteness);
				} else {
					hasTie = true;
					currentRemoteness = curRemoteness;
				}
			} else {
				currentRemoteness = Math.min(currentRemoteness, curRemoteness);
			}

		}
		if (hasWin) {
			myRecord.value = Value.WIN;
		} else if (hasTie) {
			myRecord.value = Value.TIE;
		} else {
			myRecord.value = Value.LOSE;
		}
		if (gameHasRemoteness) {
			myRecord.remoteness = currentRemoteness + 1;
		}
		long val = myTierGame.recordToLong(myRecord);
		return val;
	}

	@Override
	public void writeRecord(DatabaseHandle dh, long recordIndex, long r)
			throws IOException {
		long byteIndex = myLogic.getByteIndex(recordIndex);
		TierState curPos = myTierGame.hashToState(recordIndex);
		myTierGame.setState(curPos);
		int tier = myTierGame.getTier();
		if (shouldBeInDatabase(tier)) {
			writeRecordFromByteIndex(dh, byteIndex, r);
		}
	}

	// Is this supposed to be the actual amount of records, or the lying amount
	// of records?
	public static long getNumRecords(long firstRecord, long numRecords,
			TierGame game) {
		int numOfTiers = game.numberOfTiers();
		long lastTierHashes = game.numHashesForTier(numOfTiers - 1);
		long currentSize = 0;
		// TODO Auto-generated method stub
		if (deleteLastRow) {
			return numRecords + lastTierHashes;
		} else {
			return numRecords;
		}
	}

	private boolean shouldBeInDatabase(int tier) {
		// Unsure if this is right, only for last row
		if (tier == myTierGame.numberOfTiers()) {
			return false;
		} else {
			boolean inDB = false;
			int remainTiers = 0;
			for (int i = 1; i <= tier; i++) {
				if (remainTiers == 0) {
					inDB = true;
					remainTiers = numTiersCut;
				} else {
					remainTiers--;
					inDB = false;
				}

			}
			return inDB;
		}

	}

	private boolean inLastRow(long curByte) {
		return deleteLastRow
				&& curByte < (myTierGame.numHashes() - myTierGame
						.numHashesForTier(myTierGame.numberOfTiers() - 1));

	}
}
