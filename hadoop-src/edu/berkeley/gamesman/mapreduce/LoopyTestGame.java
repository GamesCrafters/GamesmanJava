package edu.berkeley.gamesman.mapreduce;

import java.util.ArrayList;
import java.util.List;

/**
 * <value> = <successors>
 * 1 = 2 5		(W)
 * 2 = 3		(L)
 * 3 = 2 4		(W)
 * 4 = prim L	(L)
 * 5 = 7		(D)
 * 6 = 5		(D)
 * 7 = 6 8		(D)
 * 8 = prim W	(W)
 */
public class LoopyTestGame implements LoopyGame {
	public Iterable<Long> getSuccessors(long hash) {
		List<Long> succ = new ArrayList<Long>();
		switch ((int)hash) {
			case 1: succ.add(2l); succ.add(5l); break;
			case 2: succ.add(3l); break;
			case 3: succ.add(2l); succ.add(4l); break;
			case 5: succ.add(7l); break;
			case 6: succ.add(5l); break;
			case 7: succ.add(6l); succ.add(8l); break;
			default:
				throw new IllegalArgumentException();
		}
		return succ;
	}

	public boolean isPrimitive(long hash) {
		return hash == 4 || hash == 8;
	}

	public int evalPrimitive(long hash) {
		switch ((int)hash) {
			case 4: return Node.LOSE;
			case 8: return Node.WIN;
			default: return -1;
		}
	}

	public Iterable<Long> getStartingPositions() {
		return null;
	}
}
