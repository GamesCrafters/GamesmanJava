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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
public class JSONInterface {
	
	Database db;
	Configuration conf;
	
	/**
	 * Launch a JSON server for use by GamesmanWeb
	 * @param args arguments
	 */
	public static void main(String[] args){
		new JSONInterface().run(args);
	}

	/**
	 * @param args arguments
	 */
	public void run(String[] args) {
		ServerSocket ssock = null;
		if(args.length != 1){
			Util.fatalError("You must specify a configuration file as the only command line argument");
		}
		conf = new Configuration(args[0]);
		
		Util.debugInit(conf);
		
		Util.debug(DebugFacility.JSON, "Loading JSON server...");
		
		int port = 0;
		try {
			port = conf.getInteger("gamesman.server.jsonport", 4242);
			ssock = new ServerSocket(port);
		} catch (NumberFormatException e) {
			Util.fatalError("Port must be an integer",e);
		} catch (IOException e) {
			Util.fatalError("Could not listen on port",e);
		}
		
		db = Util.typedInstantiate("edu.berkeley.gamesman.database."+conf.getProperty("gamesman.database","BlockDatabase"));
		db.initialize(conf.getProperty("gamesman.db.uri"), null);
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
	
	
	private class JSONThread<S> extends Thread {

		final Socket s;
		final Game<S> g;
		
		JSONThread(Socket s){
			this.s = s;
			g = Util.checkedCast(conf.getGame());
			Util.debug(DebugFacility.JSON,"Accepted new connection ",s);
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
					Util.debug(DebugFacility.JSON, "Connection closed.");
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
						for(Pair<String,S> next : g.validMoves(state)){
							JSONObject entry = new JSONObject();
							entry.put("move", next.car);
							fillJSONFields(entry, next.cdr);
							response.accumulate("response", entry);
						}
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
