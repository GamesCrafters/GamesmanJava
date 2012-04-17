package edu.berkeley.gamesman.parallel;

import edu.berkeley.gamesman.game.type.GameRecord;
import edu.berkeley.gamesman.util.qll.QuickLinkedList;

public final class GameRecordCombiner {
	private GameRecordCombiner() {
	}

	public static boolean combineValues(QuickLinkedList<GameRecord> grList,
			GameRecord gr) {
		QuickLinkedList<GameRecord>.QLLIterator iter = grList.iterator();
		try {
			GameRecord best = null;
			while (iter.hasNext()) {
				GameRecord next = iter.next();
				if (best == null || next.compareTo(best) > 0) {
					best = next;
				}
			}
			if (best == null || gr.equals(best))
				return false;
			else {
				gr.set(best);
				return true;
			}
		} finally {
			grList.release(iter);
		}
	}
}
