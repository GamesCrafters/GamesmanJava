package edu.berkeley.gamesman.solver;

import java.util.ArrayList;

import edu.berkeley.gamesman.core.*;
import edu.berkeley.gamesman.database.util.Page;
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
	protected void solvePartialTier(Configuration conf, long start,
			long hashes, TierSolverUpdater t, Database inRead, Database inWrite) {
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
		long endGroup = (start + hashes - 1) / conf.recordsPerGroup;
		int writeLen = (int) (endGroup + 1 - currentGroup);
		Page writePage = new Page(conf);
		writePage.loadPage(currentGroup, writeLen);
		if (!hadooping && (start + hashes) % conf.recordsPerGroup > 0) {
			if (conf.recordGroupUsesLong)
				writePage.setGroup(writeLen - 1, inRead
						.getLongRecordGroup(endGroup
								* conf.recordGroupByteLength));
			else
				writePage.setGroup(writeLen - 1, inRead
						.getBigIntRecordGroup(endGroup
								* conf.recordGroupByteLength));
		}
		assert Util.debug(DebugFacility.SOLVER, "Loading " + currentGroup
				+ " - " + (currentGroup + writeLen - 1) + " for write");
		boolean hasRemoteness = conf.containsField(RecordFields.REMOTENESS);
		game.setState(game.hashToState(start + hashes - 1));
		if (game.getTier() < game.numberOfTiers() - 1) {
			children = new ItergameState[game.maxChildren()];
			for (int i = 0; i < children.length; i++) {
				children[i] = new ItergameState();
			}
			game.lastMoves(children);
			ends = new long[children.length];
			for (int i = 0; i < ends.length; i++) {
				whichPage[i] = -1;
				ends[i] = game.stateToHash(children[i]) / conf.recordsPerGroup;
			}
		}
		game.setState(game.hashToState(start));
		for (long count = 0L; count < hashes; count++) {
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
					int col = game.openColumn[i];
					if (whichPage[col] == -1) {
						long pageStart = hashGroup, pageEnd = ends[col];
						int lowPage = -1;
						ArrayList<Page> pageList = new ArrayList<Page>(game
								.maxChildren());
						for (int c = 0; c < numPages; c++) {
							if (childPages[c] == null)
								continue;
							long firstGroup = childPages[c].firstGroup;
							long lastGroup = firstGroup
									+ childPages[c].numGroups - 1;
							if (firstGroup <= pageEnd && lastGroup >= pageStart) {
								if (lastGroup > pageEnd)
									pageEnd = lastGroup;
								if (lowPage == -1
										|| firstGroup < childPages[lowPage].firstGroup) {
									if (lowPage >= 0) {
										pageList.add(childPages[lowPage]);
										childPages[lowPage] = null;
									}
									lowPage = c;
								} else {
									pageList.add(childPages[c]);
									childPages[c] = null;
								}
							}
						}
						if (lowPage == -1) {
							childPages[numPages] = new Page(conf);
							childPages[numPages].loadPage(inRead, pageStart,
									(int) (pageEnd - pageStart) + 1);
							lowPage = numPages;
							++numPages;
						}
						Page thePage = childPages[lowPage];
						if (thePage.firstGroup <= pageStart) {
							pageStart = thePage.firstGroup;
							thePage
									.ensureCapacity((int) (pageEnd - pageStart) + 1);
						} else {
							thePage.extendDown(inRead, hashGroup,
									(int) (pageEnd - pageStart) + 1);
						}
						long min = pageEnd + 1;
						while (!pageList.isEmpty()) {
							Page nextPage = null;
							for (Page p : pageList) {
								if (p.firstGroup < min) {
									nextPage = p;
									min = p.firstGroup;
								}
							}
							pageList.remove(nextPage);
							thePage.extendUp(inRead, nextPage);
						}
						if (thePage.firstGroup + thePage.numGroups <= pageEnd)
							thePage.extendUp(inRead, pageEnd);
						whichPage[col] = lowPage;
						for (int c = 0; c < whichPage.length; c++) {
							if (whichPage[c] >= 0
									&& childPages[whichPage[c]] == null)
								whichPage[c] = lowPage;
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
			if (count < hashes - 1)
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
		writePage.writeBack(inWrite);
	}
}
