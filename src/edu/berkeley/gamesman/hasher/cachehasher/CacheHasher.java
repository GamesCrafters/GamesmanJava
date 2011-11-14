package edu.berkeley.gamesman.hasher.cachehasher;

import java.util.Arrays;
import java.util.Collections;

import edu.berkeley.gamesman.hasher.genhasher.GenHasher;
import edu.berkeley.gamesman.hasher.genhasher.GenState;

/**
 * @author dnspies
 * 
 * @param <S>
 */
public final class CacheHasher<S extends GenState> {
	private final GenHasher<S> mainHasher;
	private final S myState;
	private final long[] lastChange;
	private final boolean moveIsExact;
	private final ChildInfo[] moves;
	private long hash;

	private class ChildInfo {
		ChildInfo(GenHasher<S> childHasher, CacheMove move) {
			this.childHasher = childHasher;
			this.move = move;
			this.childState = childHasher.newState();
			this.lastMain = -1L;
		}

		final GenHasher<S> childHasher;
		final CacheMove move;
		final S childState;
		long cshash;
		long moveDiff;
		long lastMain;

		@Override
		public String toString() {
			return move.toString() + ":" + childState.toString();
		}
	}

	/**
	 * @param mainHasher
	 * @param childHashers
	 * @param allMoves
	 * @param exactMove
	 * @param childHasher
	 */
	public CacheHasher(GenHasher<S> mainHasher, GenHasher<S>[] childHashers,
			CacheMove[] allMoves, boolean exactMove) {
		this.mainHasher = mainHasher;
		this.myState = mainHasher.newState();
		moves = new CacheHasher.ChildInfo[allMoves.length];
		assert childHashers.length == allMoves.length;
		for (int i = 0; i < allMoves.length; i++) {
			moves[i] = new ChildInfo(childHashers[i], allMoves[i]);
		}
		this.moveIsExact = exactMove;
		lastChange = new long[mainHasher.numElements];
	}

	/**
	 * @param mainHasher
	 * @param childHasher
	 * @param allMoves
	 * @param exactMove
	 */
	public CacheHasher(GenHasher<S> mainHasher, GenHasher<S> childHasher,
			CacheMove[] allMoves, boolean exactMove) {
		this(mainHasher, Collections.nCopies(allMoves.length, childHasher)
				.toArray((GenHasher<S>[]) new GenHasher[allMoves.length]),
				allMoves, exactMove);
	}

	/**
	 * Iterates to the next arrangement (if possible)
	 * 
	 * @return The index of the first element unchanged or -1 if there are no
	 *         more
	 */
	public int next() {
		S tempState = null;
		if (GenHasher.useToughAsserts()) {
			tempState = mainHasher.getPoolState();
			tempState.set(myState);
		}
		int place = mainHasher.step(myState);
		hash++;
		if (place == -1)
			return -1;
		Arrays.fill(lastChange, 0, place, hash);
		if (GenHasher.useToughAsserts()) {
			assert tempState.matches(myState, place + 1);
			assert mainHasher.hash(myState) == hash;
		}
		return place;
	}

	/**
	 * Sets this hasher to match the passed array and returns the hash. Throws
	 * an error if the number of each type does not match those specified in the
	 * last call to setNums
	 * 
	 * @param state
	 *            The state to match
	 * @return The hash of this arrangement
	 */
	public long hash(S state) {
		myState.set(state);
		setPosition();
		assert hash >= 0 && hash < numHashes();
		return hash;
	}

	private void setPosition() {
		hash = mainHasher.hash(myState);
		for (int i = 0; i < mainHasher.numElements; i++)
			lastChange[i] = hash;
		for (ChildInfo ci : moves)
			ci.lastMain = -1L;
	}

	/**
	 * Sets this hasher to match the specified hash
	 * 
	 * @param hash
	 *            The hash to set this to
	 */
	public void unhash(long hash) {
		assert hash >= 0 && hash < numHashes();
		mainHasher.unhash(hash, myState);
		setPosition();
		assert hash == this.hash;
	}

	/**
	 * @return The total number of possible rearrangements for the last call to
	 *         setNums
	 */
	public long numHashes() {
		return mainHasher.totalPositions();
	}

	/**
	 * @param whichChildren
	 * @param childArray
	 * @return
	 */
	public int getChildren(int[] whichChildren, long[] childArray) {
		int numChildren = 0;
		for (int i = 0; i < moves.length; i++) {
			ChildInfo ci = moves[i];
			if (ci.move.nextStep(myState) == -1) {
				if (whichChildren != null)
					whichChildren[numChildren] = i;
				childArray[numChildren] = getChild(ci);
				numChildren++;
			}
		}
		return numChildren;
	}

	/**
	 * @param whichChild
	 * @return
	 */
	public long getChild(int whichChild) {
		return getChild(moves[whichChild]);
	}

	private long getChild(ChildInfo ci) {
		if (ci.lastMain == -1L) {
			ci.childHasher.makeMove(myState, ci.move, ci.childState);
			ci.lastMain = hash;
			ci.cshash = ci.childHasher.hash(ci.childState);
			ci.moveDiff = ci.cshash - hash;
			return ci.cshash;
		} else if (moveIsExact && ci.lastMain >= lastChange[ci.move.minPlace]) {
			if (GenHasher.useToughAsserts()) {
				S cstate = ci.childHasher.getPoolState();
				ci.childHasher.makeMove(myState, ci.move, cstate);
				assert ci.childHasher.hash(cstate) == hash + ci.moveDiff;
				ci.childHasher.release(cstate);
			}
			return hash + ci.moveDiff;
		} else {
			int same = (moveIsExact ? ci.move.minPlace : 0) + 1;
			while (same < mainHasher.numElements) {
				if (lastChange[same] > ci.lastMain)
					same++;
				else
					break;
			}
			long diff = ci.childHasher.stepTo(ci.childState, mainHasher,
					myState, ci.move, same, 1);
			ci.cshash += diff;
			ci.lastMain = hash;
			ci.moveDiff = ci.cshash - hash;
			assert correctHash(ci);
			return ci.cshash;
		}
	}

	private boolean correctHash(ChildInfo ci) {
		if (!GenHasher.useToughAsserts())
			return true;
		return ci.cshash == ci.childHasher.hash(ci.childState);
	}

	/**
	 * @return
	 */
	public long getHash() {
		return hash;
	}

	/**
	 * @param state
	 */
	public void getState(S state) {
		state.set(myState);
	}

	public S newState() {
		return mainHasher.newState();
	}

	public int get(int i) {
		return myState.get(i);
	}

	public boolean hasNext() {
		return hash < mainHasher.totalPositions() - 1;
	}

	public void set(S state, int[] seq) {
		mainHasher.set(state, seq);
	}

	public void set(int[] seq) {
		set(myState, seq);
		setPosition();
	}

	public long boundNextChild(int whichChild, int dir) {
		return boundNextChild(moves[whichChild], dir);
	}

	private long boundNextChild(ChildInfo ci, int dir) {
		return ci.childHasher.getChildBound(mainHasher, myState, ci.move, dir);
	}
}
