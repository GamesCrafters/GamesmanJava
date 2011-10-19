package edu.berkeley.gamesman.database;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.game.TierGame;
import edu.berkeley.gamesman.game.util.TierState;
import edu.berkeley.gamesman.util.Util;

/**
 * A {@link Database} wrapper for tier games that supports having a number of
 * its tiers missing. Values in the missing tiers are solved for upon request.
 */
public final class TierCutDatabase extends Database {

	private final String uri;
	private final TierGame myTierGame;
	private final boolean[] inDatabase;
	private final Database inner;

	public TierCutDatabase(String uri, Configuration conf,
			long firstRecordIndex, long numRecords, boolean reading,
			boolean writing) throws IOException {
		super(conf, firstRecordIndex, numRecords, reading, writing);
		this.uri = uri;
		myTierGame = (TierGame) conf.getGame();
		int numberOfTiers = myTierGame.numberOfTiers();
		int[] preservedTiers = Util.parseIntArray(conf.getProperty(
				"gamesman.database.stored.tiers", null));
		if (preservedTiers == null)
			preservedTiers = makeArray(
					conf.getInteger("gamesman.database.tiers.cut", 2),
					numberOfTiers);
		inDatabase = new boolean[numberOfTiers];
		for (int i : preservedTiers) {
			inDatabase[i] = true;
		}
		String innerUri = conf.getProperty("gamesman.db.inner.uri");
		try {
			inner = Database.openDatabase(innerUri);
		} catch (ClassNotFoundException e) {
			throw new Error(e);
		}
	}

	public TierCutDatabase(Database inner, String uri, Configuration conf,
			long firstRecordIndex, long numRecords, int[] preservedTiers)
			throws IOException {
		super(conf, firstRecordIndex, numRecords, false, true);
		this.uri = uri;
		myTierGame = (TierGame) conf.getGame();
		inDatabase = new boolean[myTierGame.numberOfTiers()];
		for (int i : preservedTiers) {
			inDatabase[i] = true;
		}
		this.inner = inner;
	}

	private int[] makeArray(int numTiersCut, int numberOfTiers) {
		int[] preserved = new int[(numberOfTiers + numTiersCut)
				/ (numTiersCut + 1)];
		for (int i = 0; i < preserved.length; i++) {
			preserved[i] = i * (numTiersCut + 1);
		}
		return preserved;
	}

	@Override
	public long readRecord(DatabaseHandle dh, long recordIndex)
			throws IOException {
		int tier = myTierGame.hashToTier(recordIndex);
		if (shouldBeInDatabase(tier)) {
			return inner.readRecord(dh, recordIndex);
		} else {
			// Value not in DB, so solve for it.
			TierState pos = myTierGame.getPoolState();
			myTierGame.hashToState(recordIndex, pos);
			Value value = myTierGame.primitiveValue(pos);
			if (value != Value.UNDECIDED) {
				// Primitive position, so just return it.
				Record myRecord = myTierGame.getPoolRecord();
				myRecord.value = value;
				myRecord.remoteness = 0;
				long val = myTierGame.recordToLong(pos, myRecord);
				myTierGame.release(myRecord);
				myTierGame.release(pos);
				return val;
			} else {
				// Perform a solve for the missing tiers.
				myTierGame.release(pos);
				return missingTierSolve(dh, recordIndex);
			}
		}
	}

	/**
	 * Solves for the value of the state corresponding to the given index.
	 * 
	 * @param dh
	 *            A handle to the database
	 * @param recordIndex
	 *            The hash of the game state where the record should be read
	 * @return The solved hash of the record corresponding to the given index
	 * @throws IOException
	 */
	private long missingTierSolve(DatabaseHandle dh, long recordIndex)
			throws IOException {
		// Keep the underlying database open.
		boolean setHolding = false;
		if (inner instanceof SplitDatabase)
			setHolding = ((SplitDatabase) inner).setHolding(true);
		// Initialize a TierState from the index.
		TierState pos = myTierGame.getPoolState();
		myTierGame.hashToState(recordIndex, pos);
		// Get the valid moves.
		TierState[] childStates = myTierGame.getPoolChildStateArray();
		int numChildren = myTierGame.validMoves(pos, childStates);
		Record myRecord = myTierGame.getPoolRecord();
		myRecord.value = Value.UNDECIDED;
		Record moveRecord = myTierGame.getPoolRecord();
		// Loops through children and calculates the current state's value.
		for (int childIndex = 0; childIndex < numChildren; childIndex++) {
			TierState childState = childStates[childIndex];
			long childHash = myTierGame.stateToHash(childState);
			// Retrieve the record for child's state.
			long recordLong = readRecord(dh, childHash);
			myTierGame.longToRecord(childState, recordLong, moveRecord);
			moveRecord.previousPosition();
			// Set current record to be the best of current and child.
			if (myRecord.value == Value.UNDECIDED
					|| moveRecord.compareTo(myRecord) > 0)
				myRecord.set(moveRecord);
		}
		long val = myTierGame.recordToLong(pos, myRecord);
		// Clean up resources.
		myTierGame.release(myRecord);
		myTierGame.release(moveRecord);
		myTierGame.release(childStates);
		myTierGame.release(pos);
		if (setHolding)
			((SplitDatabase) inner).setHolding(false);
		return val;
	}

	@Override
	public void writeRecord(DatabaseHandle dh, long recordIndex, long r)
			throws IOException {
		int tier = myTierGame.hashToTier(recordIndex);
		if (shouldBeInDatabase(tier)) {
			// super.writeRecord(dh, recordIndex, r);
			inner.writeRecord(dh, recordIndex, r);
		}
	}

	private final boolean shouldBeInDatabase(int tier) {
		return inDatabase[tier];
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
		return inner.getHandle(reading);
	}

	@Override
	protected void lowerPrepareReadRange(DatabaseHandle dh,
			long firstByteIndex, long numBytes) throws IOException {
		inner.lowerPrepareReadRange(dh, firstByteIndex, numBytes);
	}

	@Override
	protected void lowerPrepareWriteRange(DatabaseHandle dh,
			long firstByteIndex, long numBytes) throws IOException {
		inner.lowerPrepareWriteRange(dh, firstByteIndex, numBytes);

	}

	@Override
	protected int lowerReadBytes(DatabaseHandle dh, byte[] array, int off,
			int maxLen) throws IOException {
		return inner.lowerReadBytes(dh, array, off, maxLen);
	}

	@Override
	protected int lowerWriteBytes(DatabaseHandle dh, byte[] array, int off,
			int maxLen) throws IOException {
		return inner.lowerWriteBytes(dh, array, off, maxLen);
	}

	@Override
	public void close() throws IOException {
		if (writing) {
			DataOutputStream dos = new DataOutputStream(new FileOutputStream(
					uri));
			writeHeader(dos);
			dos.close();
		}
		inner.close();
	}

	public static void main(String[] args) throws IOException,
			ClassNotFoundException {
		Database inner = Database.openDatabase(args[0]);
		Configuration conf = inner.conf;
		int[] storedTiers = new int[args.length - 2];
		for (int i = 0; i < storedTiers.length; i++) {
			storedTiers[i] = Integer.parseInt(args[i + 2]);
		}
		Arrays.sort(storedTiers);
		conf.setProperty("gamesman.database.stored.tiers",
				Arrays.toString(storedTiers));
		conf.setProperty("gamesman.db.inner.uri", args[0]);
		conf.setProperty("gamesman.database", TierCutDatabase.class.getName());
		TierCutDatabase newDb = new TierCutDatabase(inner, args[1], conf,
				inner.firstRecordIndex, inner.numRecords, storedTiers);
		newDb.close();
	}
}
