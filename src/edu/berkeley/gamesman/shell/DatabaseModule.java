package edu.berkeley.gamesman.shell;



import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Properties;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Game;
import edu.berkeley.gamesman.core.Hasher;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.RecordFields;
import edu.berkeley.gamesman.util.Util;
import edu.berkeley.gamesman.core.Database;

/**
 * 
 * @author Jin-Su Oh
 *	DatabaseModules handles user's input to manipulate the database file.
 *	It provides the ability open and close database, and read, edit and view records.
 * 	db holds the opened database file.
 *  curRecord holds a record that is read.
 *  loc keeps track of where in the database file user is accessing.  
 */

public class DatabaseModule extends UIModule {
	private Database db;
	private Record curRecord;
	private BigInteger loc;
	
	/**
	 * constructor sets required properties and helpLines.
	 * @param c configuration that the game works with.
	 */
	public DatabaseModule(Configuration c) {
		super(c, "data");
		loc = BigInteger.ZERO;
		requiredPropKeys = new ArrayList<String>();
		requiredPropKeys.add("gamesman.db.uri");
		requiredPropKeys.add("gamesman.game");
		requiredPropKeys.add("gamesman.hasher");
		requiredPropKeys.add("gamesman.solver");
		requiredPropKeys.add("gamesman.database");
		
		helpLines = new Properties();
		
		helpLines.setProperty("openDatabase", 
				"open a database file from the current configuration.");
		helpLines.setProperty("closeDatabase",
				"close a database file that this module was working on.");
		helpLines.setProperty("readRecord",
				"read a record from the database file." +
				"\n\treadRecord(location)" +
				"\n\tlocation : location of the record(indexed from 0).");
		helpLines.setProperty("viewRecord",
				"view the record that has been read into the system.");
		helpLines.setProperty("editRecord", 
				"edit the current record." +
				"\n\teditRecord(field, value)" +
				"\n\tfield : field is either Primitive(u can type p), or Remoteness(u can type r)" +
				"\n\tvalue : in Primitive - 0 is lose, 1 is tie, 2 is win.  in Remoteness - value is how far you are away from winning.");
		helpLines.setProperty("initializeConfiguration", "initializes configuration.");
	}
	
	/**
	 * u_openDatabase calls openDatabase.
	 * @param args it doesn't check for any args given.
	 */
	protected void u_openDatabase(ArrayList<String> args) {	
		openDatabase();
	}
	
	/**
	 * openDatabase uses the configuration class's openDatabase function to open the database file.
	 * processCommand("i") instantiates game and hasher property.
	 */
	private void openDatabase() {
		proccessCommand("i");
		db = conf.openDatabase();
	}
	
	/**
	 * u_closeDatabase closes the dbFile that's opened.
	 * @param args it doesn't check for any args, if given.
	 */
	protected void u_closeDatabase(ArrayList<String> args) {
		if(db != null) {
				db.close();
		}
	}
	
	/**
	 * u_readRecord reads a record into curRecord.
	 * @param args what we expect is a number from the user which represent the index of the record.
	 * if args is empty, then this function proceeds to in default behavior which reads the record at loc.
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
	 * @param args we don't check for any args given.
	 */
	protected void u_viewRecord(ArrayList<String> args) {
		System.out.println(curRecord);
	}
	
	/**
	 * u_editRecord edits the last Record that was read by the user.
	 * @param args (field, value)
			field : field is either Primitive(u can type p), or Remoteness(u can type r)
			value : in Primitive - 0 is lose, 1 is tie, 2 is win.  
					in Remoteness - value is how far you are away from winning.
	 */	
	protected void u_editRecord(ArrayList<String> args) {
		if(curRecord != null) {
			if(!args.isEmpty()) {
				if(args.get(0).toLowerCase().startsWith("r")) {
					curRecord.set(RecordFields.REMOTENESS, Integer.parseInt(args.get(1)));					
				} else if(args.get(0).toLowerCase().startsWith("p")) {
					curRecord.set(RecordFields.VALUE, Integer.parseInt(args.get(1)));
				} else {
					Util.fatalError("Invalid arguments to editRecord");
				}
				db.putRecord(loc, curRecord);
				System.out.println("changed to: " + curRecord);
			}
		}		
	}
	
	/**
	 * initializes the game and hasher property
	 * @param args we don't check for any args given.
	 */
	protected void u_initializeConfiguration(ArrayList<String> args) {
		Game<?> g = Util.typedInstantiateArg("edu.berkeley.gamesman.game."+conf.getProperty("gamesman.game"), conf);
		Hasher<?> h = Util.typedInstantiateArg("edu.berkeley.gamesman.hasher."+conf.getProperty("gamesman.hasher"), conf);
		conf.initialize(g, h);
		
	}
}
