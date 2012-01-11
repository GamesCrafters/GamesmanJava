package edu.berkeley.gamesman.hadoop;

import java.io.IOException;
import java.util.Collection;
import java.util.Scanner;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.MapFile;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.lib.output.MapFileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import edu.berkeley.gamesman.game.type.GameRecord;
import edu.berkeley.gamesman.hadoop.ranges.Range;
import edu.berkeley.gamesman.hadoop.ranges.RangeRecords;
import edu.berkeley.gamesman.hadoop.ranges.RangeTree;
import edu.berkeley.gamesman.hasher.genhasher.GenState;
import edu.berkeley.gamesman.propogater.common.ConfParser;
import edu.berkeley.gamesman.solve.reader.SolveReader;
import edu.berkeley.gamesman.solve.reader.SolveReaders;
import edu.berkeley.gamesman.util.Pair;

public class PlayGen {
	public static <S extends GenState> void main(String[] args)
			throws IOException, ClassNotFoundException {
		GenericOptionsParser parser = new GenericOptionsParser(args);
		Configuration conf = parser.getConfiguration();
		String[] remainArgs = parser.getRemainingArgs();
		Path p = new Path(remainArgs[0]);
		ConfParser.addParameters(conf, p, false);
		RangeTree<S> tree = (RangeTree<S>) ConfParser
				.<Range<S>, RangeRecords> newTree(conf);
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
