package edu.berkeley.gamesman.parallel.compare;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapred.SequenceFileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.ReflectionUtils;

import edu.berkeley.gamesman.propogater.common.ConfParser;
import edu.berkeley.gamesman.propogater.tree.Tree;
import edu.berkeley.gamesman.propogater.tree.TreeNode;

public class ExamineFolder {
	public static <K extends WritableComparable<K>> void main(String[] args)
			throws IOException, InterruptedException {
		GenericOptionsParser parser = new GenericOptionsParser(args);
		Configuration conf = parser.getConfiguration();
		String[] remainArgs = parser.getRemainingArgs();
		Path p = new Path(remainArgs[0]);
		ConfParser.addParameters(conf, p, true);
		Path folderPath = new Path(remainArgs[1]);
		SequenceFile.Reader[] readers = SequenceFileOutputFormat.getReaders(
				conf, folderPath);
		Tree<K, ?, ?, ?, ?, ?> tree = ConfParser
				.<K, Writable, Writable, Writable, Writable, Writable> newTree(conf);
		tree.prepareRun(conf);

		TreeNode<K, ?, ?, ?, ?, ?> node = tree.newNode();
		K key = ReflectionUtils.<K> newInstance(tree.getKeyClass(), conf);
		for (SequenceFile.Reader reader : readers) {
			while (reader.next(key, node)) {
				System.out.println(key);
				System.out.println(node.hasValue());
				Thread.sleep(1000);
			}
		}
	}
}
