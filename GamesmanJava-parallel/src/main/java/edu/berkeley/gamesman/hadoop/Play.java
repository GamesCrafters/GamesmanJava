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


import edu.berkeley.gamesman.propogater.common.ConfParser;
import edu.berkeley.gamesman.propogater.tree.Tree;
import edu.berkeley.gamesman.propogater.writable.WritableSettableComparable;
import edu.berkeley.gamesman.solve.reader.SolveReader;
import edu.berkeley.gamesman.solve.reader.SolveReaders;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.game.type.GameRecord;
import edu.berkeley.gamesman.game.type.GameValue;

/**
 * Outcomes: edu.berkeley.gamesman.core.Value
 * <p>
 * displayBoard: edu.berkeley.gamesman.game.Game.displayState
 * <p>
 * getCurrentOutcome: {Hash position, fetch record from database, unhash it}
 * <p>
 * getNumberOfMovesForCurrentOutcome: {Look at the size of the collection
 * returned by validMoves}
 * <p>
 * getValidMoves = edu.berkeley.gamesman.game.Game.validMoves {with only one
 * argument}
 */
public final class Play {
	/**
	 * The main method for playing a game inside the console.
	 * 
	 * @param args
	 *            The path to a job file.
	 * @throws IOException
	 *             If an IO exception occurs while reading the file
	 * @throws ClassNotFoundException
	 *             If the configuration contains a nonexistent class
	 */
	public static <K extends WritableSettableComparable<K>> void main(
			String[] args) throws IOException, ClassNotFoundException {
		GenericOptionsParser parser = new GenericOptionsParser(args);
		Configuration conf = parser.getConfiguration();
		String[] remainArgs = parser.getRemainingArgs();
		Path p = new Path(remainArgs[0]);
		ConfParser.addParameters(conf, p, false);
		Tree<K, GameRecord> tree = ConfParser.<K, GameRecord> newTree(conf);
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
}
