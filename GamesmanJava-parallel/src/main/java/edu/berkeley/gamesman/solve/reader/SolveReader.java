package edu.berkeley.gamesman.solve.reader;

import java.util.Collection;

import edu.berkeley.gamesman.propogater.writable.WritableSettableComparable;
import edu.berkeley.gamesman.util.Pair;


public interface SolveReader<KEY extends WritableSettableComparable<KEY>> {
	public abstract KEY getPosition(String board);

	public abstract Collection<Pair<String, KEY>> getChildren(KEY position);

	public abstract String getString(KEY position);
}
