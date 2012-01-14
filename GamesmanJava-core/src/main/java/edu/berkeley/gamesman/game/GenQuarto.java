package edu.berkeley.gamesman.game;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.hasher.genhasher.GenState;
import edu.berkeley.gamesman.hasher.invhasher.InvariantHasher;
import edu.berkeley.gamesman.util.Pair;

public class GenQuarto extends Game<GenQuartoState> {

	public GenQuarto(Configuration conf) {
		super(conf);
	}

	@Override
	public Collection<GenQuartoState> startingPositions() {
		return Collections.singleton(newState());
	}

	@Override
	public Collection<Pair<String, GenQuartoState>> validMoves(
			GenQuartoState pos) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int validMoves(GenQuartoState pos, GenQuartoState[] children) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int maxChildren() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Value primitiveValue(GenQuartoState pos) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long stateToHash(GenQuartoState pos) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String stateToString(GenQuartoState pos) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String displayState(GenQuartoState pos) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GenQuartoState stringToState(String pos) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String describe() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long numHashes() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long recordStates() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void hashToState(long hash, GenQuartoState s) {
		// TODO Auto-generated method stub

	}

	@Override
	public GenQuartoState newState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void longToRecord(GenQuartoState recordState, long record,
			Record toStore) {
		// TODO Auto-generated method stub

	}

	@Override
	public long recordToLong(GenQuartoState recordState, Record fromRecord) {
		// TODO Auto-generated method stub
		return 0;
	}

}

class GenQuartoState extends GenState {
	private final BitSet usedPieces = new BitSet(16);
	private int extraPiece = -1;
	private int numPieces;

	public GenQuartoState(GenQuartoHasher myHasher) {
		super(myHasher);
		for (int i = getStart(); i < 16; i++) {
			super.set(i, 16);
		}
	}

	@Override
	protected void addLS(int ls) {
		super.addLS(ls);
		addUsed(ls);
	}

	@Override
	protected void clear() {
		super.clear();
		clearUsed();
	}

	private void clearUsed() {
		usedPieces.clear();
		extraPiece = -1;
	}

	@Override
	protected boolean incr(int dir) {
		removeUsed(leastSig());
		boolean result = super.incr(dir);
		addUsed(leastSig());
		return result;
	}

	@Override
	protected void matchSeq() {
		clearUsed();
		for (int i = getStart(); i < 16; i++) {
			addUsed(get(i));
		}
	}

	@Override
	protected void set(int place, int val) {
		removeUsed(get(place));
		super.set(place, val);
		addUsed(get(place));
	}

	@Override
	protected void trunc() {
		removeUsed(leastSig());
		super.trunc();
	}

	@Override
	protected void trunc(int startPoint) {
		if (getStart() > startPoint)
			throw new Error("Cannot trunc backwards");
		else {
			while (getStart() < startPoint)
				trunc();
		}
	}

	private void addUsed(int piece) {
		if (piece >= 0 && piece < 16) {
			if (usedPieces.get(piece)) {
				if (extraPiece == -1)
					extraPiece = piece;
				else
					throw new Error("Already invalid");
			} else
				usedPieces.set(piece);
			numPieces++;
		}
	}

	private void removeUsed(int piece) {
		if (piece >= 0 && piece < 16) {
			if (usedPieces.get(piece)) {
				if (extraPiece == piece)
					extraPiece = -1;
				else
					usedPieces.clear(piece);
			} else
				throw new Error("Not set");
			numPieces--;
		}
	}

	public long getInvariant() {
		if (extraPiece == -1) {
			return numPieces;
		} else
			return -1;
	}
}

class GenQuartoHasher extends InvariantHasher<GenQuartoState> {
	public GenQuartoHasher() {
		super(makeArr());
	}

	private static int[] makeArr() {
		int[] arr = new int[16];
		Arrays.fill(arr, 17);
		return arr;
	}

	@Override
	protected long getInvariant(GenQuartoState state) {
		return state.getInvariant();
	}

	@Override
	protected boolean valid(GenQuartoState state) {
		return state.getInvariant() != -1;
	}

	@Override
	protected GenQuartoState innerNewState() {
		return new GenQuartoState(this);
	}

}
