package edu.berkeley.gamesman.parallel;

import java.io.IOException;
import java.util.Collection;
import java.util.Scanner;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.MapFile;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.lib.output.MapFileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import edu.berkeley.gamesman.parallel.ranges.Range;
import edu.berkeley.gamesman.parallel.ranges.RangeRecords;
import edu.berkeley.gamesman.parallel.ranges.RangeTree;
import edu.berkeley.gamesman.propogater.common.ConfParser;
import edu.berkeley.gamesman.propogater.tree.Tree;
import edu.berkeley.gamesman.propogater.writable.WritableSettable;
import edu.berkeley.gamesman.propogater.writable.WritableSettableComparable;
import edu.berkeley.gamesman.solve.reader.SolveReader;
import edu.berkeley.gamesman.solve.reader.SolveReaders;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.game.tree.GameTree;
import edu.berkeley.gamesman.game.type.GameRecord;
import edu.berkeley.gamesman.game.type.GameValue;
import edu.berkeley.gamesman.hasher.genhasher.GenState;

public final class Play {
	public static <K extends WritableSettableComparable<K>> void main(
			String[] args) throws IOException, ClassNotFoundException {
		GenericOptionsParser parser = new GenericOptionsParser(args);
		Configuration conf = parser.getConfiguration();
		String[] remainArgs = parser.getRemainingArgs();
		Path p = new Path(remainArgs[0]);
		ConfParser.addParameters(conf, p, false);
		Tree<?, ?> tree = ConfParser
				.<WritableSettableComparable, WritableSettable> newTree(conf);
		if (tree instanceof GameTree) {
			Play.<K> subMain(conf, (GameTree<K>) tree);
		} else if (tree instanceof RangeTree) {
			Play.<GenState> rangeSubMain(conf, (RangeTree<GenState>) tree);
		}
	}

	private static <K extends WritableSettableComparable<K>> void subMain(
			Configuration conf, GameTree<K> tree) throws IOException,
			ClassNotFoundException {
		K position = tree.getRoots().iterator().next();
		Path[] outPath = new Path[1];
		outPath[0] = ConfParser.getOutputPath(conf);
		MapFile.Reader[] readers = MapFileOutputFormat.getReadersArray(outPath,
				conf);
		Partitioner<K, GameRecord> partitioner = ConfParser
				.<K, GameRecord> getPartitionerInstance(conf);
		String gameName = GamesmanParser.getGameName(conf);
		SolveReader<K> gameReader = SolveReaders.get(conf, gameName);

		Scanner scan = new Scanner(System.in);
		GameRecord storeRecord = new GameRecord();
		tree.getInitialValue(position, storeRecord);
		while (storeRecord.getValue() == GameValue.DRAW) {
			System.out.println(position.toString());
			MapFileOutputFormat.getEntry(readers, partitioner, position,
					storeRecord);
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
			tree.getInitialValue(position, storeRecord);
		}
		tree.getInitialValue(position, storeRecord);
		if (storeRecord.getValue() != GameValue.DRAW) {
			System.out.println(position.toString());
			System.out.println("Game over");
		}
	}

	private static <S extends GenState> void rangeSubMain(Configuration conf,
			RangeTree<S> tree) throws IOException, ClassNotFoundException {
		Range<S> posRange = tree.getRoots().iterator().next();
		Path[] outPath = new Path[1];
		outPath[0] = ConfParser.getOutputPath(conf);
		MapFile.Reader[] readers = MapFileOutputFormat.getReadersArray(outPath,
				conf);
		Partitioner<Range<S>, RangeRecords> partitioner = ConfParser
				.<Range<S>, RangeRecords> getPartitionerInstance(conf);
		String gameName = GamesmanParser.getGameName(conf);
		SolveReader<S> gameReader = SolveReaders.<S> get(conf, gameName);

		Scanner scan = new Scanner(System.in);
		RangeRecords recs = new RangeRecords();
		tree.getInitialValue(posRange, recs);

		S position = tree.getStartingPositions().iterator().next();
		boolean gameFinished = true;
		while (tree.getValue(position) == null) {
			System.out.println(position.toString());
			MapFileOutputFormat.getEntry(readers, partitioner, posRange, recs);
			GameRecord record = tree.getRecord(posRange, position, recs);
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
			posRange = tree.makeContainingRange(position);
		}
		if (gameFinished) {
			System.out.println(position.toString());
			System.out.println("Game over");
		}
	}
}
