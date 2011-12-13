package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.qll.Factory;
import edu.berkeley.gamesman.util.qll.Pool;
import edu.berkeley.gamesman.util.qll.QuickLinkedList;

/**
 * A wrapper for local loopy games in order to make them appear as
 * LoopyMutaGames
 * 
 * @param <S>
 *            The boardstate type this game take
 */
public final class LoopyGameWrapper<S extends State<S>> extends LoopyMutaGame {
	private final Game<S> myGame;
	private final Undoable<S> myGameLoopy;
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
	private final QuickLinkedList<QuickLinkedList<S>> moveLists;
	private final QuickLinkedList<QuickLinkedList<S>> parentLists;
	private final QuickLinkedList<S> stateList;
	private final S[] possibleMoves;
	private final S[] possibleParents;
	private final S[] startingPositions;

	/**
	 * Wraps the passed game
	 * 
	 * @param conf The configuration object
	 * @param g The game
	 */
	@SuppressWarnings("unchecked")
	public LoopyGameWrapper(Configuration conf, Game<S> g) {
		super(conf);
		if (!(g instanceof Undoable)) {
			throw new Error("Can only wrap games which implement Undoable");
		}
		myGame = g;
		myGameLoopy = (Undoable<S>) g;
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
		if (parentLists.getFirst().isEmpty())
			return false;
		S m = parentLists.getFirst().removeFirst();
		stateList.getFirst().set(m);
		myGame.release(m);
		return true;
	}

	@Override
	public void remakeMove() {
		QuickLinkedList<S> parentList = parentLists.pop();
		stateSetPool.release(parentList);
		myGame.release(stateList.pop());
	}

	@Override
	public int unmakeMove() {
		QuickLinkedList<S> parents = stateSetPool.get();
		int numParents = myGameLoopy.possibleParents(stateList.getFirst(),
				possibleParents);
		parentLists.push(parents);
		if (numParents == 0) {
			stateSetPool.release(parentLists.pop());
			return 0;
		} else {
			for (int i = 0; i < numParents; i++) {
				S parent = myGame.getPoolState();
				parent.set(possibleParents[i]);
				parents.add(parent);
			}
			S parent = parents.removeFirst();
			stateList.push(parent);
			return numParents;
		}
	}

	@Override
	public boolean changeMove() {
		if (moveLists.getFirst().isEmpty())
			return false;
		S m = moveLists.getFirst().removeFirst();
		stateList.getFirst().set(m);
		myGame.release(m);
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
	public void longToRecord(long record, Record toStore) {
		myGame.longToRecord(stateList.getFirst(), record, toStore);

	}

	@Override
	public int makeMove() {
		QuickLinkedList<S> moves = stateSetPool.get();
		moveLists.push(moves);
		int numMoves = myGame.validMoves(stateList.getFirst(), possibleMoves);
		if (numMoves == 0) {
			stateSetPool.release(moveLists.pop());
			return 0;
		} else {
			for (int i = 0; i < numMoves; i++) {
				S move = myGame.getPoolState();
				move.set(possibleMoves[i]);
				moves.add(move);
			}
			S curState = myGame.getPoolState();
			S move = moves.removeFirst();
			curState.set(move);
			stateList.push(curState);
			myGame.release(move);
			return numMoves;
		}
	}

	@Override
	public List<String> moveNames() {
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
	public Value primitiveValue() {
		return myGame.primitiveValue(stateList.getFirst());
	}

	@Override
	public Value strictPrimitiveValue() {
		return myGame.strictPrimitiveValue(stateList.getFirst());
	}

	@Override
	public long recordToLong(Record fromRecord) {
		return myGame.recordToLong(stateList.getFirst(), fromRecord);
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
		S state = myGame.getPoolState();
		state.set(pos);
		stateList.push(state);
	}

	@Override
	public void setToHash(long hash) {
		S state = myGame.getPoolState();
		myGame.hashToState(hash, state);
		setToState(state);
		myGame.release(state);
	}

	@Override
	public void undoMove() {
		stateSetPool.release(moveLists.pop());
		myGame.release(stateList.pop());
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

	@Override
	public String toString() {
		return myGame.displayState(stateList.getFirst());
	}
}
