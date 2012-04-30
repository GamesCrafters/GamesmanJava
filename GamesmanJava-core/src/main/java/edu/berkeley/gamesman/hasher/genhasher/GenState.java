package edu.berkeley.gamesman.hasher.genhasher;

import java.util.Arrays;

import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.util.Util;

/**
 * A general state for the GenHashers framework. If you subclass this with a
 * state which memoizes information as it changes, the following methods change
 * the state:<br />
 * 
 * addLS(int) decrements start by 1 and sets the new start to the int parameter<br />
 * clear() sets start to the sequence length so all pieces are starred<br />
 * incr(int) causes the ls to go up by some amount but not past the next valid
 * position (possibly exceeding the digit-base by one)<br />
 * matchSeq() asks all other information to reset to match the new valid
 * sequence (in other words anything might have changed)<br />
 * set(int,int) makes the piece at pos match the passed value<br />
 * trunc() increments start by one and throws off the bottom piece<br />
 * trunc(int) increments the star to int and throws off all the bottom pieces
 * below it (some subclasses override this with repeated calls to trunc, be
 * careful not to double-update)<br />
 * 
 * @author dnspies
 */
public class GenState implements State<GenState>, Comparable<GenState> {
	/**
	 * The sequence representing this board-state
	 */
	private final int[] sequence;
	/**
	 * Imagine everything from 0 to startPoint-1 is starred out
	 */
	private int startPoint;

	private GenHasher<?> myHasher;

	/**
	 * This constructor should only be called from within the hasher method
	 * newState(). Otherwise everything else should call hasher.newState()
	 * 
	 * @param myHasher
	 *            The hasher corresponding to this state
	 */
	public GenState(GenHasher<?> myHasher) {
		this.sequence = new int[myHasher.numElements];
		this.myHasher = myHasher;
	}

	// Should not access any non-final methods which might modify s
	@Override
	public final void set(GenState s) {
		assert s.myHasher == myHasher;
		setOther(s);
	}

	protected final void setOther(GenState s) {
		setSeq(s.sequence, s.startPoint);
		matchSeq();
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
		return validLS();
	}

	/**
	 * 
	 */
	protected void trunc() {
		assert startPoint < sequence.length;
		startPoint++;
	}

	/**
	 * WARNING: Do not change thrown-away values. GenHasher actually uses them
	 * in certain situations.
	 * 
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
		addLS(startHigh ? myHasher.baseFor(startPoint - 1) - 1 : 0);
	}

	final void unsafeAdd() {
		addLS(sequence[startPoint - 1]);
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
		setLS(startHigh ? lsBase() - 1 : 0);
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
	protected final boolean isComplete() {
		return startPoint == 0;
	}

	/**
	 * @param place
	 * @return
	 */
	public final int get(int place) {
		assert startPoint <= place;
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
		return isEmpty() || leastSig() >= 0 && leastSig() < lsBase();
	}

	private int lsBase() {
		return myHasher.baseFor(startPoint);
	}

	public final boolean matches(GenState other, int off) {
		return sequence.length == other.sequence.length
				&& Util.arraysEquals(sequence, off, other.sequence, off,
						sequence.length - off);
	}

	@Override
	public String toString() {
		String[] starSeq = new String[sequence.length];
		for (int i = 0; i < startPoint; i++) {
			starSeq[sequence.length - 1 - i] = "*";
		}
		for (int i = startPoint; i < sequence.length; i++) {
			starSeq[sequence.length - 1 - i] = Integer.toString(sequence[i]);
		}
		return Arrays.toString(starSeq);
	}

	public final boolean hasHasher(GenHasher<?> h) {
		return h == myHasher;
	}

	@Override
	public int compareTo(GenState o) {
		if (sequence.length != o.sequence.length)
			return sequence.length - o.sequence.length;
		else {
			int maxPoint = Math.max(startPoint, o.startPoint);
			for (int i = sequence.length - 1; i >= maxPoint; i--) {
				if (sequence[i] != o.sequence[i])
					return sequence[i] - o.sequence[i];
			}
			return o.startPoint - startPoint;
			// Yes this is correct, larger start point = smaller suffix
		}
	}

	public final void getSuffix(int[] toFill, int length) {
		assert toFill.length >= length;
		assert sequence.length - startPoint >= length;
		System.arraycopy(sequence, sequence.length - length, toFill, 0, length);
	}
}
