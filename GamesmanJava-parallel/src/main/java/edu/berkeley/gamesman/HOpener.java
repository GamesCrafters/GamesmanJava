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
import org.apache.thrift.TException;

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

		public HFetcher(Configuration hConf, String game, String filename)
				throws ClassNotFoundException {
			conf = hConf;
			String folderName = hConf.get("solve.folder");
			if (folderName == null) {
				folderPath = new Path(solveDirectory, filename + "_folder");
			} else
				folderPath = new Path(folderName);
			reader = SolveReaders.<KEY> get(hConf, game);
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
				GamestateResponse response = new GamestateResponse();
				response.setMove(child.car);
				response.setBoard(reader.getString(child.cdr));
				GameRecord rec;
				try {
					rec = SolveReaders.<KEY> readPosition(conf, folderPath,
							child.cdr);
					rec.previousPosition();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				response.setValue(rec.getValue().name().toLowerCase());
				response.setRemoteness(rec.getRemoteness());
				records.add(response);
			}
			return records;
		}

		@Override
		public GamestateResponse getMoveValue(String board) throws TException {
			KEY position = reader.getPosition(board);
			GamestateResponse response = new GamestateResponse();
			response.setBoard(reader.getString(position));
			GameRecord rec;
			try {
				rec = SolveReaders.<KEY> readPosition(conf, folderPath,
						position);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			response.setValue(rec.getValue().name().toLowerCase());
			response.setRemoteness(rec.getRemoteness());
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
		InputStream is;
		try {
			is = fs.open(solvePath);
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
