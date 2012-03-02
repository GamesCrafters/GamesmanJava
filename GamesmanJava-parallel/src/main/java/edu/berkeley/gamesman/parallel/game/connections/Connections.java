package edu.berkeley.gamesman.parallel.game.connections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.Reducer;

import edu.berkeley.gamesman.game.type.GameValue;
import edu.berkeley.gamesman.hasher.genhasher.GenHasher;
import edu.berkeley.gamesman.hasher.genhasher.Move;
import edu.berkeley.gamesman.hasher.genhasher.Moves;
import edu.berkeley.gamesman.parallel.game.connect4.C4Hasher;
import edu.berkeley.gamesman.parallel.game.connect4.C4State;
import edu.berkeley.gamesman.parallel.ranges.MoveWritable;
import edu.berkeley.gamesman.parallel.ranges.Range;
import edu.berkeley.gamesman.parallel.ranges.RangeReducer;
import edu.berkeley.gamesman.parallel.ranges.RangeTree;
import edu.berkeley.gamesman.solve.reader.SolveReader;
import edu.berkeley.gamesman.util.Pair;

public class Connections extends RangeTree<ConnectionsState> implements
SolveReader<ConnectionsState>{
	
	private Move[] myMoves;
	private Move[][] colMoves;
	private C4Hasher myHasher;
	private int width, height, inARow;
	private int gameSize;
	private int suffLen;
	
	public String getString(ConnectionsState position) {
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
	public ConnectionsState getPosition(String board) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Pair<String, ConnectionsState>> getChildren(
			ConnectionsState position) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GameValue getValue(ConnectionsState state) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<ConnectionsState> getStartingPositions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GenHasher<ConnectionsState> getHasher() {
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
}
