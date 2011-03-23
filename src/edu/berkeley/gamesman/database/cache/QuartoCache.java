package edu.berkeley.gamesman.database.cache;

import java.io.IOException;

import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.game.Quarto;
import edu.berkeley.gamesman.game.util.TierState;
import edu.berkeley.gamesman.hasher.DartboardHasher;
import edu.berkeley.gamesman.hasher.QuartoMinorHasher;

public class QuartoCache extends TierCache {
	private final RecordRangeCache[][] upperCaches = new RecordRangeCache[16][16];
	private final RecordRangeCache[] lowerCache = new RecordRangeCache[16];
	private final DartboardHasher majorHasher;
	private final QuartoMinorHasher minorHasher;
	private final Quarto game;

	public QuartoCache(Quarto game, DartboardHasher majorHasher,
			QuartoMinorHasher minorHasher, Database db, long availableMemory) {
		super(db, availableMemory);
		this.game = game;
		this.majorHasher = majorHasher;
		this.minorHasher = minorHasher;
		for (int place = 0; place < 16; place++) {
			for (int pieceNum = 0; pieceNum < 16; pieceNum++) {
				upperCaches[place][pieceNum] = new RecordRangeCache(db);
			}
			lowerCache[place] = new RecordRangeCache(db);
		}
	}

	@Override
	public void fetchChildren(TierState position, int numChildren,
			TierState[] children, int[] hints, Record[] values) {
		// TODO Auto-generated method stub

	}

	private boolean setCacheThrough(int place, long availableMemory) {
		int availableIntMemory = (int) Math.min(availableMemory,
				Integer.MAX_VALUE);
		int minorIndex = game.placeList[place].getMinorIndex();
		long[] range = minorHasher.getCache(minorIndex,
				db.myLogic.getNumRecords(availableIntMemory));
		if (range == null)
			return false;
		else {
			lowerCache[place].ensureByteCapacity(availableIntMemory, false);
			lowerCache[place].setRange(
					game.hashOffsetForTier()
							+ majorHasher.getChild(' ', 'P', place)
							* minorHasher.numHashesForTier() + range[0],
					(int) range[1]);
			return true;
		}
	}

	private boolean setCacheThrough(int place, int piece, long availableMemory) {
		int availableIntMemory = (int) Math.min(availableMemory,
				Integer.MAX_VALUE);
		int minorIndex = game.placeList[place].getMinorIndex();
		long[] range = minorHasher.getCache(minorIndex, piece,
				db.myLogic.getNumRecords(availableIntMemory));
		if (range == null)
			return false;
		else {
			upperCaches[place][piece].ensureByteCapacity(availableIntMemory,
					false);
			upperCaches[place][piece].setRange(
					game.hashOffsetForTier()
							+ majorHasher.getChild(' ', 'P', place)
							* minorHasher.numHashesForTier() + range[0],
					(int) range[1]);
			return true;
		}
	}

	private boolean setCacheThroughAll(int place, long availableMemory) {
		int availableIntMemory = (int) Math.min(availableMemory,
				Integer.MAX_VALUE);
		long currentMajorChild = majorHasher.nextChild(' ', 'P', place);
		int availableMajor = (int) (db.myLogic
				.getNumRecords(availableIntMemory) / minorHasher
				.numHashesForTier());
		if (availableMajor == 0)
			return false;
		else {
			int addMajor = availableMajor * 2;
			long lastMajorChild;
			long majorHash = majorHasher.getHash();
			do {
				majorHasher.unhash(majorHash + addMajor - 1);
				lastMajorChild = majorHasher.previousChild(' ', 'P', place);
				addMajor /= 2;
			} while (lastMajorChild - currentMajorChild + 1 > addMajor);
			majorHasher.unhash(majorHash);
			return true;
		}
	}
}
