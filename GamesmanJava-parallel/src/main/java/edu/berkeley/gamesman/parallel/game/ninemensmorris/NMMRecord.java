package edu.berkeley.gamesman.parallel.game.ninemensmorris;

import edu.berkeley.gamesman.game.type.GameRecord;
import edu.berkeley.gamesman.game.type.GameValue;
import edu.berkeley.gamesman.propogater.writable.FixedLengthWritable;
import edu.berkeley.gamesman.util.qll.QuickLinkedList;

public class NMMRecord extends GameRecord implements FixedLengthWritable{
	
	public static boolean combineValues(QuickLinkedList<NMMRecord> grList,
			NMMRecord gr) {
		QuickLinkedList<NMMRecord>.QLLIterator iter = grList.iterator();
		try {
			NMMRecord best = null;
			while (iter.hasNext()) {
				NMMRecord next = iter.next();
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
	
	public static GameRecord getRecord(NMMRecord fetchedRec, int tieRemoteness) {
		GameValue gv = fetchedRec.getValue();
		assert gv == GameValue.LOSE || gv == GameValue.WIN;
		return new GameRecord(gv, fetchedRec.getRemoteness());
	}
}


