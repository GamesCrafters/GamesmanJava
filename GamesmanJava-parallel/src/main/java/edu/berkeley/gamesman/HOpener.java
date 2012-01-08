package edu.berkeley.gamesman;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.thrift.TException;

import edu.berkeley.gamesman.propogater.common.ConfParser;
import edu.berkeley.gamesman.propogater.writable.WritableSettableComparable;
import edu.berkeley.gamesman.solve.reader.SolveReader;
import edu.berkeley.gamesman.solve.reader.SolveReaders;
import edu.berkeley.gamesman.thrift.GamestateResponse;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.game.type.GameRecord;

public class HOpener implements Opener {
	private class HFetcher<KEY extends WritableSettableComparable<KEY>>
			implements RecordFetcher {
		private final Configuration conf;
		private final Path folderPath;
		private final SolveReader<KEY> reader;
		private final boolean solved;
		private final Partitioner<KEY, GameRecord> partitioner;

		public HFetcher(Configuration hConf, String game, String filename)
				throws ClassNotFoundException, IOException {
			conf = hConf;
			String folderName = hConf.get("solve.folder");
			if (folderName == null) {
				folderPath = new Path(solveDirectory, filename + "_folder");
			} else
				folderPath = new Path(folderName);
			solved = folderPath.getFileSystem(hConf).exists(folderPath);
			reader = SolveReaders.<KEY> get(hConf, game);
			if (solved)
				partitioner = ConfParser
						.<KEY, GameRecord> getPartitionerInstance(conf);
			else
				partitioner = null;
		}

		@Override
		public List<GamestateResponse> getNextMoveValues(String board)
				throws TException {
			KEY position = reader.getPosition(board);
			Collection<Pair<String, KEY>> children = reader
					.getChildren(position);
			ArrayList<GamestateResponse> records = new ArrayList<GamestateResponse>(
					children.size());
			for (Pair<String, KEY> child : children) {
				GamestateResponse response = getMoveValue(child.cdr, true);
				response.setMove(child.car);
				records.add(response);
			}
			return records;
		}

		@Override
		public GamestateResponse getMoveValue(String board) throws TException {
			KEY position = reader.getPosition(board);
			return getMoveValue(position, false);
		}

		private GamestateResponse getMoveValue(KEY position,
				boolean previousPosition) {
			GamestateResponse response = new GamestateResponse();
			response.setBoard(reader.getString(position));
			if (solved) {
				GameRecord rec;
				try {
					rec = SolveReaders.<KEY> readPosition(conf, folderPath,
							position, partitioner);
					if (previousPosition)
						rec.previousPosition();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				response.setValue(rec.getValue().name().toLowerCase());
				response.setRemoteness(rec.getRemoteness());
			}
			return response;
		}

	}

	private final Configuration conf;
	private final Path solveDirectory;
	private final FileSystem fs;

	public HOpener(Configuration conf, Path solveDirectory) throws IOException {
		this.conf = conf;
		this.solveDirectory = solveDirectory;
		fs = solveDirectory.getFileSystem(conf);
	}

	@Override
	public RecordFetcher addDatabase(Map<String, String> params, String game,
			String filename) {
		Properties props = new Properties();
		Path solvePath = new Path(solveDirectory, filename + ".job");
		try {
			InputStream is;
			if (fs.exists(solvePath)) {
				is = fs.open(solvePath);
			} else {
				is = fs.open(new Path(solveDirectory, game + ".job"));
			}
			props.load(is);
			is.close();
			Configuration hConf = new Configuration(conf);
			for (Map.Entry<Object, Object> entry : props.entrySet()) {
				hConf.set(entry.getKey().toString(), entry.getValue()
						.toString());
			}
			return new HFetcher(hConf, game, filename);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
}
