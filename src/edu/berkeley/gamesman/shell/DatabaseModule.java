package edu.berkeley.gamesman.shell;



import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Properties;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Game;
import edu.berkeley.gamesman.core.Hasher;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.RecordFields;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Util;
import edu.berkeley.gamesman.core.Database;

/**
 * 
 * @author Jin-Su Oh
 *	DatabaseModules handles user's input to manipulate the database file.
 *	It provides the ability open and close database, and read, edit and view records.
 * 	@db holds the opened database file.
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
	 * @see "openDatabase()"
	 */
	protected void u_openDatabase(ArrayList<String> args) {	
		openDatabase();
		System.out.println("Opened database.");
	}
	
	/**
	 * openDatabase uses the configuration class's openDatabase function to open the database file.
	 * processCommand("i") instantiates game and hasher property.
	 */
	private void openDatabase() {
		proccessCommand("i");
		try {
			db = conf.openDatabase();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * u_closeDatabase closes the dbFile that's opened.
	 * @param args it doesn't check for any args, if given.
	 */
	protected void u_closeDatabase(ArrayList<String> args) {
		if(db != null) {
				db.close();
				db = null;
				loc = BigInteger.ZERO;
				System.out.println("Closed the current database.");
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
	 * @param args (arg0,arg1) No space in between the arguments
	 * 		argument(ie. arg1, arg2...) can be a Primitive Type as in win, lose, tie and undecided(cases don't matter) or
	 * 		a Remoteness value.
	 * If the argument is a number, it will automatically be considered as Remoteness value.
	 * If the argument is w, l, t, u, Win, Lose, Tie, or Undecided, it will be considered as Value of the record.
	 * If there are more than 2 arguments passed in, only first two will be considered.
	 */	
	protected void u_editRecord(ArrayList<String> args) {
		boolean isValid = true;
		if(curRecord != null) {
			if(!args.isEmpty()) {
				int n = 0;
				while(n < 2 && n < args.size()) { //do it for args.get(0) and args.get(1)
					if(args.get(n).toLowerCase().equals("w") || args.get(n).equalsIgnoreCase("win")) {
						curRecord.set(RecordFields.VALUE, PrimitiveValue.WIN.value());
					} else if(args.get(n).toLowerCase().equals("l") || args.get(n).equalsIgnoreCase("lose")) {
						curRecord.set(RecordFields.VALUE, PrimitiveValue.LOSE.value());
					} else if(args.get(n).toLowerCase().equals("t") || args.get(n).equalsIgnoreCase("tie")) {
						curRecord.set(RecordFields.VALUE, PrimitiveValue.TIE.value());
					} else if(args.get(n).toLowerCase().equals("u") || args.get(n).equalsIgnoreCase("undecided")) {
						curRecord.set(RecordFields.VALUE, PrimitiveValue.UNDECIDED.value());
					} else if(isParsableToInt(args.get(n))) {
						curRecord.set(RecordFields.REMOTENESS, Integer.parseInt(args.get(n)));
					} else {
						System.out.println("Invalid arguments to editRecord");
						isValid = false;
					}
					if(isValid) {
						db.putRecord(loc.subtract(BigInteger.ONE), curRecord); //because readRecord increases Loc by one.
						System.out.println("changed to: " + curRecord);
					}
					n++;
				}
			}
		} else {
			System.out.println("No record has been read yet.");
		}
	}
	
	/**
	 * isParsableToInt returns true if the string given is a string that represents a int number, and returns false
	 * otherwise.
	 * @param i String that is to be tested.
	 * @return
	 */
	private boolean isParsableToInt(String i) {
		try
		{
			Integer.parseInt(i);
			return true;
		}
		catch(NumberFormatException nfe)
		{
			return false;
		}		
	}
	
	/**
	 * initializes the game and hasher property
	 * @param args we don't check for any args given.
	 */
	protected void u_initializeConfiguration(ArrayList<String> args) throws ClassNotFoundException {
		conf.initialize(conf.getProperty("gamesman.game"), conf.getProperty("gamesman.hasher"));
		
	}
	
	/**
	 * quit the program.
	 */
	public void quit() {
	}
}
