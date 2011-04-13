package edu.berkeley.gamesman.database;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.game.TierGame;
import edu.berkeley.gamesman.game.util.TierState;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

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
			// return super.readRecord(dh, recordIndex);
			return inner.readRecord(dh, recordIndex);
		} else {
			TierState pos = myTierGame.hashToState(recordIndex);
			myTierGame.setState(pos);
			Record myRecord = myTierGame.newRecord();
			myRecord.value = myTierGame.primitiveValue();
			if (myRecord.value != Value.UNDECIDED) {
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
