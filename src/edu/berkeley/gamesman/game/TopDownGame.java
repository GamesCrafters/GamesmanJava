package edu.berkeley.gamesman.game;

import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Game;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.qll.Factory;
import edu.berkeley.gamesman.util.qll.RLLFactory;
import edu.berkeley.gamesman.util.qll.RecycleLinkedList;

/**
 * A wrapper for using the top-down solver with any game
 * 
 * @author dnspies
 * 
 * @param <S>
 *            The game state
 */
public final class TopDownGame<S extends State> extends TopDownMutaGame<S> {
	private final Game<S> myGame;
	private RecycleLinkedList<RecycleLinkedList<S>> moveLists;
	private RecycleLinkedList<S> stateList;
	private S[] possibleMoves;

	/**
	 * @param g
	 *            The game to wrap
	 */
	public TopDownGame(Game<S> g) {
		myGame = g;
		moveLists = new RecycleLinkedList<RecycleLinkedList<S>>(
				new Factory<RecycleLinkedList<S>>() {
					RLLFactory<S> gen = new RLLFactory<S>(new Factory<S>() {

						public S newObject() {
							return newState();
						}

						public void reset(S t) {
						}
					});

					public RecycleLinkedList<S> newObject() {
						return gen.getList();
					}

					public void reset(RecycleLinkedList<S> t) {
						t.clear();
					}

				});
		stateList = new RecycleLinkedList<S>(new Factory<S>() {

			public S newObject() {
				return newState();
			}

			public void reset(S t) {
			}

		});
		stateList.add();
		possibleMoves = myGame.newStateArray(myGame.maxChildren());
	}

	@Override
	public boolean changeMove() {
		if (moveLists.getLast().isEmpty())
			return false;
		S m = moveLists.getLast().removeFirst();
		stateList.getLast().set(m);
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
	public S getState() {
		return stateList.getLast();
	}

	@Override
	public boolean makeMove() {
		RecycleLinkedList<S> moves = moveLists.addLast();
		int numMoves = myGame.validMoves(stateList.getLast(), possibleMoves);
		if (numMoves == 0) {
			moveLists.removeLast();
			return false;
		} else {
			for (int i = 0; i < numMoves; i++) {
				S move = moves.add();
				move.set(possibleMoves[i]);
			}
			S curState = stateList.addLast();
			curState.set(moves.removeFirst());
			return true;
		}
	}

	@Override
	public PrimitiveValue primitiveValue() {
		return myGame.primitiveValue(stateList.getLast());
	}

	@Override
	public void setFromString(String pos) {
		setToState(myGame.stringToState(pos));
	}

	@Override
	public void setToHash(long hash) {
		stateList.clear();
		S state = stateList.add();
		myGame.hashToState(hash, state);
	}

	@Override
	public void setToState(S pos) {
		stateList.clear();
		S state = stateList.add();
		state.set(pos);
	}

	@Override
	public void undoMove() {
		moveLists.removeLast();
		stateList.removeLast();
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
	public S newState() {
		return myGame.newState();
	}

	@Override
	public long numHashes() {
		return myGame.numHashes();
	}

	@Override
	public Collection<S> startingPositions() {
		return myGame.startingPositions();
	}

	@Override
	public void initialize(Configuration conf) {
		myGame.initialize(conf);
	}

	@Override
	public int validMoves(S pos, S[] children) {
		return myGame.validMoves(pos, children);
	}

	@Override
	public S doMove(S pos, String move) {
		return myGame.doMove(pos, move);
	}

	@Override
	public int primitiveScore(S pos) {
		return myGame.primitiveScore(pos);
	}

	@Override
	public int getPlayerCount() {
		return myGame.getPlayerCount();
	}

	@Override
	public String displayHTML(S pos) {
		return myGame.displayHTML(pos);
	}

	@Override
	public Record combine(Record[] recordArray, int offset, int len) {
		return myGame.combine(recordArray, offset, len);
	}

	@Override
	public Record newRecord(PrimitiveValue pv) {
		return myGame.newRecord(pv);
	}

	@Override
	public Record newRecord() {
		return myGame.newRecord();
	}

	@Override
	public Record newRecord(long val) {
		return myGame.newRecord(val);
	}

	@Override
	public long recordStates() {
		return myGame.recordStates();
	}

	@Override
	public void setInterperet(long recNum) {
		myGame.setInterperet(recNum);
	}

	@Override
	public String displayState(S pos) {
		return myGame.displayState(pos);
	}

	@Override
	public void hashToState(long hash, S s) {
		myGame.hashToState(hash, s);
	}

	@Override
	public PrimitiveValue primitiveValue(S pos) {
		return myGame.primitiveValue(pos);
	}

	@Override
	public long stateToHash(S pos) {
		return myGame.stateToHash(pos);
	}

	@Override
	public String stateToString(S pos) {
		return myGame.stateToString(pos);
	}

	@Override
	public S stringToState(String pos) {
		return myGame.stringToState(pos);
	}

	@Override
	public Collection<Pair<String, S>> validMoves(S pos) {
		return myGame.validMoves(pos);
	}
}
