package edu.berkeley.gamesman.solve.reader;

import edu.berkeley.gamesman.parallel.game.connect4.Connect4;
import edu.berkeley.gamesman.parallel.game.reversi.Reversi;
import edu.berkeley.gamesman.parallel.game.tictactoe.TicTacToe;
import edu.berkeley.gamesman.propogater.tree.Tree;

import java.io.IOException;
import java.util.HashMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.MapFile;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.lib.output.MapFileOutputFormat;
import org.apache.hadoop.util.ReflectionUtils;

public class SolveReaders {
	private static final HashMap<String, Class<? extends SolveReader>> gameClasses = new HashMap<String, Class<? extends SolveReader>>();

	static {
		gameClasses.put("ttt", TicTacToe.class);
		gameClasses.put("reversi", Reversi.class);
		gameClasses.put("connect4", Connect4.class);
		// TODO Add more games here
	}

	public static <KEY, GR> SolveReader<KEY, GR> get(Configuration conf,
			String game) throws ClassNotFoundException {
		Class<? extends SolveReader<KEY, GR>> gameClass = (Class<? extends SolveReader<KEY, GR>>) gameClasses
				.get(game);
		if (gameClass == null)
			throw new ClassNotFoundException(game);
		SolveReader<KEY, GR> t = ReflectionUtils.newInstance(gameClass, conf);
		return t;
	}

	public static <K extends WritableComparable<K>, V extends Writable> V readPosition(
			Tree<K, V, ?, ?, ?, ?> tree, Path folder, K position,
			Partitioner<K, V> partitioner) throws IOException {
		V value = ReflectionUtils.newInstance(tree.getValClass(),
				tree.getConf());
		MapFile.Reader[] readers = MapFileOutputFormat.getReadersArray(
				new Path[] { folder }, tree.getConf());
		MapFileOutputFormat.<K, V> getEntry(readers, partitioner, position,
				value);
		for (MapFile.Reader r : readers)
			r.close();
		return value;
	}
}
