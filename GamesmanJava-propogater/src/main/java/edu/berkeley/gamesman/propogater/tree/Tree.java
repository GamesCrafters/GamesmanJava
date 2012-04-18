package edu.berkeley.gamesman.propogater.tree;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.MapFileOutputFormat;
import org.apache.hadoop.util.ReflectionUtils;

import edu.berkeley.gamesman.propogater.common.Adder;
import edu.berkeley.gamesman.propogater.common.Entry3;
import edu.berkeley.gamesman.propogater.writable.Entry;
import edu.berkeley.gamesman.propogater.writable.list.WritList;
import edu.berkeley.gamesman.propogater.writable.list.WritableList;

public abstract class Tree<K extends WritableComparable<K>, V extends Writable, PI extends Writable, UM extends Writable, CI extends Writable, DM extends Writable>
		extends Configured {
	public abstract void firstVisit(K key, V valueToFill,
			WritList<Entry<K, PI>> parents,
			Adder<Entry3<K, CI, DM>> childrenToFill);

	public abstract void combineDown(K key, V value,
			WritList<Entry<K, PI>> parents, int firstNewParent,
			WritableList<Entry<K, CI>> children);

	public abstract boolean combineUp(K key, V value,
			WritList<Entry<K, PI>> parents, WritableList<Entry<K, CI>> children);

	public abstract void receiveDown(K key, V currentValue, K parentKey,
			DM parentMessage, PI toFill);

	public abstract void receiveUp(K key, V currentValue, K childKey,
			UM childMessage, CI currentChildInfo);

	public abstract void sendUp(K key, V value, K parentKey, PI parentInfo,
			UM toFill);

	public boolean copyUM() {
		return false;
	}

	public boolean copyDM() {
		return false;
	}

	public abstract Class<K> getKeyClass();

	public abstract Class<V> getValClass();

	public abstract Class<PI> getPiClass();

	public abstract Class<CI> getCiClass();

	public abstract Class<UM> getUmClass();

	public abstract Class<DM> getDmClass();

	public abstract Collection<K> getRoots();

	/**
	 * Returns a value which is unique to the tier at which this position
	 * occurs. This allows for optimization in dealing with what should be
	 * passed to each hadoop task and what will remain unchanged
	 * 
	 * @param position
	 *            The position
	 * @return A unique integer corresponding to the tier of this position
	 */
	public int getDivision(K position) {
		return 0;
	}

	public boolean isSingleLinear() {
		return false;
	}

	public Set<Integer> getChildren(int t) {
		return Collections.emptySet();
	}

	public Class<? extends TreeNode<K, V, PI, UM, CI, DM>> getTreeNodeClass() {
		return (Class<? extends TreeNode<K, V, PI, UM, CI, DM>>) TreeNode.class
				.<TreeNode> asSubclass(TreeNode.class);
	}

	public Class<? extends Reducer> getCleanupReducerClass() {
		return Reducer.class;
	}

	public Class<? extends OutputFormat> getCleanupOutputFormatClass() {
		return MapFileOutputFormat.class;
	}

	@Override
	public final void setConf(Configuration conf) {
		if (conf == getConf())
			return;
		else if (conf == null)
			return;
		super.setConf(conf);
		configure(conf);
		if (copyUM()) {
			if (!getUmClass().equals(getCiClass()))
				throw new ClassCastException("CIClass: " + getCiClass()
						+ " and UMClass: " + getUmClass() + " are not equal");
		}
		if (copyDM()) {
			if (!getDmClass().equals(getPiClass()))
				throw new ClassCastException("PIClass: " + getPiClass()
						+ " and DMClass: " + getDmClass() + " are not equal");
		}
	}

	protected void configure(Configuration conf) {
	}

	public final void prepareRun(Configuration conf) {
		treePrepareRun(conf);
		conf.setClass("propogater.run.key.class", getKeyClass(),
				WritableComparable.class);
		conf.setClass("propogater.run.value.class", getValClass(),
				Writable.class);
		conf.setClass("propogater.run.pi.class", getPiClass(), Writable.class);
		conf.setClass("propogater.run.ci.class", getCiClass(), Writable.class);
		conf.setClass("propogater.run.um.class", getUmClass(), Writable.class);
		conf.setClass("propogater.run.dm.class", getDmClass(), Writable.class);
	}

	protected void treePrepareRun(Configuration conf) {
	}

	static <K extends WritableComparable<K>> Class<K> getRunKClass(
			Configuration conf) {
		Class<K> kClass = (Class<K>) conf.getClass("propogater.run.key.class",
				null, WritableComparable.class);
		if (kClass == null)
			throw new NullPointerException();
		return kClass;
	}

	static <V extends Writable> Class<? extends V> getRunVClass(
			Configuration conf) {
		Class<? extends V> vClass = (Class<? extends V>) conf.getClass(
				"propogater.run.value.class", null, Writable.class);
		if (vClass == null)
			throw new NullPointerException();
		return vClass;
	}

	static <PI extends Writable> Class<? extends PI> getRunPiClass(
			Configuration conf) {
		Class<? extends PI> vClass = (Class<? extends PI>) conf.getClass(
				"propogater.run.pi.class", null, Writable.class);
		if (vClass == null)
			throw new NullPointerException();
		return vClass;
	}

	static <CI extends Writable> Class<? extends CI> getRunCiClass(
			Configuration conf) {
		Class<? extends CI> vClass = (Class<? extends CI>) conf.getClass(
				"propogater.run.ci.class", null, Writable.class);
		if (vClass == null)
			throw new NullPointerException();
		return vClass;
	}

	static <PM extends Writable> Class<? extends PM> getRunUmClass(
			Configuration conf) {
		Class<? extends PM> vClass = (Class<? extends PM>) conf.getClass(
				"propogater.run.um.class", null, Writable.class);
		if (vClass == null)
			throw new NullPointerException();
		return vClass;
	}

	static <CM extends Writable> Class<? extends CM> getRunDmClass(
			Configuration conf) {
		Class<? extends CM> vClass = (Class<? extends CM>) conf.getClass(
				"propogater.run.dm.class", null, Writable.class);
		if (vClass == null)
			throw new NullPointerException();
		return vClass;
	}

	public TreeNode<K, V, PI, UM, CI, DM> newNode() {
		return ReflectionUtils.newInstance(getTreeNodeClass(), getConf());
	}

	public long getCleanupSplitSize(Configuration conf) {
		long splitSize = conf.getLong("propogater.cleanup.split.size", -1);
		if (splitSize == -1)
			splitSize = splitSize(conf);
		return splitSize;
	}

	public long getCombineSplitSize(Configuration conf, int tier) {
		long splitSize = conf.getLong("propogater.combine.split.size", -1);
		if (splitSize == -1)
			splitSize = splitSize(conf);
		return splitSize;
	}

	public long getCreateSplitSize(Configuration conf, int tier) {
		long splitSize = conf.getLong("propogater.create.split.size", -1);
		if (splitSize == -1)
			splitSize = splitSize(conf);
		return Math.max(
				splitSize / edgeMultiplier(Collections.singleton(tier)), 1);
	}

	public long getPropogateSplitSize(Configuration conf, Set<Integer> tiers) {
		long splitSize = conf.getLong("propogater.propogate.split.size", -1);
		if (splitSize == -1)
			splitSize = splitSize(conf);
		return Math.max(splitSize / edgeMultiplier(tiers), 1);
	}

	protected int edgeMultiplier(Set<Integer> tiers) {
		return 1;
	}

	protected long splitSize(Configuration conf) {
		long splitSize = conf.getLong("propogater.split.size", -1);
		if (splitSize == -1)
			throw new RuntimeException("No split size set");
		return splitSize;
	}

	public SequenceFile.CompressionType getCleanupCompressionType() {
		return SequenceFile.CompressionType.BLOCK;
	}

	public long getMapperMaxSplitSize(Configuration conf, int tier) {
		return conf.getLong("creation.mapper.split.size", -1);
	}
}
