package edu.berkeley.gamesman.game.tree;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.GenericOptionsParser;

import edu.berkeley.gamesman.game.type.GameRecord;
import edu.berkeley.gamesman.propogater.common.ConfParser;
import edu.berkeley.gamesman.propogater.solver.Solver;
import edu.berkeley.gamesman.propogater.writable.WritableSettableComparable;


public class Solve {
	public static <K extends WritableSettableComparable<K>> void main(
			String[] args) throws IOException, InterruptedException,
			ClassNotFoundException, ExecutionException {
		GenericOptionsParser parser = new GenericOptionsParser(args);
		Configuration conf = parser.getConfiguration();
		Path p = new Path(parser.getRemainingArgs()[0]);
		ConfParser.addParameters(conf, p, true);
		Solver<K, GameRecord> solve = new Solver<K, GameRecord>(conf);
		solve.run();
	}
}
