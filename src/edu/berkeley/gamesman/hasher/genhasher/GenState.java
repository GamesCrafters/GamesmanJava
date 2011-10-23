package edu.berkeley.gamesman.hasher.genhasher;

import java.util.Arrays;

import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.util.Util;

/**
 * @author dnspies
 * 
 */
public class GenState implements State {
	/**
	 * 
	 */
	private final int[] sequence;
	/**
	 * 
	 */
	public final int digBase;
	/**
	 * 
	 */
	private int startPoint;

	private GenHasher<?> myHasher;

	/**
	 * @param myHasher
	 */
	public GenState(GenHasher<?> myHasher) {
		this.sequence = new int[myHasher.numElements];
		this.digBase = myHasher.digitBase;
		this.myHasher = myHasher;
	}

	@Override
	public final void set(State s) {
		set((GenState) s);
	}

	// Should not access any non-final methods which might modify s
	/**
	 * @param s
	 */
	public final void set(GenState s) {
		assert s.myHasher == myHasher;
		setOther(s);
	}

	protected final void setOther(GenState s) {
		setSeq(s.sequence, s.startPoint);
		matchSeq(s);
		assert s.startPoint == startPoint;
		assert !GenHasher.useToughAsserts()
				|| Util.arraysEquals(s.sequence, startPoint, sequence,
						startPoint, sequence.length - startPoint);
	}

	/**
	 * @param othersequence
	 * @param startPoint
	 */
	final void set(int[] othersequence, int startPoint) {
		setSeq(othersequence, startPoint);
		matchSeq();
		assert this.startPoint == startPoint;
		assert !GenHasher.useToughAsserts()
				|| Util.arraysEquals(othersequence, startPoint, sequence,
						startPoint, sequence.length - startPoint);
	}

	protected void matchSeq() {
	}

	protected void matchSeq(GenState other) {
		matchSeq();
	}

	private final void setSeq(int[] othersequence, int startPoint) {
		assert sequence.length == othersequence.length;
		System.arraycopy(othersequence, startPoint, sequence, startPoint,
				sequence.length - startPoint);
		this.startPoint = startPoint;
	}

	/**
	 * @param dir
	 * @return
	 */
	protected boolean incr(int dir) {
		assert dir == 1 || dir == -1;
		sequence[startPoint] += dir;
		if (sequence[startPoint] < 0 || sequence[startPoint] >= digBase) {
			return false;
		} else
			return true;
	}

	/**
	 * 
	 */
	protected void trunc() {
		assert startPoint < sequence.length;
		startPoint++;
	}

	/**
	 * @param startAt
	 */
	protected void trunc(int startAt) {
		assert startPoint <= startAt;
		startPoint = startAt;
	}

	/**
	 * @param startHigh
	 */
	final void addOn(boolean startHigh) {
		addLS(startHigh ? digBase - 1 : 0);
	}

	protected void addLS(int ls) {
		startPoint--;
		sequence[startPoint] = ls;
	}

	/**
	 * @param n
	 */
	protected final void setLS(int n) {
		set(getStart(), n);
	}

	/**
	 * @return
	 */
	protected final int leastSig() {
		return sequence[startPoint];
	}

	final void resetLS(boolean startHigh) {
		setLS(startHigh ? digBase - 1 : 0);
	}

	protected void clear() {
		startPoint = sequence.length;
	}

	/**
	 * @return
	 */
	protected final boolean isEmpty() {
		return startPoint == sequence.length;
	}

	/**
	 * @return
	 */
	final boolean isComplete() {
		return startPoint == 0;
	}

	/**
	 * @param place
	 * @return
	 */
	public final int get(int place) {
		return sequence[place];
	}

	/**
	 * @return
	 */
	protected final int getStart() {
		return startPoint;
	}

	@Override
	public final boolean equals(Object other) {
		if (other instanceof GenState) {
			return Arrays.equals(sequence, ((GenState) other).sequence);
		} else
			return false;
	}

	@Override
	public final int hashCode() {
		return Arrays.hashCode(sequence);
	}

	/**
	 * @return
	 */
	public final int[] cloneSequence() {
		return sequence.clone();
	}

	public final int numElements() {
		return sequence.length;
	}

	protected void set(int place, int val) {
		sequence[place] = val;
	}

	protected final boolean validLS() {
		return isEmpty() || leastSig() >= 0 && leastSig() < digBase;
	}

	public final boolean matches(GenState other, int off) {
		return sequence.length == other.sequence.length
				&& Util.arraysEquals(sequence, off, other.sequence, off,
						sequence.length - off);
	}

	@Override
	public final String toString() {
		String[] starSeq = new String[sequence.length];
		for (int i = 0; i < startPoint; i++) {
			starSeq[i] = "*";
		}
		for (int i = startPoint; i < sequence.length; i++) {
			starSeq[i] = Integer.toString(sequence[i]);
		}
		return Arrays.toString(starSeq);
	}

	public final boolean hasHasher(GenHasher h) {
		return h == myHasher;
	}
}
