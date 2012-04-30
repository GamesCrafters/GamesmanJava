package edu.berkeley.gamesman.propogater.tasks;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.Reducer;

import edu.berkeley.gamesman.propogater.common.ConfParser;
import edu.berkeley.gamesman.propogater.tree.Tree;
import edu.berkeley.gamesman.propogater.tree.TreeNode;

public class TreeReducer<K extends WritableComparable<K>, V extends Writable, PI extends Writable, UM extends Writable, CI extends Writable, DM extends Writable>
		extends
		Reducer<K, TreeNode<K, V, PI, UM, CI, DM>, K, TreeNode<K, V, PI, UM, CI, DM>> {
	protected Tree<K, V, PI, UM, CI, DM> tree;
	private TreeNode<K, V, PI, UM, CI, DM> value;
	private final IntWritable tempDiv = new IntWritable();
	private final HashMap<IntWritable, LongWritable> recordCount = new HashMap<IntWritable, LongWritable>();

	protected void setup(Context context) throws IOException,
			InterruptedException {
		tree = ConfParser.newTree(context.getConfiguration());
		value = tree.newNode();
	}

	@Override
	protected void reduce(K key,
			Iterable<TreeNode<K, V, PI, UM, CI, DM>> values, Context context)
			throws IOException, InterruptedException {
		value.reset();
		for (TreeNode<K, V, PI, UM, CI, DM> node : values)
			value.combineWith(node);
		int division = tree.getDivision(key);
		combine(key, value, division);
		addRecord(division, key);
		context.write(key, value);
	}

	private void addRecord(int division, K key) {
		tempDiv.set(division);
		LongWritable val = recordCount.get(tempDiv);
		if (val == null) {
			IntWritable newVal = new IntWritable(division);
			val = new LongWritable(0L);
			recordCount.put(newVal, val);
		}
		val.set(val.get() + tree.sizeof(key));
	}

	protected void combine(K key, TreeNode<K, V, PI, UM, CI, DM> value,
			int division) {
	}

	@Override
	protected void cleanup(Context context) {
		for (Map.Entry<IntWritable, LongWritable> entry : recordCount
				.entrySet()) {
			Counter counter = context.getCounter("num_records",
					"t" + entry.getKey());
			counter.increment(entry.getValue().get());
		}
	}
}
