package edu.berkeley.gamesman;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.avro.ipc.AvroRemoteException;
import org.apache.avro.ipc.HttpServer;
import org.apache.avro.specific.SpecificResponder;

import edu.berkeley.gamesman.core.*;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

import edu.berkeley.gamesman.avro.Fields;
import edu.berkeley.gamesman.avro.GamesmanProvider;
import edu.berkeley.gamesman.avro.PositionValue;
import edu.berkeley.gamesman.avro.VariantSupport;

/**
 * Basic JSON interface for web app usage
 * 
 * @author Patrick Reiter Horn
 */
public class AvroInterface extends GamesmanApplication {
	/**
	 * No arg constructor
	 */
	public AvroInterface() {
	}

	Properties serverConf;

	Map<String, Pair<Configuration, Database>> loadedConfigurations = new HashMap<String, Pair<Configuration, Database>>();

	private URI apiServer;

	@Override
	public int run(Properties props) {
		this.serverConf = props;
		int port = 0;
		try {
			port = Integer
					.parseInt(serverConf.getProperty("avro.port", "8042"));
		} catch (NumberFormatException e) {
			throw new Error("Port must be an integer", e);
		}
		reallyRun(port);
		return 0;
	}

	/**
	 * Run the server
	 * 
	 * @param port
	 *            the port to listen on
	 */
	public void reallyRun(final int port) {
		assert Util.debug(DebugFacility.AVRO, "Loading Avro server...");

		try {
			this.apiServer = new URI(serverConf.getProperty("avro.api.uri",
					"http://solvedgames.appspot.com/api/"));
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}

		GamestateProvider handler = new GamestateProvider();
		SpecificResponder processor = new SpecificResponder(
				GamesmanProvider.class, handler);
		HttpServer server;
		try {
			server = new HttpServer(processor, port);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		server.start();

		try {
			URLConnection conn = apiServer.toURL().openConnection();
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setUseCaches(false);
			conn.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");
			DataOutputStream os = new DataOutputStream(conn.getOutputStream());
			URI serverAddress = new URI("http", null, InetAddress
					.getLocalHost().getHostAddress(), port, "/", null, null);
			os.writeBytes("uri="
					+ URLEncoder.encode(serverAddress.toASCIIString(), "ASCII"));
			os.flush();
			os.close();

			// Want to always print this out.
			System.out.println("Server ready on port " + port + "!");

			server.join();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	/*
	 * public static void main(String[] args) { boolean verbose = false; int i =
	 * 0; if (args.length > 0 && args[0].equals("-v")) { verbose = true; i = 1;
	 * } if (args.length < 2+i) {Util.fatalError(
	 * "Usage: JSONInterface [-v] file:///.../database.db  portnum  [DatabaseClass]"
	 * ); } String dbClass = "BlockDatabase"; String dbURI = args[i]; int port =
	 * Integer.valueOf(args[i+1]); if (args.length > i+2) { dbClass = args[i+2];
	 * } if (verbose) { EnumSet<DebugFacility> debugOpts =
	 * EnumSet.noneOf(DebugFacility.class); ClassLoader cl =
	 * ClassLoader.getSystemClassLoader(); cl.setDefaultAssertionStatus(false);
	 * debugOpts.add(DebugFacility.JSON);
	 * DebugFacility.JSON.setupClassloader(cl);
	 * debugOpts.add(DebugFacility.CORE);
	 * DebugFacility.CORE.setupClassloader(cl); Util.enableDebuging(debugOpts);
	 * } JSONInterface ji = new JSONInterface(); Database db; try { db =
	 * Util.typedInstantiate
	 * ("edu.berkeley.gamesman.database."+dbClass,Database.class); } catch
	 * (ClassNotFoundException e1) {
	 * Util.fatalError("Failed to create database",e1); return; }
	 * db.initialize(dbURI, null); ji.conf = db.getConfiguration(); ji.conf.db =
	 * db; ji.reallyRun(port); }
	 */

	static String sanitise(CharSequence orig) {
		String val = orig.toString();
		val = val.replace('.', '-');
		val = val.replace('\\', '-');
		val = val.replace('/', '-');
		try {
			return URLEncoder.encode(val, "utf-8");
		} catch (java.io.UnsupportedEncodingException e) {
			return "";
		}
	}

	Pair<Configuration, Database> newLoadDatabase(
			Map<CharSequence, CharSequence> params, CharSequence game) {
		/*
		 * String game = params.get("game"); if (game == null) { return null; }
		 */

		String filename = sanitise(game);
		String[] allowedFields = serverConf.getProperty("json.fields." + game,
				"").split(",");
		for (String key : allowedFields) {
			key = key.trim();
			if (key.length() == 0) {
				continue;
			}
			CharSequence val = params.get(key);
			if (val == null) {
				val = "";
			} else {
				val = sanitise(val);
			}
			filename += "_" + key + "_" + val;
		}
		Pair<Configuration, Database> cPair = loadedConfigurations
				.get(filename);
		if (cPair == null) {
			cPair = addDatabase(params, game, filename);
			loadedConfigurations.put(filename, cPair);
		}
		return cPair;
	}

	synchronized Pair<Configuration, Database> addDatabase(
			Map<CharSequence, CharSequence> params, CharSequence game,
			String filename) {
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
				return new Pair<Configuration, Database>(db.conf, db);
			} else {
				assert Util.debug(DebugFacility.JSON, "Database at " + filename
						+ " does not exist! Using unsolved.");
			}
		} catch (Error fe) {
			fe.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String unsolvedJob = serverConf.getProperty("json.unsolved." + game,
				null);
		if (unsolvedJob != null) {
			Properties props = Configuration.readProperties(unsolvedJob);
			String[] allowedFields = serverConf.getProperty(
					"json.fields." + game, "").split(",");
			for (String key : allowedFields) {
				key = key.trim();
				if (key.length() == 0) {
					continue;
				}
				CharSequence val = params.get(key);
				if (val == null) {
					val = "";
				}
				props.setProperty("gamesman.game." + key, val.toString());
			}
			try {
				return new Pair<Configuration, Database>(new Configuration(
						props), null);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				return null;
			}
		} else {
			new Error("Failed to find an appropriate job file for " + game)
					.printStackTrace();
			return null;
		}
	}

	private class GamestateProvider implements GamesmanProvider {
		private edu.berkeley.gamesman.avro.Value getAvroValue(Value val) {
			switch (val) {
			case WIN:
				return edu.berkeley.gamesman.avro.Value.WIN;
			case LOSE:
				return edu.berkeley.gamesman.avro.Value.LOSE;
			case TIE:
				return edu.berkeley.gamesman.avro.Value.TIE;
			case DRAW:
				return edu.berkeley.gamesman.avro.Value.DRAW;
			case UNDECIDED:
			case IMPOSSIBLE:
				return null;
			default:
				return null;
			}
		}

		private <T extends State> PositionValue fillResponseFields(
				Pair<Configuration, Database> cPair, T state, Fields fields) {
			// FIXME: Use fields....
			PositionValue request = new PositionValue();
			Database db = cPair.cdr;
			Configuration conf = cPair.car;
			Game<T> g = conf.getCheckedGame();

			request.position = g.synchronizedStateToString(state);

			if (db != null) {
				Record rec = g.newRecord();
				DatabaseHandle handle = db.getHandle(true);
				try {
					g.synchronizedLongToRecord(
							state,
							db.readRecord(handle,
									g.synchronizedStateToHash(state)), rec);
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (conf.hasValue) {
					Value pv = rec.value;
					// FIXME: We don't want to flip values!
					/*
					 * if (g.getPlayerCount() > 1 && isChildState) pv =
					 * pv.flipValue();
					 */
					request.value = getAvroValue(pv);
				}
				if (conf.hasRemoteness) {
					request.remoteness = (rec.remoteness);
				}
				if (conf.hasScore) {
					request.winBy = (rec.score);
				}
				assert Util.debug(DebugFacility.JSON, "    Response: value="
						+ request.value + "; remote=" + rec.remoteness
						+ "; score=" + rec.score);
			} else {
				Value pv = g.synchronizedStrictPrimitiveValue(state);
				if (pv != Value.UNDECIDED) {
					// FIXME: We don't want to flip values!
					/*
					 * if (g.getPlayerCount() > 1 && isChildState) { if (pv ==
					 * Value.WIN) pv = Value.LOSE; else if (pv == Value.LOSE) pv
					 * = Value.WIN; }
					 */
					request.value = getAvroValue(pv);
				}
				int score = g.synchronizedPrimitiveScore(state);
				if (score > 0) {
					request.winBy = (score);
				}
			}
			/*
			 * String boardString = g.synchronizedStateToString(state); if
			 * (!g.synchronizedStringToState(boardString).equals(state)) throw
			 * new Error("States do not match"); request.board = (boardString);
			 */
			return request;
		}

		@Override
		public Map<CharSequence, List<VariantSupport>> getSupportedGames()
				throws AvroRemoteException {
			Map<CharSequence, List<VariantSupport>> ret = new TreeMap<CharSequence, List<VariantSupport>>();
			VariantSupport supp = new VariantSupport();
			supp.variant = new TreeMap<CharSequence, CharSequence>();
			supp.variant.put("width", "4");
			supp.variant.put("height", "4");
			supp.variant.put("pieces", "4");
			supp.fields.value = true;
			List<VariantSupport> testList = new ArrayList<VariantSupport>();
			testList.add(supp);
			ret.put("connect4", testList);
			return ret;
		}

		@Override
		public PositionValue getInitialPositionValue(CharSequence gameName,
				Map<CharSequence, CharSequence> variant, Fields fields)
				throws AvroRemoteException {
			Pair<Configuration, Database> cPair = newLoadDatabase(variant,
					gameName);
			Configuration config = cPair.car;
			Game<State> game = config.getCheckedGame();

			int count = 0;
			State chosenState = null;
			for (State state : game.startingPositions()) {
				count++;
				if (Math.random() <= 1.0 / count) {
					chosenState = state;
				}
			}

			if (chosenState != null) {
				return fillResponseFields(cPair, chosenState, fields);
			}
			return null;
		}

		@Override
		public Map<CharSequence, PositionValue> getPositionValues(
				CharSequence gameName, Map<CharSequence, CharSequence> variant,
				List<CharSequence> positions, Fields fields)
				throws AvroRemoteException {
			Map<CharSequence, PositionValue> response = new TreeMap<CharSequence, PositionValue>();
			System.out.println("getMoveValue request for \"" + gameName
					+ "\": " + variant);
			try {
				Pair<Configuration, Database> cPair = newLoadDatabase(variant,
						gameName);
				Configuration config = cPair.car;
				Game<State> game = config.getCheckedGame();

				for (CharSequence board : positions) {
					State state = game.synchronizedStringToState(board
							.toString());
					response.put(board,
							fillResponseFields(cPair, state, fields));
				}
			} catch (RuntimeException e) {
				e.printStackTrace();
				throw new AvroRemoteException(e);
			}
			return response;
		}

		@Override
		public Map<CharSequence, PositionValue> getNextPositionValues(
				CharSequence gameName, Map<CharSequence, CharSequence> variant,
				CharSequence position, Fields fields)
				throws AvroRemoteException {
			Map<CharSequence, PositionValue> response = new TreeMap<CharSequence, PositionValue>();
			System.out.println("getMoveValue request for \"" + gameName
					+ "\": " + variant);
			try {
				final Pair<Configuration, Database> cPair = newLoadDatabase(
						variant, gameName);
				final Configuration config = cPair.car;
				final Fields syncFields = fields;
				final Game<State> game = config.getCheckedGame();
				final Map<CharSequence, PositionValue> syncMap = Collections
						.synchronizedMap(response);

				State state = game.synchronizedStringToState(position
						.toString());
				Value pv = game.synchronizedStrictPrimitiveValue(state);
				if (game.getPlayerCount() >= 1 && pv != Value.UNDECIDED) {
					return response;
				}

				List<Thread> threads = new ArrayList<Thread>();
				for (Pair<String, State> moveTemp : game
						.synchronizedValidMoves(state)) {
					final Pair<String, State> move = moveTemp;
					threads.add(new Thread() {
						public void run() {
							syncMap.put(
									move.car,
									fillResponseFields(cPair, move.cdr,
											syncFields));
						}
					});
					threads.get(threads.size() - 1).start();
				}
				for (Thread t : threads) {
					while (t.isAlive()) {
						try {
							t.join();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			} catch (RuntimeException e) {
				e.printStackTrace();
				throw new AvroRemoteException(e);
			}
			return response;
		}

		@Override
		public void solve(CharSequence game,
				Map<CharSequence, CharSequence> variant, CharSequence pingback) {
			// TODO Auto-generated method stub

		}
	}
}
