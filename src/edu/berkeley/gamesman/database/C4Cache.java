package edu.berkeley.gamesman.database;

import java.io.IOException;

import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.game.Connect4;
import edu.berkeley.gamesman.game.util.TierState;

public class C4Cache extends TierCache {
	private final RecordRangeCache[] ranges;
	private final Connect4 game;
	private final int memPerChild;
	private final DatabaseHandle dh;

	public C4Cache(Connect4 g, Database db, long availableMemory) {
		super(db, availableMemory);
		dh = db.getHandle(true);
		game = g;
		int width = g.gameWidth;
		memPerChild = (int) Math
				.min(Integer.MAX_VALUE, availableMemory / width);
		ranges = new RecordRangeCache[width];
		for (int i = 0; i < width; i++) {
			ranges[i] = new RecordRangeCache(db);
			ranges[i].ensureByteCapacity(memPerChild, false);
		}
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
		long addHash = Math.min(db.myLogic.getNumRecords(memPerChild) * 2,
				game.numHashesForTier(currentPosition.tier)
						- currentPosition.hash);
		long lastChild;
		TierState endPosition = game.newState();
		TierState[] lastChildren = game.newStateArray(game.gameWidth);
		endPosition.set(currentPosition);
		do {
			endPosition.hash = currentPosition.hash + addHash - 1;
			game.setState(endPosition);
			game.lastMoves(lastChildren);
			lastChild = lastChildren[place].hash;
			addHash /= 2;
		} while (db.myLogic.getNumBytes(lastChild - child.hash) > memPerChild);
		game.setState(currentPosition);
		long tierOffset = game.hashOffsetForTier(child.tier);
		long endChildHash = tierOffset + lastChild + 1;
		int numRecords = (int) (endChildHash - childHash);
		ranges[place].setRange(childHash, numRecords);
		long byteIndex = db.myLogic.getByteIndex(childHash);
		int numBytes = (int) db.myLogic.getNumBytes(numRecords);
		System.out.println("Reading records for place " + place + ": "
				+ byteIndex + "-" + (byteIndex + numBytes - 1));
		try {
			ranges[place].readFromDatabase(db, dh, byteIndex, numBytes);
		} catch (IOException e) {
			throw new Error(e);
		}
		return childHash;
	}
}
