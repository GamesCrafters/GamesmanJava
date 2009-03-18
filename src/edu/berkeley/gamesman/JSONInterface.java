package edu.berkeley.gamesman;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Game;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.RecordFields;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * Basic JSON interface for web app usage
 * @author Steven Schlansker
 */
public class JSONInterface extends GamesmanApplication {
	Database db;

	/**
	 * No arg constructor
	 */
	public JSONInterface() {}
	
	Configuration conf;
	@Override
	public int run(Configuration conf) {
		
		try {
			db = Util.typedInstantiate("edu.berkeley.gamesman.database."+conf.getProperty("gamesman.database","BlockDatabase"),
					Database.class);
		} catch (ClassNotFoundException e1) {
			Util.fatalError("Failed to create database",e1);
		}
		db.initialize(conf.getProperty("gamesman.db.uri"), null);
		this.conf = db.getConfiguration();
		int port = 0;
		try {
			port = conf.getInteger("gamesman.server.jsonport", 4242);
		} catch (NumberFormatException e) {
			Util.fatalError("Port must be an integer",e);
		}
		reallyRun(port);
		return 0;
	}
	
	public void reallyRun(int port) {
		assert Util.debug(DebugFacility.JSON, "Loading JSON server...");
		
		ServerSocket ssock;
		try {
			ssock = new ServerSocket(port);
		} catch (IOException e) {
			Util.fatalError("Could not listen on port "+port,e);
			return;
		}
		conf = db.getConfiguration();
		
		// Want to always print this out.
		System.out.println("Server ready on port "+port+"!");
		while(true){
			Socket s;
			try {
				s = ssock.accept();
			} catch (IOException e) {
				Util.warn("IO Exception while accepting: "+e);
				break;
			}
			JSONThread<?> t = new JSONThread<Object>(s);
			t.setName("JSONThread "+s);
			t.start();
		}
	}
	
	public static void main(String[] args) {
		boolean verbose = false;
		int i = 0;
		if (args.length > 0 && args[0].equals("-v")) {
			verbose = true;
			i = 1;
		}
		if (args.length < 2+i) {
			Util.fatalError("Usage: JSONInterface [-v] file:///.../database.db  portnum  [DatabaseClass]");
		}
		String dbClass = "BlockDatabase";
		String dbURI = args[i];
		int port = Integer.valueOf(args[i+1]);
		if (args.length > i+2) {
			dbClass = args[i+2];
		}
		if (verbose) {
			EnumSet<DebugFacility> debugOpts = EnumSet.noneOf(DebugFacility.class);
			ClassLoader cl = ClassLoader.getSystemClassLoader();
			cl.setDefaultAssertionStatus(false);
			debugOpts.add(DebugFacility.JSON);
			DebugFacility.JSON.setupClassloader(cl);
			debugOpts.add(DebugFacility.CORE);
			DebugFacility.CORE.setupClassloader(cl);
			Util.enableDebuging(debugOpts);
		}
		JSONInterface ji = new JSONInterface();
		try {
			ji.db = Util.typedInstantiate("edu.berkeley.gamesman.database."+dbClass,Database.class);
		} catch (ClassNotFoundException e1) {
			Util.fatalError("Failed to create database",e1);
			return;
		}
		ji.db.initialize(dbURI, null);
		ji.conf = ji.db.getConfiguration();
		ji.reallyRun(port);
	}
	
	private class JSONThread<S> extends Thread {

		final Socket s;
		final Game<S> g;
		
		JSONThread(Socket s){
			this.s = s;
			g = Util.checkedCast(conf.getGame());
			assert Util.debug(DebugFacility.JSON, "Accepted new connection " + s);
		}
		
		private void fillJSONFields(JSONObject entry, S state) throws JSONException {
			Set<RecordFields> storedFields = 
				conf.getStoredFields().keySet();
			Record rec = db.getRecord(g.stateToHash(state));
			for(RecordFields f : storedFields){
				if (f == RecordFields.VALUE) {
					entry.put(f.name().toLowerCase(),rec.get().name().toLowerCase());
				} else {
					entry.put(f.name().toLowerCase(),rec.get(f));
				}
			}
			entry.put("board", g.stateToString(state));
		}
		
		public void run() {
			LineNumberReader r = null;
			PrintWriter w = null;
			try{
				r = new LineNumberReader(new InputStreamReader(s.getInputStream()));
				w = new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
			}catch (IOException e) {
				Util.warn("Got IO exception in JSONThread: "+e);
			}
			
			while(true){
				String line;
				try {
					line = r.readLine();
				} catch (IOException e) {
					Util.warn("Unable to read line from socket, dropping");
					break;
				}
				if (line == null) {
					assert Util.debug(DebugFacility.JSON, "Connection closed.");
					break; // connection closed.
				}
				Map<String,String> j = new HashMap<String,String>();
				line = line.replace(';', '&');
				for (String param : line.split("&")) {
					String[] key_val = param.split("=");
					if (key_val.length != 2) {
						continue;
					}
					try {
						j.put(URLDecoder.decode(key_val[0],"utf-8"),
								URLDecoder.decode(key_val[1],"utf-8"));
					} catch (UnsupportedEncodingException e) {
					}
				}
				
				JSONObject response = new JSONObject();
				
				try {
					String method = j.get("method");
					if (method == null) {
						response.put("status", "error");
						response.put("msg","No method specified!");
					} else if (method.equals("getNextMoveValues")) {
						String board = j.get("board");
						if (board == null) {
							throw new RuntimeException("No board passed!");
						}
						S state = g.stringToState(board);
						JSONArray responseArray = new JSONArray();
						for(Pair<String,S> next : g.validMoves(state)){
							JSONObject entry = new JSONObject();
							entry.put("move", next.car);
							fillJSONFields(entry, next.cdr);
							responseArray.put(entry);
						}
						response.put("response", responseArray);
						response.put("status","ok");
					} else if (method.equals("getMoveValue")) {
						String board = j.get("board");
						if (board == null) {
							throw new RuntimeException("No board passed!");
						}
						S state = g.stringToState(board);
						JSONObject entry = new JSONObject();
						fillJSONFields(entry, state);
						response.put("response", entry);
						response.put("status","ok");
					}
				} catch (Exception e) {
					try {
						response.put("status", "error");
						e.printStackTrace();
						response.put("msg", "An exception was generated: "+e);
					} catch (JSONException e1) {
						Util.warn("Could not send an error response to client: "+e);
						break;
					}
				}
				w.println(response.toString());
				w.flush();
			}
		}
	}
}
