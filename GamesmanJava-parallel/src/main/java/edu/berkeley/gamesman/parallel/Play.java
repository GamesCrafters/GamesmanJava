package edu.berkeley.gamesman.parallel;

import java.io.IOException;
import java.util.Collection;
import java.util.Scanner;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.lib.output.MapFileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import edu.berkeley.gamesman.parallel.ranges.MainRecords;
import edu.berkeley.gamesman.parallel.ranges.Suffix;
import edu.berkeley.gamesman.parallel.ranges.RangeTree;
import edu.berkeley.gamesman.propogater.common.ConfParser;
import edu.berkeley.gamesman.propogater.tree.Tree;
import edu.berkeley.gamesman.propogater.writable.FixedLengthWritable;
import edu.berkeley.gamesman.solve.reader.SolveReader;
import edu.berkeley.gamesman.solve.reader.SolveReaders;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.game.tree.GameTree;
import edu.berkeley.gamesman.game.type.GameRecord;
import edu.berkeley.gamesman.game.type.GameValue;
import edu.berkeley.gamesman.hasher.genhasher.GenState;

public final class Play {
	public static <K extends WritableComparable<K>, GR extends FixedLengthWritable> void main(
			String[] args) throws IOException, ClassNotFoundException {
		GenericOptionsParser parser = new GenericOptionsParser(args);
		Configuration conf = parser.getConfiguration();
		String[] remainArgs = parser.getRemainingArgs();
		Path p = new Path(remainArgs[0]);
		ConfParser.addParameters(conf, p, false);
		Tree<K, ?, ?, ?, ?, ?> tree = ConfParser
				.<K, Writable, Writable, Writable, Writable, Writable> newTree(conf);
		tree.prepareRun(conf);
		if (tree instanceof GameTree) {
			Play.<K> subMain(conf, (GameTree) tree);
		} else if (tree instanceof RangeTree) {
			Play.<GenState, GR> rangeSubMain(conf, (RangeTree) tree);
		}
	}

	private static <K extends WritableComparable<K>> void subMain(
			Configuration conf, GameTree<K> tree) throws IOException,
			ClassNotFoundException {
		K position = tree.getRoots().iterator().next();
		Path[] outPath = new Path[1];
		outPath[0] = ConfParser.getOutputPath(conf);
		FileSystem fs = outPath[0].getFileSystem(conf);
		Path[] readers = MapFileOutputFormat.getFileNames(fs, outPath, conf);
		Partitioner<K, GameRecord> partitioner = ConfParser
				.<K, GameRecord> getPartitionerInstance(conf);
		String gameName = GamesmanParser.getGameName(conf);
		SolveReader<K, GameRecord> gameReader = SolveReaders
				.<K, GameRecord> get(conf, gameName);

		Scanner scan = new Scanner(System.in);
		GameRecord storeRecord = new GameRecord();
		GameValue primVal = tree.getPrimitiveValue(position);
		while (primVal == null) {
			System.out.println(position.toString());
			MapFileOutputFormat.getEntry(fs, conf, readers, partitioner,
					position, storeRecord);
			System.out.println(storeRecord);
			Collection<Pair<String, K>> moves = gameReader
					.getChildren(position);
			StringBuilder availableMoves = new StringBuilder(
					"Available Moves: ");
			for (Pair<String, K> move : moves) {
				availableMoves.append(move.car);
				availableMoves.append(", ");
			}
			System.out.println(availableMoves);
			if (scan.hasNext()) {
				String moveString = scan.nextLine();
				for (Pair<String, K> move : moves) {
					if (move.car.toUpperCase().equals(moveString.toUpperCase())) {
						position = move.cdr;
						break;
					}
				}
			} else {
				break;
			}
			primVal = tree.getPrimitiveValue(position);
		}
		if (primVal != null) {
			System.out.println(position.toString());
			System.out.println("Game over");
		}
	}

	private static <S extends GenState, GR extends FixedLengthWritable> void rangeSubMain(
			Configuration conf, RangeTree<S, GR> tree) throws IOException,
			ClassNotFoundException {
		Suffix<S> posRange = tree.getRoots(true).iterator().next();
		Path[] outPath = new Path[1];
		outPath[0] = ConfParser.getOutputPath(conf);
		FileSystem fs = outPath[0].getFileSystem(conf);
		Path[] readers = MapFileOutputFormat.getFileNames(fs, outPath, conf);
		Partitioner<Suffix<S>, MainRecords<GR>> partitioner = ConfParser
				.<Suffix<S>, MainRecords<GR>> getPartitionerInstance(conf);
		String gameName = GamesmanParser.getGameName(conf);
		SolveReader<S, GR> gameReader = SolveReaders
				.<S, GR> get(conf, gameName);

		Scanner scan = new Scanner(System.in);
		MainRecords<GR> recs = new MainRecords<GR>(conf);
		S position = tree.getStartingPositions().iterator().next();
		boolean gameFinished = true;
		while (tree.getValue(position) == null) {
			System.out.println(position.toString());
			MapFileOutputFormat.getEntry(fs, conf, readers, partitioner,
					posRange, recs);
			GR unparsedRecord = tree.getRecord(posRange, position, recs);
			GameRecord record = gameReader.getRecord(position, unparsedRecord);
			System.out.println(record);

			Collection<Pair<String, S>> moves = gameReader
					.getChildren(position);
			StringBuilder availableMoves = new StringBuilder(
					"Available Moves: ");
			for (Pair<String, S> move : moves) {
				availableMoves.append(move.car);
				availableMoves.append(", ");
			}
			System.out.println(availableMoves);
			if (scan.hasNext()) {
				String moveString = scan.nextLine();
				for (Pair<String, S> move : moves) {
					if (move.car.toUpperCase().equals(moveString.toUpperCase())) {
						position = move.cdr;
						break;
					}
				}
			} else {
				gameFinished = false;
				break;
			}
			posRange = tree.makeContainingRange(position, true);
		}
		if (gameFinished) {
			System.out.println(position.toString());
			System.out.println("Game over");
		}
	}
}
