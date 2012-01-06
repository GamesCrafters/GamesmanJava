package edu.berkeley.gamesman.database.cache;

import java.io.IOException;
import java.util.Collections;

import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.game.TierGame;
import edu.berkeley.gamesman.game.util.TierState;
import edu.berkeley.gamesman.hasher.cachehasher.CacheHasher;
import edu.berkeley.gamesman.hasher.cachehasher.CacheMove;
import edu.berkeley.gamesman.hasher.genhasher.GenHasher;
import edu.berkeley.gamesman.hasher.genhasher.GenState;

/**
 * @author dnspies
 * 
 */
public class GenCache<S extends GenState> extends TierCache {
	private final CacheHasher<S> myH;
	private final RecordRangeCache[] ranges;
	private final TierGame game;
	private long curStart, curNum;
	private int tier;
	private final DatabaseHandle dh;

	/**
	 * @param db
	 * @param availableMemory
	 * @param thisTier
	 * @param nextTier
	 * @param allMoves
	 * @param game
	 * @param tier
	 */
	@SuppressWarnings("unchecked")
	public GenCache(Database db, long availableMemory, GenHasher<S> thisTier,
			GenHasher<S> nextTier, CacheMove[] allMoves, TierGame game, int tier) {
		super(db, availableMemory);
		myH = new CacheHasher<S>(thisTier, Collections.nCopies(allMoves.length,
				nextTier).toArray(new GenHasher[allMoves.length]), allMoves,
				false);
		ranges = new RecordRangeCache[allMoves.length];
		for (int i = 0; i < allMoves.length; i++) {
			ranges[i] = new RecordRangeCache(db);
		}
		this.game = game;
		this.tier = tier;
		dh = db.getHandle(true);
	}

	@Override
	public void fetchChildren(TierState position, int numChildren,
			TierState[] children, int[] hints, Record[] values) {
		long curHash = game.stateToHash(position);
		ensureContains(curHash);
		for (int i = 0; i < numChildren; i++) {
			long hash = game.stateToHash(children[i]);
			game.longToRecord(children[i], ranges[hints[i]].readRecord(hash),
					values[i]);
		}
	}

	private void ensureContains(long curHash) {
		if (curHash >= curStart && curHash - curStart < curNum)
			return;
		curStart = curHash;
		long available = db.recordsForBytes(availableMemory);
		long addOn = available / ranges.length;
		long lastHash = curHash + addOn;
		TierState ts = game.getPoolState();
		while (lastHash < game.hashOffsetForTier(tier + 1)
				&& storeSize(curHash, lastHash, ts) < available) {
			addOn *= 2;
			lastHash = curHash + addOn;
		}
		if (lastHash > game.hashOffsetForTier(tier + 1))
			lastHash = game.hashOffsetForTier(tier + 1);
		long lowLast = curHash, highLast = lastHash;
		while (highLast - lowLast > 1) {
			long guessLast = (highLast + lowLast) / 2;
			long numRecs = storeSize(curHash, guessLast, ts);
			if (numRecs <= available)
				lowLast = guessLast;
			else
				highLast = guessLast;
		}
		curNum = lowLast - curStart + 1;
		for (int i = 0; i < ranges.length; i++) {
			long[] firstLast = rangePair(i, curStart, lowLast, ts);
			if (firstLast != null) {
				long recordIndex = firstLast[0];
				int numRecords = (int) (firstLast[1] - recordIndex + 1);
				ranges[i].setRange(recordIndex, numRecords);
				try {
					ranges[i].readRecordsFromDatabase(db, dh, recordIndex,
							numRecords);
				} catch (IOException e) {
					throw new Error(e);
				}
			}
		}
		game.release(ts);
	}

	private long[] rangePair(int whichChild, long startPoint, long endPoint,
			TierState ts) {
		game.hashToState(startPoint, ts);
		myH.unhash(ts.hash);
		ts.tier = tier + 1;
		ts.hash = myH.boundNextChild(whichChild, 1);
		if (ts.hash == -1)
			return null;
		long firstRecordIndex = game.stateToHash(ts);
		game.hashToState(endPoint, ts);
		myH.unhash(ts.hash);
		ts.tier = tier + 1;
		ts.hash = myH.boundNextChild(whichChild, -1);
		if (ts.hash == -1)
			return null;
		long lastRecordIndex = game.stateToHash(ts);
		if (lastRecordIndex < firstRecordIndex)
			return null;
		else
			return new long[] { firstRecordIndex, lastRecordIndex };
	}

	private long storeSize(long startHash, long lastHash, TierState ts) {
		long total = 0L;
		for (int i = 0; i < ranges.length; i++) {
			long[] pair = rangePair(i, startHash, lastHash, ts);
			if (pair != null)
				total += pair[1] - pair[0] + 1;
		}
		return total;
	}
}
