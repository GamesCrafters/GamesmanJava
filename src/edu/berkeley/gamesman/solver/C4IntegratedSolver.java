package edu.berkeley.gamesman.solver;

import edu.berkeley.gamesman.core.*;
import edu.berkeley.gamesman.database.util.DelocalizedPage;
import edu.berkeley.gamesman.database.util.LocalizedPage;
import edu.berkeley.gamesman.game.Connect4;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Util;

public class C4IntegratedSolver extends TierSolver<ItergameState> {
	@Override
	protected void solvePartialTier(Configuration conf, long start, long end,
			TierSolverUpdater t) {
		if (end < start)
			return;
		Connect4 game = Util.checkedCast(conf.getGame());
		long current = start;
		long currentGroup = current / conf.recordsPerGroup;
		int currentNum = (int) (current % conf.recordsPerGroup);
		game.setState(game.hashToState(start));
		Record[] vals = new Record[game.maxChildren()];
		for (int i = 0; i < vals.length; i++)
			vals[i] = game.newRecord();
		Record prim = game.newRecord();
		ItergameState[] children = new ItergameState[game.maxChildren()];
		DelocalizedPage[] childPages = new DelocalizedPage[game.maxChildren()];
		int pageBytes = conf.getInteger("gamesman.db.pageSize",
				100000 / childPages.length);
		int pageSize = DelocalizedPage.numGroups(conf, pageBytes);
		int writeLen = (int) (end / conf.recordsPerGroup - currentGroup + 1);
		LocalizedPage writePage = new LocalizedPage(conf, writeLen, 1);
		assert Util.debug(DebugFacility.SOLVER, "Loading " + currentGroup
				+ " - " + (currentGroup + writeLen - 1) + " for write");
		writePage.loadPage(db, currentGroup, writeLen);
		boolean hasRemoteness = conf.containsField(RecordFields.REMOTENESS);
		for (int i = 0; i < children.length; i++) {
			children[i] = new ItergameState();
			childPages[i] = new DelocalizedPage(conf, pageSize);
		}
		while (current <= end) {
			if (current % stepSize == 0)
				t.calculated(stepSize);
			PrimitiveValue pv = game.primitiveValue();
			if (pv.equals(PrimitiveValue.UNDECIDED)) {
				int len = game.validMoves(children);
				Record r;
				for (int i = 0; i < len; i++) {
					r = vals[i];
					long hash = game.stateToHash(children[i]);
					long hashGroup = hash / conf.recordsPerGroup;
					if (!childPages[game.openColumn[i]]
							.containsGroup(hashGroup)) {
						long afterHashGroup = game
								.lastHashValueForTier(children[i].tier)
								/ conf.recordsPerGroup + 1L;
						int loadSize = pageSize;
						if (hashGroup + pageSize > afterHashGroup)
							loadSize = (int) (afterHashGroup - hashGroup);
						assert Util.debug(DebugFacility.SOLVER, "Loading "
								+ hashGroup + " - "
								+ (hashGroup + loadSize - 1) + " for column "
								+ game.openColumn[i]);
						childPages[game.openColumn[i]].loadPage(db, hashGroup,
								loadSize);
					}
					childPages[game.openColumn[i]].getRecord(hashGroup,
							(int) (hash % conf.recordsPerGroup), r);
					r.previousPosition();
				}
				Record newVal = game.combine(vals, 0, len);
				writePage.putRecord(currentGroup, currentNum, newVal);
			} else {
				if (hasRemoteness)
					prim.set(RecordFields.REMOTENESS, 0);
				prim.set(RecordFields.VALUE, pv.value());
				writePage.putRecord(currentGroup, currentNum, prim);
			}
			if (current != end)
				game.nextHashInTier();
			current++;
			currentNum++;
			if (currentNum == conf.recordsPerGroup) {
				currentNum = 0;
				currentGroup++;
			}
		}
		assert Util.debug(DebugFacility.SOLVER, "Writing "
				+ writePage.firstGroup + " - "
				+ (writePage.firstGroup + writePage.numGroups - 1));
		writePage.writeBack(db);
	}
}
