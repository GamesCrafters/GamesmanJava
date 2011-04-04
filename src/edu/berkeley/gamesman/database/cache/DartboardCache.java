package edu.berkeley.gamesman.database.cache;

import java.io.IOException;

import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.game.TierGame;
import edu.berkeley.gamesman.game.util.TierState;
import edu.berkeley.gamesman.hasher.DartboardHasher;

public class DartboardCache extends TierCache {
	private final RecordRangeCache[] ranges;
	private final TierGame game;
	private final DartboardHasher hasher;
	private final int memPerChild;
	private final DatabaseHandle dh;
	private char old, replace;

	public DartboardCache(TierGame g, Database db, long availableMemory,
			DartboardHasher hasher) {
		super(db, availableMemory);
		dh = db.getHandle(true);
		game = g;
		this.hasher = hasher;
		int boardSize = hasher.boardSize();
		memPerChild = (int) Math.min(Integer.MAX_VALUE, availableMemory
				/ boardSize);
		ranges = new RecordRangeCache[boardSize];
		for (int i = 0; i < boardSize; i++) {
			ranges[i] = new RecordRangeCache(db);
			ranges[i].ensureByteCapacity(memPerChild, false);
		}
	}

	public void setReplaceType(char old, char replace) {
		this.old = old;
		this.replace = replace;
	}

	@Override
	public void fetchChildren(TierState position, int numChildren,
			TierState[] children, int[] hints, Record[] values) {
		for (int i = 0; i < numChildren; i++) {
			int cache = hints[i];
			TierState child = children[i];
			long childHash = ensureContains(position, child, cache);
			game.longToRecord(child, ranges[cache].readRecord(childHash),
					values[i]);
		}
	}

	private long ensureContains(TierState currentPosition, TierState child,
			int place) {
		long childHash = game.stateToHash(child);
		if (ranges[place].containsRecord(childHash))
			return childHash;
		long addHash = Math.min(db.recordsForBytes(memPerChild) * 2,
				hasher.numHashes() - currentPosition.hash);
		long lastChild;
		do {
			hasher.unhash(currentPosition.hash + addHash - 1);
			lastChild = hasher.previousChild(old, replace, place);
			addHash /= 2;
		} while (db.getNumBytes(lastChild - child.hash + 1) > memPerChild);
		hasher.unhash(currentPosition.hash);
		long tierOffset = game.hashOffsetForTier(child.tier);
		long endChildHash = tierOffset + lastChild + 1;
		int numRecords = (int) (endChildHash - childHash);
		ranges[place].setRange(childHash, numRecords);
		try {
			ranges[place]
					.readRecordsFromDatabase(db, dh, childHash, numRecords);
		} catch (IOException e) {
			throw new Error(e);
		}
		return childHash;
	}
}
