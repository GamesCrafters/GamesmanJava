package edu.berkeley.gamesman.database.cache;

import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.game.Quarto;
import edu.berkeley.gamesman.game.util.TierState;

public class QuartoCache extends TierCache {
	private final RecordRangeCache[][] upperCaches = new RecordRangeCache[16][16];
	private final RecordRangeCache lowerCache;
	private final Quarto game;

	public QuartoCache(Quarto game, Database db, long availableMemory) {
		super(db, availableMemory);
		this.game = game;
		lowerCache = new RecordRangeCache(db);
		for (int place = 0; place < 16; place++) {
			for (int pieceNum = 0; pieceNum < 16; pieceNum++) {
				upperCaches[place][pieceNum] = new RecordRangeCache(db);
			}
		}
	}

	@Override
	public void fetchChildren(TierState position, int numChildren,
			TierState[] children, int[] hints, Record[] values) {
		// TODO Auto-generated method stub

	}

}
