package edu.berkeley.gamesman.solve.reader;

import edu.berkeley.gamesman.parallel.game.connect4.Connect4;
import edu.berkeley.gamesman.parallel.game.ninemensmorris.NineMensMorris;
import edu.berkeley.gamesman.parallel.game.reversi.Reversi;
import edu.berkeley.gamesman.parallel.game.tictactoe.TicTacToe;
import edu.berkeley.gamesman.parallel.game.tootandotto.TootAndOtto;
import edu.berkeley.gamesman.propogater.tree.Tree;

import java.io.IOException;
import java.util.HashMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
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
		gameClasses.put("tootnotto", TootAndOtto.class);
		gameClasses.put("ninemensmorris", NineMensMorris.class);
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
		Configuration conf = tree.getConf();
		FileSystem fs = folder.getFileSystem(conf);
		V value = ReflectionUtils.newInstance(tree.getValClass(), conf);
		Path[] readerPaths = MapFileOutputFormat.getFileNames(fs,
				new Path[] { folder }, conf);
		MapFileOutputFormat.<K, V> getEntry(fs, conf, readerPaths, partitioner,
				position, value);
		return value;
	}
}
