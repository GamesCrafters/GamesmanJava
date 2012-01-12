package edu.berkeley.gamesman.propogater.tree;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.MapFileOutputFormat;
import org.apache.hadoop.util.ReflectionUtils;

import edu.berkeley.gamesman.propogater.writable.WritableSettable;
import edu.berkeley.gamesman.propogater.writable.WritableSettableComparable;
import edu.berkeley.gamesman.propogater.writable.list.WritableArray;
import edu.berkeley.gamesman.propogater.writable.list.WritableList;

public abstract class Tree<KEY extends WritableSettableComparable<KEY>, VALUE extends WritableSettable<VALUE>>
		implements Configurable {
	private Configuration conf;

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
	public abstract void travelUp(VALUE tVal, int childNum, KEY child,
			KEY parent, VALUE toFill);

	public abstract Class<? extends KEY> getKeyClass();

	public abstract Class<? extends VALUE> getValClass();

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

	public abstract boolean combine(KEY pos, WritableArray<VALUE> children, VALUE toFill);

	@Override
	public final Configuration getConf() {
		return conf;
	}

	@Override
	public final void setConf(Configuration conf) {
		this.conf = conf;
		configure(conf);
	}

	protected void configure(Configuration conf) {
	}

	public final void prepareRun(Configuration conf) {
		conf.setClass("propogater.run.key.class", getKeyClass(),
				WritableSettableComparable.class);
		conf.setClass("propogater.run.value.class", getValClass(),
				WritableSettable.class);
	}

	public static <KEY extends WritableSettableComparable<KEY>> Class<KEY> getRunKeyClass(
			Configuration conf) {
		Class<KEY> kClass = (Class<KEY>) conf.getClass(
				"propogater.run.key.class", null,
				WritableSettableComparable.class);
		if (kClass == null)
			throw new NullPointerException();
		return kClass;
	}

	public static <VALUE extends WritableSettable<VALUE>> Class<VALUE> getRunValueClass(
			Configuration conf) {
		Class<VALUE> vClass = (Class<VALUE>) conf.getClass(
				"propogater.run.value.class", null, WritableSettable.class);
		if (vClass == null)
			throw new NullPointerException();
		return vClass;
	}

	public VALUE newValue() {
		return ReflectionUtils.newInstance(getValClass(), conf);
	}
}
