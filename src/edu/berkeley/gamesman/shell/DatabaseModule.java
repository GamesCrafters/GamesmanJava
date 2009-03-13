package edu.berkeley.gamesman.shell;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Properties;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Game;
import edu.berkeley.gamesman.core.Hasher;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.RecordFields;
import edu.berkeley.gamesman.util.Util;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.database.BlockDatabase;

public class DatabaseModule extends UIModule {
	private Database db;
	private static Record curRecord;
	private BigInteger loc;
	
	public DatabaseModule(Configuration c) {
		super(c, "data");
		loc = BigInteger.ZERO;
		requiredPropKeys = new ArrayList<String>();
		requiredPropKeys.add("gamesman.db.uri");
		requiredPropKeys.add("gamesman.game");
		requiredPropKeys.add("gamesman.hasher");
		requiredPropKeys.add("gamesman.solver");
		requiredPropKeys.add("gamesman.database");
		//what property would it want? 
		//maybe that if conf has a database path?
		
		helpLines = new Properties();
		
		helpLines.setProperty("openDatabase", 
				"open a database file from the current configuration.");
		helpLines.setProperty("closeDatabase",
				"close a database file that this module was working on.");
		helpLines.setProperty("readRecord(location)",
				"read a record from the database file.\n\tlocation : location of the record(indexed from 0).");
		helpLines.setProperty("viewRecord",
				"view the record that has been read into the system.");
		//helpLines.setProperty("editRecord", "");		
	}
	
	/**
	 * u_openDatabase calls openDatabase with curConf.
	 */
	protected void u_openDatabase(ArrayList<String> args) {	
		openDatabase();
	}
	
	/**
	 * openDatabase takes a configuration and instantiates dbFile as a RandomAccessFile for the user to read.
	 * it's in RandomAccessFile because it uses Record class later on.
	 * @param conf configuration it's trying to read a database file from.
	 */
	private void openDatabase() {
		proccessCommand("i");
		db = conf.openDatabase();
		/*
		db = new BlockDatabase();
		URI f;
		try {
			f = new URI(conf.getProperty("gamesman.db.uri"));
			System.out.println(f);
			db.initialize(f.toString(), conf);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		/*
		try {
			f = new URI(conf.getProperty("gamesman.db.uri"));
			db.initialize(f.toString(), conf);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
	}
	
	/**
	 * u_closeDatabase closes the dbFile that's opened.
	 */
	protected void u_closeDatabase(ArrayList<String> args) {
		if(db != null) {
				db.close();
		}
	}
	
	/**
	 * u_readRecord reads a record from theRecord into curRecord.
	 */	
	protected void u_readRecord(ArrayList<String> args) {
		if(db != null) {
			if(!args.isEmpty())
				loc = new BigInteger(args.get(0));
			curRecord = db.getRecord(loc);
			System.out.println("Read a record at location " + loc);
			System.out.println(curRecord);
			loc = loc.add(BigInteger.ONE);
		} else {
			openDatabase();
			System.out.println("GamesmanJava has opened the database file for you.");
			u_readRecord(args);
		}
	}
	
	/**
	 * u_viewRecord prints out curRecord
	 */
	protected void u_viewRecord(ArrayList<String> args) {
		System.out.println(curRecord);
	}
	
	/**
	 * u_editRecord edits the curRecord
	 */
	//I don't know what you would put in to change the record.	
	protected void u_editRecord(ArrayList<String> args) {
		if(curRecord != null) {
			if(!args.isEmpty()) {
				if(args.get(0).toLowerCase().startsWith("r")) {
					curRecord.set(RecordFields.REMOTENESS, Integer.parseInt(args.get(1)));
					System.out.println("changed to: " + curRecord);
				} else if(args.get(0).toLowerCase().startsWith("p")) {
					curRecord.set(RecordFields.VALUE, Integer.parseInt(args.get(1)));
					System.out.println("changed to: " + curRecord);
				} else {
					Util.fatalError("Invalid arguments to editRecord");
				}
			}
		}		
	}
	
	protected void u_initializeConfiguration(ArrayList<String> args) {
		Game<?> g = Util.typedInstantiateArg("edu.berkeley.gamesman.game."+conf.getProperty("gamesman.game"), conf);
		Hasher<?> h = Util.typedInstantiateArg("edu.berkeley.gamesman.hasher."+conf.getProperty("gamesman.hasher"), conf);
		conf.initialize(g, h);
		
	}
}

/*
 * ability to open database, close, 
 * read records, edit records, view records
 */