package edu.berkeley.gamesman.game;

import java.util.Collection;

import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.TierCache;
import edu.berkeley.gamesman.game.util.TierState;
import edu.berkeley.gamesman.util.Pair;

public final class MisereTierGame extends TierGame {
	private final TierGame myGame;
	private final Record tempRecord;

	public MisereTierGame(Game<?> game) {
		super(game.conf);
		myGame = (TierGame) game;
		tempRecord = newRecord();
	}

	@Override
	public void setState(TierState pos) {
		myGame.setState(pos);
	}

	@Override
	public Value primitiveValue() {
		return myGame.primitiveValue().flipValue();
	}

	@Override
	public Collection<Pair<String, TierState>> validMoves() {
		return myGame.validMoves();
	}

	@Override
	public int getTier() {
		return myGame.getTier();
	}

	@Override
	public String stateToString() {
		return myGame.stateToString();
	}

	@Override
	public void setFromString(String pos) {
		myGame.setFromString(pos);
	}

	@Override
	public void getState(TierState state) {
		myGame.getState(state);
	}

	@Override
	public long numHashesForTier(int tier) {
		return myGame.numHashesForTier(tier);
	}

	@Override
	public String displayState() {
		return myGame.displayState();
	}

	@Override
	public void setStartingPosition(int n) {
		myGame.setStartingPosition(n);
	}

	@Override
	public int numStartingPositions() {
		return myGame.numStartingPositions();
	}

	@Override
	public boolean hasNextHashInTier() {
		return myGame.hasNextHashInTier();
	}

	@Override
	public void nextHashInTier() {
		myGame.nextHashInTier();
	}

	@Override
	public int numberOfTiers() {
		return myGame.numberOfTiers();
	}

	@Override
	public int maxChildren() {
		return myGame.maxChildren();
	}

	@Override
	public int validMoves(TierState[] moves) {
		return myGame.validMoves(moves);
	}

	@Override
	public String describe() {
		return myGame.describe();
	}

	@Override
	public long recordStates() {
		return myGame.recordStates();
	}

	@Override
	public void longToRecord(TierState recordState, long record, Record toStore) {
		myGame.longToRecord(recordState, record, toStore);
		toStore.value = toStore.value.flipValue();
	}

	@Override
	public long recordToLong(TierState recordState, Record fromRecord) {
		tempRecord.set(fromRecord);
		tempRecord.value = fromRecord.value.flipValue();
		return myGame.recordToLong(recordState, tempRecord);
	}

	@Override
	public TierCache getCache(Database db, long availableMem) {
		return myGame.getCache(db, availableMem);
	}

	@Override
	public Value strictPrimitiveValue() {
		return myGame.strictPrimitiveValue();
	}

	@Override
	public int validMoves(TierState[] children, int[] cachePlaces) {
		return myGame.validMoves(children, cachePlaces);
	}
}
