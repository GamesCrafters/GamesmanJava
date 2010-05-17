package edu.berkeley.gamesman.solver;

import java.text.DecimalFormat;
import java.util.ArrayList;

import edu.berkeley.gamesman.core.*;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.database.util.Page;
import edu.berkeley.gamesman.database.util.SequentialPage;
import edu.berkeley.gamesman.game.Connect4;
import edu.berkeley.gamesman.game.util.ItergameState;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Util;

/**
 * Solves Connect 4 using the most efficient possible cache and cache settings
 * 
 * @author dnspies
 */
public class C4IntegratedSolver extends TierSolver {
	private boolean directRead;

	@Override
	protected void solvePartialTier(Configuration conf, long start,
			long hashes, TierSolverUpdater t, Database readDb,
			DatabaseHandle readDh, Database writeDb, DatabaseHandle writeDh) {
		Connect4 game = Util.checkedCast(conf.getGame());
		long current = start;
		Record[] vals = new Record[game.maxChildren()];
		for (int i = 0; i < vals.length; i++)
			vals[i] = game.newRecord();
		Record prim = game.newRecord();
		ItergameState[] children = null;
		long[] ends = null;
		int numPages = 0;
		Page[] childPages = null;
		int[] whichPage = null;
		SequentialPage writePage = null;
		long currentGroup = 0L;
		int currentNum = 0;
		boolean hasRemoteness = conf.remotenessStates > 0;
		if (!directRead) {
			currentGroup = current / conf.recordsPerGroup;
			currentNum = (int) (current % conf.recordsPerGroup);
			long endGroup = (start + hashes - 1) / conf.recordsPerGroup;
			int writeLen = (int) (endGroup + 1 - currentGroup);
			childPages = new Page[game.maxChildren()];
			whichPage = new int[game.maxChildren()];
			writePage = new SequentialPage(conf);
			writePage.loadPage(currentGroup, writeLen);
			if (!parallelSolving && (start + hashes) % conf.recordsPerGroup > 0) {
				if (conf.recordGroupUsesLong)
					writePage.setGroup(writeLen - 1, readDb.getLongRecordGroup(
							readDh, endGroup * conf.recordGroupByteLength));
				else
					writePage.setGroup(writeLen - 1, readDb
							.getBigIntRecordGroup(readDh, endGroup
									* conf.recordGroupByteLength));
			}
			// assert Util.debug(DebugFacility.SOLVER, "Loading " + currentGroup
			// + " - " + (currentGroup + writeLen - 1) + " for write");
			game.setState(game.hashToState(start + hashes - 1));
		}
		if (tier < game.numberOfTiers() - 1) {
			children = new ItergameState[game.maxChildren()];
			for (int i = 0; i < children.length; i++) {
				children[i] = new ItergameState();
			}
			if (!directRead) {
				game.lastMoves(children);
				ends = new long[children.length];
				for (int i = 0; i < ends.length; i++) {
					whichPage[i] = -1;
					ends[i] = game.stateToHash(children[i])
							/ conf.recordsPerGroup;
				}
			}
		}
		game.setState(game.hashToState(start));
		for (long count = 0L; count < hashes; count++) {
			if (current % STEP_SIZE == 0)
				t.calculated(STEP_SIZE);
			PrimitiveValue pv = game.primitiveValue();
			switch (pv) {
			case UNDECIDED:
				int len = game.validMoves(children);
				Record r;
				for (int i = 0; i < len; i++) {
					r = vals[i];
					long hash = game.stateToHash(children[i]);
					if (directRead) {
						readDb.getRecord(readDh, hash, r);
					} else {
						long hashGroup = hash / conf.recordsPerGroup;
						int col = game.openColumn[i];
						if (whichPage[col] == -1
								|| hashGroup >= childPages[whichPage[col]].firstGroup
										+ childPages[whichPage[col]].numGroups) {
							long pageStart = hashGroup, pageEnd = ends[col];
							if (strainingMemory) {
								double marginUsed = (pageEnd - pageStart)
										/ prevToCurFraction
										* conf.recordsPerGroup / hashes;
								if (whichPage[col] < 0) {
									marginVarSum += (marginUsed - 1)
											* (marginUsed - 1);
									++timesUsed;
								}
								if (marginUsed > SAFETY_MARGIN) {
									pageEnd = pageStart
											+ (long) (SAFETY_MARGIN
													* prevToCurFraction
													* hashes / conf.recordsPerGroup);
									assert Util
											.debug(
													DebugFacility.SOLVER,
													"Exceeded page size limit by: "
															+ DecimalFormat
																	.getPercentInstance()
																	.format(
																			marginUsed
																					/ SAFETY_MARGIN
																					- 1));
								}
							}
							if (whichPage[col] >= 0) {
								int c;
								for (c = 0; c < whichPage.length; c++) {
									if (whichPage[c] == whichPage[col]
											&& c != col)
										break;
								}
								if (c == whichPage.length)
									childPages[whichPage[col]] = null;
								else
									whichPage[col] = -1;
							}
							int lowPage = -1;
							ArrayList<Page> pageList = new ArrayList<Page>(game
									.maxChildren());
							for (int c = 0; c < numPages; c++) {
								if (childPages[c] == null)
									continue;
								long firstGroup = childPages[c].firstGroup;
								long lastGroup = firstGroup
										+ childPages[c].numGroups - 1;
								if (firstGroup < pageEnd
										&& lastGroup > pageStart) {
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
								if (whichPage[col] < 0)
									lowPage = numPages++;
								else
									lowPage = whichPage[col];
								childPages[lowPage] = new Page(conf);
								childPages[lowPage].loadPage(readDb, readDh,
										pageStart,
										(int) (pageEnd - pageStart) + 1);
							}
							Page thePage = childPages[lowPage];
							if (thePage.firstGroup <= pageStart) {
								pageStart = thePage.firstGroup;
								thePage
										.ensureCapacity((int) (pageEnd - pageStart) + 1);
							} else {
								thePage.extendDown(readDb, readDh, hashGroup,
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
								thePage.extendUp(readDb, readDh, nextPage);
							}
							if (thePage.firstGroup + thePage.numGroups <= pageEnd)
								thePage.extendUp(readDb, readDh, pageEnd);
							whichPage[col] = lowPage;
							for (int c = 0; c < whichPage.length; c++) {
								if (whichPage[c] >= 0
										&& childPages[whichPage[c]] == null)
									whichPage[c] = lowPage;
							}
						}
						childPages[whichPage[col]].getRecord(hashGroup,
								(int) (hash % conf.recordsPerGroup), r);
					}
					r.previousPosition();
				}
				Record newVal = game.combine(vals, 0, len);
				if (directRead)
					writeDb.putRecord(writeDh, current, newVal);
				else
					writePage.putRecord(currentGroup, currentNum, newVal);
				break;
			case IMPOSSIBLE:
				prim.value = PrimitiveValue.TIE;
				if (directRead)
					writeDb.putRecord(writeDh, current, prim);
				else
					writePage.putRecord(currentGroup, currentNum, prim);
				break;
			default:
				if (hasRemoteness)
					prim.remoteness = 0;
				prim.value = pv;
				if (directRead)
					writeDb.putRecord(writeDh, current, prim);
				else
					writePage.putRecord(currentGroup, currentNum, prim);
			}
			if (count < hashes - 1)
				game.nextHashInTier();
			current++;
			if (!directRead) {
				currentNum++;
				if (currentNum == conf.recordsPerGroup) {
					currentNum = 0;
					currentGroup++;
				}
			}
		}
		if (strainingMemory && children != null)
			for (int col = 0; col < children.length; col++) {
				if (whichPage[col] < 0) {
					++timesUsed;
					++marginVarSum;
				}
			}
		if (!directRead) {
			// assert Util.debug(DebugFacility.SOLVER, "Writing "
			// + writePage.firstGroup + " - "
			// + (writePage.firstGroup + writePage.numGroups - 1));
			writePage.writeBack(writeDb, writeDh);
		}
	}

	@Override
	public void initialize(Configuration conf) {
		super.initialize(conf);
		directRead = conf.getBoolean("gamesman.solver.directRead", false);
	}
}
