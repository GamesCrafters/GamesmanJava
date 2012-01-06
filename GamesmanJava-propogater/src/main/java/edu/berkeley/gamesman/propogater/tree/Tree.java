package edu.berkeley.gamesman.propogater.tree;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.MapFileOutputFormat;

import edu.berkeley.gamesman.propogater.writable.WritableSettableCombinable;
import edu.berkeley.gamesman.propogater.writable.WritableSettableComparable;
import edu.berkeley.gamesman.propogater.writable.list.WritableList;


public abstract class Tree<KEY extends WritableSettableComparable<KEY>, VALUE extends WritableSettableCombinable<VALUE>> {
	public abstract Collection<KEY> getRoots();

	public abstract void getChildren(KEY position, WritableList<KEY> toFill);

	/**
	 * Returns true if this position has children (in this case initial value
	 * only has meaning if the tree is cyclic)
	 * 
	 * @param position
	 *            The position to get the initial value for
	 * @param toFill
	 *            The value to fill in
	 * @return Whether this position has children
	 */
	public abstract boolean getInitialValue(KEY position, VALUE toFill);

	/**
	 * @param tVal
	 *            The original value
	 * @param child
	 *            The child you're coming from
	 * @param parent
	 *            The parent you're going to
	 * @param toFill
	 *            Where to store the result
	 */
	public abstract void travelUp(VALUE tVal, KEY child, KEY parent,
			VALUE toFill);

	public abstract Class<KEY> getKeyClass();

	public abstract Class<VALUE> getValClass();

	public int getDivision(KEY position) {
		return 0;
	}

	public boolean isSingleLinear() {
		return false;
	}

	public Set<Integer> getChildren(int t) {
		return Collections.emptySet();
	}

	public Class<? extends OutputFormat> getCleanupOutputFormatClass() {
		return MapFileOutputFormat.class;
	}

	public Class<? extends Reducer> getCleanupReducerClass() {
		return Reducer.class;
	}

	public boolean changed(VALUE from, VALUE to) {
		return !to.equals(from);
	}
}
