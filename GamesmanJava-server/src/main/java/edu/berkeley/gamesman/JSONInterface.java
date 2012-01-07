package edu.berkeley.gamesman;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.thrift.TException;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;

import edu.berkeley.gamesman.thrift.GamestateRequestHandler;
import edu.berkeley.gamesman.thrift.GamestateRequestHandler.Iface;
import edu.berkeley.gamesman.thrift.GamestateResponse;
import edu.berkeley.gamesman.thrift.GetMoveResponse;
import edu.berkeley.gamesman.thrift.GetNextMoveResponse;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Util;

/**
 * Basic JSON interface for web app usage
 * 
 * @author Steven Schlansker
 */
public class JSONInterface implements Iface {
	private static final Map<String, Opener> gameOpeners = Collections
			.synchronizedMap(new HashMap<String, Opener>());

	public static void addOpener(String game, Opener opener) {
		gameOpeners.put(game, opener);
	}

	private final DefaultOpener defaultOpen;

	/**
	 * No arg constructor
	 */
	public JSONInterface(Properties props) {
		this.serverConf = props;
		defaultOpen = new DefaultOpener(props);
	}

	private final Properties serverConf;

	private final Map<String, RecordFetcher> loadedConfigurations = new HashMap<String, RecordFetcher>();

	public int run() {
		/*
		 * try { db = Util.typedInstantiate(, Database.class); } catch
		 * (ClassNotFoundException e1) {
		 * Util.fatalError("Failed to create database",e1); }
		 * db.initialize(inconf.getProperty("gamesman.db.uri"), null); this.conf
		 * = db.getConfiguration();
		 */
		int port = 0;
		try {
			port = Integer
					.parseInt(serverConf.getProperty("json.port", "4242"));
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
		assert Util.debug(DebugFacility.JSON, "Loading JSON server...");

		// Want to always print this out.
		System.out.println("Server ready on port " + port + "!");

		GamestateRequestHandler.Processor processor = new GamestateRequestHandler.Processor(
				this);

		TServerTransport serverTransport;
		try {
			serverTransport = new TServerSocket(port);
			TServer threaded = new TThreadPoolServer(processor, serverTransport);

			System.out.println("Starting the server...");
			threaded.serve();

		} catch (TTransportException e) {
			throw new Error("Could not start server on port " + port, e);
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

	private static String sanitise(String val) {
		val = val.replace('.', '-');
		val = val.replace('\\', '-');
		val = val.replace('/', '-');
		try {
			return URLEncoder.encode(val, "utf-8");
		} catch (java.io.UnsupportedEncodingException e) {
			return "";
		}
	}

	private RecordFetcher newLoadDatabase(Map<String, String> params,
			String game) {
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
			String val = params.get(key);
			if (val == null) {
				val = "";
			} else {
				val = sanitise(val);
			}
			filename += "_" + key + "_" + val;
		}
		RecordFetcher cPair;
		synchronized (this) {
			cPair = loadedConfigurations.get(filename);
			if (cPair == null) {
				Opener opener = gameOpeners.get(game);
				if (opener == null)
					opener = defaultOpen;
				cPair = opener.addDatabase(params, game, filename);
				loadedConfigurations.put(filename, cPair);
			}
		}
		return cPair;
	}

	public GetNextMoveResponse getNextMoveValues(String game,
			String configuration) throws TException {
		GetNextMoveResponse response = new GetNextMoveResponse();
		Map<String, String> params = reconstructGameParams(configuration);
		System.out.println("getNextMoveValue request " + params);
		try {
			response.setResponse(getNextMoveValues_core(game, params));
			response.setStatus("ok");
		} catch (RuntimeException e) {
			e.printStackTrace();
			response.setStatus("error");
			response.setMessage(e.getMessage());
		}
		return response;
	}

	public GetMoveResponse getMoveValue(String game, String configuration)
			throws TException {
		GetMoveResponse response = new GetMoveResponse();
		Map<String, String> params = reconstructGameParams(configuration);
		System.out.println("getMoveValue request for \"" + game + "\": "
				+ params);
		try {
			response.setResponse(getMoveValue_core(game, params));
			response.setStatus("ok");
		} catch (RuntimeException e) {
			e.printStackTrace();
			response.setStatus("error");
			response.setMessage(e.getMessage());
		}
		return response;
	}

	private List<GamestateResponse> getNextMoveValues_core(String gamename,
			Map<String, String> params) throws TException {

		String board = params.get("board");
		if (board == null) {
			throw new TException("No board passed!");
		}

		final RecordFetcher cPair = newLoadDatabase(params, gamename);
		return cPair.getNextMoveValues(board);
	}

	public GamestateResponse getMoveValue_core(String gamename,
			Map<String, String> params) throws TException {
		String board = params.get("board");
		if (board == null) {
			throw new TException("No board passed!");
		}

		final RecordFetcher cPair = newLoadDatabase(params, gamename);
		return cPair.getMoveValue(board);
	}

	/**
	 * Returns a Map containing the params and corresponding values from the
	 * configuration given Ex: {"board": +++++++++}
	 * 
	 * @param configuration
	 * @return
	 */
	private Map<String, String> reconstructGameParams(String configuration) {
		Map<String, String> j = new HashMap<String, String>();
		String line = configuration.replace(';', '&');
		for (String param : line.split("&")) {
			String[] key_val = param.split("=", 2);
			if (key_val.length != 2) {
				continue;
			}
			try {
				j.put(URLDecoder.decode(key_val[0], "utf-8"),
						URLDecoder.decode(key_val[1], "utf-8"));
			} catch (UnsupportedEncodingException e) {
			}
		}
		return j;
	}
}
