package edu.berkeley.gamesman.solve.reader;

import edu.berkeley.gamesman.hadoop.game.connect4.Connect4;
import edu.berkeley.gamesman.hadoop.game.reversi.Reversi;
import edu.berkeley.gamesman.hadoop.game.tictactoe.TicTacToe;
import edu.berkeley.gamesman.propogater.tree.Tree;
import edu.berkeley.gamesman.propogater.writable.WritableSettable;
import edu.berkeley.gamesman.propogater.writable.WritableSettableComparable;

import java.io.IOException;
import java.util.HashMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.MapFile;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.lib.output.MapFileOutputFormat;
import org.apache.hadoop.util.ReflectionUtils;

public class SolveReaders {
	private static final HashMap<String, Class<? extends SolveReader>> gameClasses = new HashMap<String, Class<? extends SolveReader>>();

	static {
		gameClasses.put("ttt", TicTacToe.class);
		gameClasses.put("reversi", Reversi.class);
		gameClasses.put("Connect4", Connect4.class);
		// TODO Add more games here
	}

	public static <KEY> SolveReader<KEY> get(Configuration conf, String game)
			throws ClassNotFoundException {
		Class<? extends SolveReader<KEY>> gameClass = (Class<? extends SolveReader<KEY>>) gameClasses
				.get(game);
		if (gameClass == null)
			throw new ClassNotFoundException(game);
		SolveReader<KEY> t = ReflectionUtils.newInstance(gameClass, conf);
		return t;
	}

	public static <KEY extends WritableSettableComparable<KEY>, VALUE extends WritableSettable<VALUE>> VALUE readPosition(
			Tree<KEY, VALUE> tree, Path folder, KEY position,
			Partitioner<KEY, VALUE> partitioner) throws IOException {
		VALUE value = tree.newValue();
		MapFile.Reader[] readers = MapFileOutputFormat.getReadersArray(
				new Path[] { folder }, tree.getConf());
		MapFileOutputFormat.<KEY, VALUE> getEntry(readers, partitioner,
				position, value);
		return value;
	}
}
