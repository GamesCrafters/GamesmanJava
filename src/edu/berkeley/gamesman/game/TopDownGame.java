package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.qll.Factory;
import edu.berkeley.gamesman.util.qll.Pool;
import edu.berkeley.gamesman.util.qll.QuickLinkedList;

/**
 * A wrapper for using the top-down solver with any game
 * 
 * @author dnspies
 * 
 * @param <S>
 *            The game state
 */
public final class TopDownGame<S extends State> extends TopDownMutaGame {
	private final Game<S> myGame;
	private final QuickLinkedList<SIterator> moveLists = new QuickLinkedList<SIterator>();
	private final Pool<SIterator> moveArrayPool = new Pool<SIterator>(
			new Factory<SIterator>() {

				public SIterator newObject() {
					return new SIterator();
				}

				public void reset(SIterator t) {
					t.currentPlace = t.numPlaces = 0;
				}

			});
	private final QuickLinkedList<S> stateList = new QuickLinkedList<S>();
	private final Pool<S> statePool = new Pool<S>(new Factory<S>() {

		public S newObject() {
			return myGame.newState();
		}

		public void reset(S t) {
		}

	});
	private final S[] startingPositions;

	private class SIterator implements Iterator<S> {

		private final S[] stateArray;
		private int currentPlace = 0;
		private int numPlaces = 0;

		public SIterator() {
			this.stateArray = myGame.newStateArray(myGame.maxChildren());
		}

		@Override
		public boolean hasNext() {
			return currentPlace < numPlaces;
		}

		@Override
		public S next() throws NoSuchElementException {
			if (hasNext())
				return stateArray[currentPlace++];
			throw new NoSuchElementException();
		}

		@Override
		public void remove() throws UnsupportedOperationException {
			throw new UnsupportedOperationException();
		}

		private void setToValidMoves() {
			numPlaces = myGame.validMoves(stateList.getFirst(), stateArray);
			currentPlace = 0;
		}
	}

	/**
	 * @param g
	 *            The game to wrap
	 */
	public TopDownGame(Game<S> g) {
		super(g.conf);
		myGame = g;
		stateList.push(statePool.get());
		Collection<S> startingPositions = myGame.startingPositions();
		this.startingPositions = startingPositions.toArray(myGame
				.newStateArray(startingPositions.size()));
	}

	@Override
	public boolean changeMove() {
		if (!moveLists.getFirst().hasNext())
			return false;
		S m = moveLists.getFirst().next();
		stateList.getFirst().set(m);
		return true;
	}

	@Override
	public String displayState() {
		return myGame.displayState(stateList.getFirst());
	}

	@Override
	public long getHash() {
		return myGame.stateToHash(stateList.getFirst());
	}

	@Override
	public int makeMove() {
		SIterator moves = moveArrayPool.get();
		moves.setToValidMoves();
		moveLists.push(moves);
		if (moves.hasNext()) {
			S curState = moves.next();
			S newState = statePool.get();
			newState.set(curState);
			stateList.push(newState);
			return moves.numPlaces;
		} else {
			moveArrayPool.release(moveLists.pop());
			return 0;
		}
	}

	@Override
	public Value primitiveValue() {
		return myGame.primitiveValue(stateList.getFirst());
	}

	@Override
	public void setFromString(String pos) {
		setToState(myGame.stringToState(pos));
	}

	@Override
	public void setToHash(long hash) {
		clearLists();
		S state = statePool.get();
		myGame.hashToState(hash, state);
		stateList.push(state);
	}

	private void setToState(S pos) {
		clearLists();
		S state = statePool.get();
		state.set(pos);
		stateList.push(state);
	}

	private void clearLists() {
		while (!stateList.isEmpty()) {
			statePool.release(stateList.pop());
		}
		while (!moveLists.isEmpty()) {
			moveArrayPool.release(moveLists.pop());
		}
	}

	@Override
	public void undoMove() {
		moveArrayPool.release(moveLists.pop());
		statePool.release(stateList.pop());
	}

	@Override
	public Collection<String> moveNames() {
		Collection<Pair<String, S>> validMoves = myGame.validMoves(stateList
				.getFirst());
		ArrayList<String> moveNames = new ArrayList<String>(validMoves.size());
		for (Pair<String, S> move : validMoves) {
			moveNames.add(move.car);
		}
		return moveNames;
	}

	@Override
	public int numStartingPositions() {
		return startingPositions.length;
	}

	@Override
	public void setStartingPosition(int i) {
		setToState(startingPositions[i]);
	}

	@Override
	public void longToRecord(long record, Record toStore) {
		myGame.longToRecord(stateList.getFirst(), record, toStore);
	}

	@Override
	public long recordToLong(Record fromRecord) {
		return myGame.recordToLong(stateList.getFirst(), fromRecord);
	}

	@Override
	public int maxChildren() {
		return myGame.maxChildren();
	}

	@Override
	public String describe() {
		return myGame.describe();
	}

	@Override
	public long numHashes() {
		return myGame.numHashes();
	}

	@Override
	public long recordStates() {
		return myGame.recordStates();
	}
}
