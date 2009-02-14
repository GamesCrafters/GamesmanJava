package edu.berkeley.gamesman;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Game;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.RecordFields;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

public class JSONInterface implements Runnable {
	
	private Database db;
	private Configuration conf;
	final String [] args;
	boolean done;
	ServerSocket ssock;
	
	public JSONInterface(String[] args){
		this.args = args;
	}
	
	/**
	 * Launch a JSON server for use by GamesmanWeb
	 * @param args arguments
	 */
	public static void main(String[] args){
		JSONInterface j = new JSONInterface(args);
		Thread t = new Thread(j);
		t.setName("JSON Master Server");
		t.start();
	}

	/**
	 * @param args arguments
	 */
	public void run() {
		ssock = null;
		if(args.length != 1){
			Util.fatalError("You must specify a configuration file as the only command line argument");
		}
		conf = new Configuration(System.getProperties());
		conf.addProperties(args[0]);
		
		Util.debugInit(conf);
		
		Util.debug(DebugFacility.JSON, "Loading JSON server...");
		
		try {
			ssock = new ServerSocket(Integer.parseInt(conf.getProperty("gamesman.server.jsonport")));
		} catch (NumberFormatException e) {
			Util.fatalError("Port must be an integer",e);
		} catch (IOException e) {
			Util.fatalError("Could not listen on port",e);
		}
		
		db = Util.typedInstantiate("edu.berkeley.gamesman.database."+conf.getProperty("gamesman.database","BlockDatabase"));
		db.initialize(conf.getProperty("gamesman.db.uri"), null);
		conf = db.getConfiguration();
		
		Util.debug(DebugFacility.JSON, "Server ready!");
		while(!done){
			Socket s;
			try {
				s = ssock.accept();
			} catch (IOException e) {
				if(!done)
					Util.warn("IO Exception while accepting: "+e);
				break;
			}
			JSONThread<?> t = new JSONThread<Object>(s);
			t.setName("JSONThread "+s);
			t.start();
		}
		
		Util.debug(DebugFacility.JSON,"Shutting down JSON server cleanly");
		db.close();
		
	}
	
	
	private class JSONThread<S> extends Thread {

		final Socket s;
		final Game<S> g;
		
		JSONThread(Socket s){
			this.s = s;
			g = Util.checkedCast(conf.getGame());
			Util.debug(DebugFacility.JSON,"Accepted new connection "+s);
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
				if(line.equals("###quit")){
					done = true;
					try {
						ssock.close();
					} catch (IOException e) {
						Util.fatalError("Could not shut down server",e);
					}
					return;
				}
				JSONObject j;
				try {
					j = new JSONObject("{"+line+"}");
				} catch (JSONException e) {
					Util.warn("Could not parse JSON: \n"+line);
					break;
				}
				
				JSONObject response = new JSONObject();
				
				try {
					String board = j.getString("board");
					
					S state = g.stringToState(board);
					
					for(Pair<String,S> next : g.validMoves(state)){
						JSONObject entry = new JSONObject();
						Record rec = db.getRecord(g.stateToHash(next.cdr));
						for(RecordFields f : conf.getStoredFields().keySet()){
							if(f == RecordFields.Value)
								entry.put(f.name(), PrimitiveValue.values()[(int) rec.get(f)]);
							else
								entry.put(f.name(),rec.get(f));
							entry.put("move",next.car);
							entry.put("board", g.stateToString(next.cdr));
						}
						response.accumulate("response", entry);
					}
					
					response.put("status","ok");
				} catch (JSONException e) {
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
