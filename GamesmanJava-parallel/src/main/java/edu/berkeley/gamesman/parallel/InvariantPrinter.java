package edu.berkeley.gamesman.parallel;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.util.GenericOptionsParser;

import edu.berkeley.gamesman.hasher.genhasher.GenHasher;
import edu.berkeley.gamesman.hasher.genhasher.GenState;
import edu.berkeley.gamesman.hasher.invhasher.InvariantHasher;
import edu.berkeley.gamesman.parallel.ranges.ChildMap;
import edu.berkeley.gamesman.parallel.ranges.MainRecords;
import edu.berkeley.gamesman.parallel.ranges.Suffix;
import edu.berkeley.gamesman.parallel.ranges.RangeTree;
import edu.berkeley.gamesman.parallel.writable.WritableTreeMap;
import edu.berkeley.gamesman.propogater.common.ConfParser;

public class InvariantPrinter {
	public static <S extends GenState, GR extends Writable> void main(
			String[] args) throws IOException {
		GenericOptionsParser parser = new GenericOptionsParser(args);
		Configuration conf = parser.getConfiguration();
		String[] remainArgs = parser.getRemainingArgs();
		Path p = new Path(remainArgs[0]);
		ConfParser.addParameters(conf, p, false);
		RangeTree<S, GR> tree = (RangeTree<S, GR>) ConfParser
				.<Suffix<S>, MainRecords<GR>, ChildMap, WritableTreeMap<GR>, WritableTreeMap<GR>, ChildMap> newTree(conf);
		GenHasher<S> hasher = tree.getHasher();
		InvariantHasher<S> h = (InvariantHasher<S>) hasher;
		h.printStates();
	}
}
