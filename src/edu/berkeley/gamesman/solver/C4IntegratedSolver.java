package edu.berkeley.gamesman.solver;

import edu.berkeley.gamesman.core.*;
import edu.berkeley.gamesman.database.util.Page;
import edu.berkeley.gamesman.database.util.LocalizedPage;
import edu.berkeley.gamesman.game.Connect4;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Util;

/**
 * Solves Connect 4 using the most efficient possible cache and cache settings
 * 
 * @author dnspies
 */
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
		Record[] vals = new Record[game.maxChildren()];
		for (int i = 0; i < vals.length; i++)
			vals[i] = game.newRecord();
		Record prim = game.newRecord();
		ItergameState[] children = null;
		long[] ends = null;
		int numPages = 0;
		Page[] childPages = new Page[game.maxChildren()];
		int[] whichPage = new int[game.maxChildren()];
		int writeLen = (int) (end / conf.recordsPerGroup - currentGroup + 1);
		LocalizedPage writePage = new LocalizedPage(conf, 1);
		assert Util.debug(DebugFacility.SOLVER, "Loading " + currentGroup
				+ " - " + (currentGroup + writeLen - 1) + " for write");
		writePage.loadPage(db, currentGroup, writeLen);
		boolean hasRemoteness = conf.containsField(RecordFields.REMOTENESS);
		game.setState(game.hashToState(end));
		if (game.getTier() < game.numberOfTiers() - 1) {
			children = new ItergameState[game.maxChildren()];
			for (int i = 0; i < children.length; i++) {
				children[i] = new ItergameState();
			}
			game.lastMoves(children);
			ends = new long[children.length];
			for (int i = 0; i < ends.length; i++) {
				whichPage[i] = -1;
				ends[i] = (game.stateToHash(children[i]) + conf.recordsPerGroup - 1)
						/ conf.recordsPerGroup;
			}
		}
		game.setState(game.hashToState(start));
		while (current <= end) {
			if (current % STEP_SIZE == 0)
				t.calculated(STEP_SIZE);
			PrimitiveValue pv = game.primitiveValue();
			if (pv.equals(PrimitiveValue.UNDECIDED)) {
				int len = game.validMoves(children);
				Record r;
				for (int i = 0; i < len; i++) {
					r = vals[i];
					long hash = game.stateToHash(children[i]);
					long hashGroup = hash / conf.recordsPerGroup;
					int startPlace = -1, endPlace = -1;
					IF_DNE: if (whichPage[game.openColumn[i]] == -1) {
						for (int c = 0; c < numPages; c++)
							if (childPages[c].containsGroup(hashGroup)) {
								if (childPages[c]
										.containsGroup(ends[game.openColumn[i]])) {
									whichPage[game.openColumn[i]] = c;
									break IF_DNE;
								} else
									startPlace = c;
							} else if (childPages[c]
									.containsGroup(ends[game.openColumn[i]])) {
								endPlace = c;
							}
						if (startPlace == -1 && endPlace == -1) {
							whichPage[game.openColumn[i]] = numPages;
							childPages[numPages] = new Page(conf);
							int numGroups = (int) (ends[game.openColumn[i]]
									- hashGroup + 1);
							childPages[numPages].loadPage(db, hashGroup,
									numGroups);
							++numPages;
						} else if (endPlace == -1) {
							childPages[startPlace].extendUp(db,
									ends[game.openColumn[i]]);
							whichPage[game.openColumn[i]] = startPlace;
						} else if (startPlace == -1) {
							childPages[endPlace].extendDown(db, hashGroup);
							whichPage[game.openColumn[i]] = endPlace;
						} else {
							childPages[startPlace].extendUp(db,
									childPages[endPlace].firstGroup - 1);
							childPages[startPlace]
									.extendUp(childPages[endPlace]);
							whichPage[game.openColumn[i]] = startPlace;
						}
					}
					childPages[whichPage[game.openColumn[i]]].getRecord(
							hashGroup, (int) (hash % conf.recordsPerGroup), r);
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
