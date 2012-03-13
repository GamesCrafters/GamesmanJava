package edu.berkeley.gamesman.propogater.tree;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;

import edu.berkeley.gamesman.propogater.factory.Factory;
import edu.berkeley.gamesman.propogater.factory.FactoryUtil;
import edu.berkeley.gamesman.propogater.writable.BitSetWritable;
import edu.berkeley.gamesman.propogater.writable.Entry;
import edu.berkeley.gamesman.propogater.writable.IntEntry;
import edu.berkeley.gamesman.propogater.writable.ValueWrapper;
import edu.berkeley.gamesman.propogater.writable.list.WritableList;

public class TreeNode<K extends WritableComparable<K>, V extends Writable, PI extends Writable, UM extends Writable, CI extends Writable, DM extends Writable>
		extends Configured implements Writable {
	private ValueWrapper<V> value;
	private int lastParentsLength = 0;
	private WritableList<IntEntry<Entry<K, PI>>> parents;
	private BitSetWritable cleanSet = new BitSetWritable();
	private WritableList<Entry<K, CI>> children;

	private WritableList<IntEntry<UM>> uMess;

	private WritableList<IntEntry<Entry<K, DM>>> dMess;

	public TreeNode() {
	}

	public TreeNode(Configuration conf) {
		super(conf);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		checkConf();
		value.readFields(in);
		if (value.hasValue()) {
			lastParentsLength = in.readInt();
			cleanSet.readFields(in);
			children.readFields(in);
		}
		parents.readFields(in);
		uMess.readFields(in);
		dMess.readFields(in);
	}

	@Override
	public void write(DataOutput out) throws IOException {
		checkConf();
		value.write(out);
		if (value.hasValue()) {
			out.writeInt(lastParentsLength);
			cleanSet.write(out);
			children.write(out);
		}
		parents.write(out);
		uMess.write(out);
		dMess.write(out);
	}

	@Override
	public final void setConf(final Configuration conf) {
		super.setConf(conf);
		if (conf != null) {
			treeNodeConfigure(conf);
			final Factory<K> kFactory = makeKFactory(conf);
			Factory<V> vFactory = makeVFactory(conf);
			final Factory<PI> piFactory = makePIFactory(conf);
			final Factory<CI> ciFactory = makeCIFactory(conf);
			value = new ValueWrapper<V>(vFactory);
			parents = new WritableList<IntEntry<Entry<K, PI>>>(
					new Factory<IntEntry<Entry<K, PI>>>() {

						@Override
						public IntEntry<Entry<K, PI>> create() {
							return new IntEntry<Entry<K, PI>>(new Entry<K, PI>(
									kFactory, piFactory));
						}
					});
			children = new WritableList<Entry<K, CI>>(
					new Factory<Entry<K, CI>>() {
						@Override
						public Entry<K, CI> create() {
							return new Entry<K, CI>(kFactory, ciFactory);
						}
					});
			final Factory<UM> umFactory = makeUMFactory(conf);
			final Factory<DM> dmFactory = makeDMFactory(conf);
			uMess = new WritableList<IntEntry<UM>>(new Factory<IntEntry<UM>>() {
				@Override
				public IntEntry<UM> create() {
					return new IntEntry<UM>(umFactory);
				}
			});
			dMess = new WritableList<IntEntry<Entry<K, DM>>>(
					new Factory<IntEntry<Entry<K, DM>>>() {
						@Override
						public IntEntry<Entry<K, DM>> create() {
							return new IntEntry<Entry<K, DM>>(new Entry<K, DM>(
									kFactory, dmFactory));
						}
					});
		}
	}

	protected void treeNodeConfigure(Configuration conf) {
	}

	protected Factory<UM> makeUMFactory(Configuration conf) {
		return FactoryUtil
				.<UM> makeFactory(Tree.<UM> getRunUmClass(conf), conf);
	}

	public Factory<DM> makeDMFactory(Configuration conf) {
		return FactoryUtil
				.<DM> makeFactory(Tree.<DM> getRunDmClass(conf), conf);
	}

	protected Factory<PI> makePIFactory(Configuration conf) {
		return FactoryUtil
				.<PI> makeFactory(Tree.<PI> getRunPiClass(conf), conf);
	}

	protected Factory<CI> makeCIFactory(Configuration conf) {
		return FactoryUtil
				.<CI> makeFactory(Tree.<CI> getRunCiClass(conf), conf);
	}

	protected Factory<V> makeVFactory(Configuration conf) {
		return FactoryUtil.<V> makeFactory(Tree.<V> getRunVClass(conf), conf);
	}

	protected Factory<K> makeKFactory(Configuration conf) {
		return FactoryUtil.<K> makeFactory(Tree.<K> getRunKClass(conf), conf);
	}

	public boolean hasValue() {
		checkConf();
		return value.hasValue();
	}

	private final NodeAdder<K, CI, DM> fvAdder = new NodeAdder<K, CI, DM>();
	private final ParList<K, PI> parList = new ParList<K, PI>();

	public void firstVisit(Tree<K, V, PI, UM, CI, DM> tree, K key,
			WritableList<DM> childMessagesToFill) {
		checkConf();
		children.clear();
		cleanSet.clear();
		lastParentsLength = 0;
		fvAdder.setList(children, childMessagesToFill);
		parList.setList(parents);
		tree.firstVisit(key, value.setHasAndGet(), parList, fvAdder);
		lastParentsLength = parents.length();
		if (children.length() != childMessagesToFill.length()) {
			throw new RuntimeException(
					"Must have exactly one message per child");
		}
	}

	public void combineDown(Tree<K, V, PI, UM, CI, DM> tree, K key) {
		checkConf();
		checkVisited();
		parList.setList(parents);
		tree.combineDown(key, value.get(), parList, lastParentsLength, children);
		lastParentsLength = parents.length();
		cleanSet.clear();
	}

	public boolean combineUp(Tree<K, V, PI, UM, CI, DM> tree, K key) {
		checkConf();
		parList.setList(parents);
		return tree.combineUp(key, value.get(), parList, children);
	}

	private void checkConf() {
		if (getConf() == null)
			throw new RuntimeException("setConf() never called");
	}

	public V getValue() {
		checkConf();
		return value.get();
	}

	public WritableList<IntEntry<Entry<K, PI>>> getParentList() {
		checkConf();
		return parents;
	}

	public WritableList<Entry<K, CI>> getChildren() {
		checkConf();
		checkVisited();
		return children;
	}

	private void checkVisited() {
		if (!value.hasValue())
			throw new RuntimeException("firstVisit should be called first");
	}

	public WritableList<IntEntry<Entry<K, DM>>> getDownList() {
		checkConf();
		return dMess;
	}

	public BitSetWritable getCleanSet() {
		checkConf();
		checkVisited();
		return cleanSet;
	}

	public WritableList<IntEntry<UM>> getUpList() {
		checkConf();
		return uMess;
	}

	public void clearMixes() {
		checkConf();
		value.clear();
		parents.clear();
		uMess.clear();
		dMess.clear();
	}

	public void combineWith(TreeNode<K, V, PI, UM, CI, DM> other) {
		checkConf();
		if (other.value.hasValue()) {
			if (value.hasValue())
				throw new Error("Two nodes claim to be correct");
			stealValue(other);
			lastParentsLength = other.lastParentsLength;
			stealCleanSet(other);
			stealChildren(other);
		}
		addAllParents(other);
		addAllUMess(other);
		addAllDMess(other);
	}

	private void addAllDMess(TreeNode<K, V, PI, UM, CI, DM> other) {
		dMess.steal(other.dMess);
	}

	private void addAllUMess(TreeNode<K, V, PI, UM, CI, DM> other) {
		uMess.steal(other.uMess);
	}

	private void addAllParents(TreeNode<K, V, PI, UM, CI, DM> other) {
		parents.steal(other.parents);
	}

	private void stealChildren(TreeNode<K, V, PI, UM, CI, DM> other) {
		WritableList<Entry<K, CI>> temp = children;
		children = other.children;
		other.children = temp;
	}

	private void stealCleanSet(TreeNode<K, V, PI, UM, CI, DM> other) {
		BitSetWritable temp = cleanSet;
		cleanSet = other.cleanSet;
		other.cleanSet = temp;
	}

	private void stealValue(TreeNode<K, V, PI, UM, CI, DM> other) {
		ValueWrapper<V> temp = value;
		value = other.value;
		other.value = temp;
	}
}
