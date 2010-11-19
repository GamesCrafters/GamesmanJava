package edu.berkeley.gamesman.game;

import java.util.Collection;
import java.util.ArrayList;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.util.qll.RecycleLinkedList;

import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.qll.Factory;
import edu.berkeley.gamesman.util.qll.Pool;
import edu.berkeley.gamesman.util.qll.QuickLinkedList;
import edu.berkeley.gamesman.util.qll.RLLFactory;

//public class LoopyGameWrapper extends LoopyMutaGame {
public final class LoopyGameWrapper<S extends State> extends LoopyMutaGame {
	private final Game<S> myGame;
	private final LoopyGame<S> myGameLoopy;
	private final Pool<QuickLinkedList<S>> stateSetPool = new Pool<QuickLinkedList<S>>(
			new Factory<QuickLinkedList<S>>() {

				@Override
				public QuickLinkedList<S> newObject() {
					return new QuickLinkedList<S>();
				}

				@Override
				public void reset(QuickLinkedList<S> t) {
					t.clear();
				}

			});
	private final Pool<S> statePool = new Pool<S>(new Factory<S>() {

		@Override
		public S newObject() {
			return myGame.newState();
		}

		@Override
		public void reset(S t) {
		}

	});
	private final QuickLinkedList<QuickLinkedList<S>> moveLists;
	private final QuickLinkedList<QuickLinkedList<S>> parentLists;
	private final QuickLinkedList<S> stateList;
	private final S[] possibleMoves;
	private final S[] possibleParents;
	private final S[] startingPositions;

	@SuppressWarnings("unchecked")
	public LoopyGameWrapper(Configuration conf, Game<S> g) {
		super(conf);
		if (!(g instanceof LoopyGame<?>)) {
			throw new Error("Can only wrap Loopy games");
		}
		myGame = g;
		myGameLoopy = (LoopyGame<S>) g;
		moveLists = new QuickLinkedList<QuickLinkedList<S>>();
		parentLists = new QuickLinkedList<QuickLinkedList<S>>();
		stateList = new QuickLinkedList<S>();
		stateList.push(g.newState());
		possibleMoves = myGame.newStateArray(myGame.maxChildren());
		possibleParents = myGame.newStateArray(myGameLoopy.maxParents());
		Collection<S> startingPositions = myGame.startingPositions();
		this.startingPositions = startingPositions.toArray(myGame
				.newStateArray(startingPositions.size()));
	}

	@Override
	public boolean changeUnmakeMove() {
		if (parentLists.getLast().isEmpty())
			return false;
		S m = parentLists.getLast().removeFirst();
		stateList.getLast().set(m);
		statePool.release(m);
		return true;
	}

	@Override
	public void remakeMove() {
		QuickLinkedList<S> parentList = parentLists.pop();
		stateSetPool.release(parentList);
		statePool.release(stateList.pop());
	}

	@Override
	public int unmakeMove() {
		QuickLinkedList<S> parents = stateSetPool.get();
		parentLists.push(parents);
		int numParents = myGameLoopy.possibleParents(stateList.getLast(),
				possibleParents);
		if (numParents == 0) {
			stateSetPool.release(parentLists.pop());
			return 0;
		} else {
			for (int i = 0; i < numParents; i++) {
				S parent = statePool.get();
				parents.add(parent);
				parent.set(possibleParents[i]);
			}
			S curState = statePool.get();
			stateList.push(curState);
			S parent = parents.removeFirst();
			curState.set(parent);
			statePool.release(parent);
			return numParents;
		}
	}

	@Override
	public boolean changeMove() {
		if (moveLists.getLast().isEmpty())
			return false;
		S m = moveLists.getLast().removeFirst();
		stateList.getLast().set(m);
		statePool.release(m);
		return true;
	}

	@Override
	public String displayState() {
		return myGame.displayState(stateList.getLast());
	}

	@Override
	public long getHash() {
		return myGame.stateToHash(stateList.getLast());
	}

	@Override
	public void longToRecord(long record, Record toStore) {
		myGame.longToRecord(stateList.getLast(), record, toStore);

	}

	@Override
	public int makeMove() {
		QuickLinkedList<S> moves = stateSetPool.get();
		moveLists.push(moves);
		int numMoves = myGame.validMoves(stateList.getLast(), possibleMoves);
		if (numMoves == 0) {
			stateSetPool.release(moveLists.pop());
			return 0;
		} else {
			for (int i = 0; i < numMoves; i++) {
				S move = statePool.get();
				move.set(possibleMoves[i]);
				moves.add(move);
			}
			S curState = statePool.get();
			S move = moves.removeFirst();
			curState.set(move);
			stateList.push(curState);
			statePool.release(move);
			return numMoves;
		}
	}

	@Override
	public Collection<String> moveNames() {
		Collection<Pair<String, S>> validMoves = myGame.validMoves(stateList
				.getLast());
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
	public Value primitiveValue() {
		return myGame.primitiveValue(stateList.getLast());
	}

	@Override
	public long recordToLong(Record fromRecord) {
		return myGame.recordToLong(stateList.getLast(), fromRecord);
	}

	@Override
	public void setFromString(String pos) {
		setToState(myGame.stringToState(pos));
	}

	@Override
	public void setStartingPosition(int i) {
		setToState(startingPositions[i]);
	}

	private void setToState(S pos) {
		stateList.clear();
		moveLists.clear();
		parentLists.clear();
		S state = statePool.get();
		state.set(pos);
		stateList.push(state);
	}

	@Override
	public void setToHash(long hash) {
		S state = statePool.get();
		myGame.hashToState(hash, state);
		setToState(state);
		statePool.release(state);
	}

	@Override
	public void undoMove() {
		stateSetPool.release(moveLists.pop());
		statePool.release(stateList.pop());
	}

	@Override
	public String describe() {
		return myGame.describe();
	}

	@Override
	public int maxChildren() {
		return myGame.maxChildren();
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
