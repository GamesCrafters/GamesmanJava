package edu.berkeley.gamesman.parallel.game.connections;

import java.util.Collection;
import java.util.Collections;

import edu.berkeley.gamesman.game.type.GameRecord;
import edu.berkeley.gamesman.game.type.GameValue;
import edu.berkeley.gamesman.hasher.DBHasher;
import edu.berkeley.gamesman.hasher.counting.CountingState;
import edu.berkeley.gamesman.hasher.genhasher.GenHasher;
import edu.berkeley.gamesman.hasher.genhasher.Move;
import edu.berkeley.gamesman.parallel.ranges.RangeTree;
import edu.berkeley.gamesman.solve.reader.SolveReader;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.qll.QuickLinkedList;

public class Connections extends RangeTree<CountingState, ConnectionRecord>
		implements SolveReader<CountingState, ConnectionRecord> {
	private Move[] myMoves;
	private Move[][] colMoves;
	private DBHasher myHasher;
	private int width, height, inARow;
	private int gameSize;
	private int suffLen;

	public String getString(CountingState position) {
		StringBuilder sb = new StringBuilder(gameSize);
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				sb.append(charFor(position.get(col * height + row)));
			}
		}
		return sb.toString();
	}

	private Object charFor(int i) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CountingState getPosition(String board) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Pair<String, CountingState>> getChildren(
			CountingState position) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GameValue getValue(CountingState state) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<CountingState> getStartingPositions() {
		CountingState result = myHasher.newState(); // newState() calls the
													// CountingState constructor
		return Collections.singleton(result);
	}

	@Override
	public GenHasher<CountingState> getHasher() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Move[] getMoves() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected int suffixLength() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public GameRecord getRecord(CountingState position,
			ConnectionRecord fetchedRec) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected boolean setNewRecordAndHasChildren(CountingState state,
			ConnectionRecord rec) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean combineValues(QuickLinkedList<ConnectionRecord> grList,
			ConnectionRecord gr) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void previousPosition(ConnectionRecord gr, ConnectionRecord toFill) {
		// TODO Auto-generated method stub

	}

	@Override
	protected Class<ConnectionRecord> getGameRecordClass() {
		// TODO Auto-generated method stub
		return null;
	}
}
