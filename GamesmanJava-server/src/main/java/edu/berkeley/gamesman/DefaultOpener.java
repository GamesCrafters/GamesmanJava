package edu.berkeley.gamesman;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.thrift.TException;

import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.thrift.GamestateResponse;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;
import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.core.Value;

public class DefaultOpener implements Opener {

	private ExecutorService tp = Executors.newCachedThreadPool();

	private class DefaultFetcher implements RecordFetcher {
		private final Configuration config;
		private final Database db;

		public DefaultFetcher(Configuration conf, Database db) {
			this.config = conf;
			this.db = db;
		}

		@Override
		public List<GamestateResponse> getNextMoveValues(String board)
				throws TException {
			return this.<State> innerGetNextMoveValues(board);
		}

		private <T extends State<T>> List<GamestateResponse> innerGetNextMoveValues(
				String board) throws TException {
			if (config == null) {
				throw new TException("This game does not exist.");
			}
			// Database db = config.getDatabase();
			Game<T> game = config.getCheckedGame();

			T state = game.synchronizedStringToState(board);

			if (!game.synchronizedStateToString(state).equals(board))
				throw new Error("Board does not match: "
						+ game.synchronizedStateToString(state) + "; " + board);

			// Access to this list must be synchronized!
			final List<GamestateResponse> responseArray = Collections
					.synchronizedList(new ArrayList<GamestateResponse>());

			Value pv = game.synchronizedStrictPrimitiveValue(state);
			if (game.getPlayerCount() <= 1 || pv == Value.UNDECIDED) {
				Collection<Pair<String, T>> states = game
						.synchronizedValidMoves(state);
				Iterator<Pair<String, T>> iter = states.iterator();
				Future<?>[] recordThreads = new Future<?>[states.size()];
				for (int i = 0; i < recordThreads.length; i++) {
					final Pair<String, T> next = iter.next();
					recordThreads[i] = tp.submit(new FieldFiller<T>(config, db,
							next.car, next.cdr, responseArray));
				}

				// Wait for the worker threads to complete.
				for (Future<?> t : recordThreads) {
					while (!t.isDone()) {
						try {
							t.get();
						} catch (InterruptedException e) {
							e.printStackTrace();
						} catch (ExecutionException e) {
							throw new RuntimeException(e);
						}
					}
				}
			}
			return responseArray;
		}

		@Override
		public GamestateResponse getMoveValue(String board) throws TException {
			return this.<State> innerGetMoveValue(board);
		}

		private <T extends State<T>> GamestateResponse innerGetMoveValue(
				String board) throws TException {
			if (config == null) {
				System.out.println("This game does not exist.");
				throw new TException("This game does not exist.");
			}

			// Database db = config.getDatabase();
			Game<T> game = config.getCheckedGame();

			T state = game.synchronizedStringToState(board);

			if (!game.synchronizedStateToString(state).equals(board))
				throw new Error("Board does not match: "
						+ game.synchronizedStateToString(state) + "; " + board);

			return fillResponseFields(config, db, state, false);
		}

	}

	private final Properties serverConf;

	public DefaultOpener(Properties conf) {
		serverConf = conf;
	}

	public synchronized RecordFetcher addDatabase(Map<String, String> params,
			String game, String filename) {
		String dbPath = serverConf.getProperty("json.databasedirectory", "");
		if (dbPath != null && dbPath.length() > 0) {
			if (dbPath.charAt(dbPath.length() - 1) != '/') {
				dbPath += '/';
			}
			filename = dbPath + filename + ".db";
		} else {
			dbPath = null;
			filename = null;
		}
		try {
			File f = new File(filename);
			if (filename != null && f.exists()) {
				System.out.println("Loading solved database " + filename);
				Database db = Database.openDatabase(filename);
				return new DefaultFetcher(db.conf, db);
			} else {
				assert Util.debug(DebugFacility.JSON, "Database at " + filename
						+ " does not exist! Using unsolved.");
			}
		} catch (Error fe) {
			fe.printStackTrace();
		} catch (ClassNotFoundException cnfe) {
			cnfe.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String unsolvedJob = serverConf.getProperty("json.unsolved." + game,
				null);
		if (unsolvedJob != null) {
			try {
				Properties props = Configuration.readProperties(unsolvedJob);
				String[] allowedFields = serverConf.getProperty(
						"json.fields." + game, "").split(",");
				for (String key : allowedFields) {
					key = key.trim();
					if (key.length() == 0) {
						continue;
					}
					String val = params.get(key);
					if (val == null) {
						val = "";
					}
					props.setProperty("gamesman.game." + key, val);
				}
				return new DefaultFetcher(new Configuration(props), null);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				return null;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return null;
			}
		} else {
			new Error("Failed to find an appropriate job file for " + game)
					.printStackTrace();
			return null;
		}
	}

	private class FieldFiller<T extends State<T>> implements Runnable {
		private final Configuration config;
		private final Database db;
		private final String move;
		private final T state;
		private final List<GamestateResponse> responseArray;

		public FieldFiller(Configuration conf, Database db, String move,
				T state, List<GamestateResponse> responseArray) {
			config = conf;
			this.db = db;
			this.move = move;
			this.state = state;
			this.responseArray = responseArray;
		}

		@Override
		public void run() {
			GamestateResponse entry = new GamestateResponse();
			entry = fillResponseFields(config, db, state, true);
			entry.setMove(move);
			responseArray.add(entry);
		}
	};

	private <T extends State<T>> GamestateResponse fillResponseFields(
			Configuration conf, Database db, T state, boolean isChildState) {
		GamestateResponse request = new GamestateResponse();

		Game<T> g = conf.getCheckedGame();
		if (db != null) {
			Record rec = g.newRecord();
			DatabaseHandle handle = db.getHandle(true);
			try {
				g.synchronizedLongToRecord(
						state,
						db.readRecord(handle, g.synchronizedStateToHash(state)),
						rec);
			} catch (IOException e) {
				throw new Error(e);
			}
			if (conf.hasValue) {
				Value pv = rec.value;
				if (g.getPlayerCount() > 1 && isChildState)
					pv = pv.opposite();
				request.setValue(pv.name().toLowerCase());
			}
			if (conf.hasRemoteness) {
				request.setRemoteness(rec.remoteness);
			}
			if (conf.hasScore) {
				request.setScore(rec.score);
			}
			assert Util.debug(DebugFacility.JSON, "    Response: value="
					+ request.getValue() + "; remote=" + rec.remoteness
					+ "; score=" + rec.score);
		} else {
			Value pv = g.synchronizedStrictPrimitiveValue(state);
			if (pv != Value.UNDECIDED) {
				if (g.getPlayerCount() > 1 && isChildState) {
					if (pv == Value.WIN)
						pv = Value.LOSE;
					else if (pv == Value.LOSE)
						pv = Value.WIN;
				}
				request.setValue(pv.name().toLowerCase());

			}
			int score = g.synchronizedPrimitiveScore(state);
			if (score > 0) {
				request.setScore(score);
			}
		}
		String boardString = g.synchronizedStateToString(state);
		request.setBoard(boardString);
		return request;
	}
}
