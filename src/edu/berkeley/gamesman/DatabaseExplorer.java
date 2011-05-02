package edu.berkeley.gamesman;

import java.io.IOException;
import java.util.Scanner;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.database.DummyDatabase;
import edu.berkeley.gamesman.game.Game;

/**
 * Outcomes = edu.berkeley.gamesman.core.Value displayBoard =
 * edu.berkeley.gamesman.game.Game.displayState getCurrentOutcome = {Hash
 * position, fetch record from database, unhash it}
 * getNumberOfMovesForCurrentOutcome = {Look at the size of the collection
 * returned by validMoves} getValidMoves =
 * edu.berkeley.gamesman.game.Game.validMoves {with only one argument}
 */
public final class DatabaseExplorer {
	/**
	 * The main method for exploring a database(jump to random hash and display) inside the console.
	 * 
	 * @param args
	 *            The path to a job file or database file. Job files end in
	 *            .job, otherwise it's assumed to be a database file
	 * @throws IOException
	 *             If an IO exception occurs while reading the file
	 * @throws ClassNotFoundException
	 *             If the configuration contains a nonexistant class
	 */
	public static void main(String[] args) throws IOException,
			ClassNotFoundException {
		Configuration conf;
		Database db;
		if (args[0].endsWith(".job")) {
			conf = new Configuration(args[0]);
			db = new DummyDatabase(conf, true, false);
		} else {
			db = Database.openDatabase(args[0]);
			// Opens a solved database file
			conf = db.conf;
			// Fetch the configuration information from that database
		}
		Game<? extends State> g = conf.getGame();
		// Get the game object for the current game
		playGame(g, db);
	}

	private static <S extends State> void playGame(Game<S> g, Database db)
			throws IOException {
		S position = g.startingPositions().iterator().next();
		/*
		 * It's possible different variants of a game can be the same except for
		 * the starting position. This is just taking the first starting
		 * position (ideally, there would be a better way to handle this, such
		 * as having a configuration option)
		 */
		Scanner scan = new Scanner(System.in);
		Record storeRecord = g.newRecord();
		// Creates a Record object for storing unhashed records retrieved from
		// the database
		DatabaseHandle dh = db.getHandle(true);
		// Each thread accessing a database must maintain its own DatabaseHandle
		// for that Database. This deals with many multi-threading issues
		while (true) {
			System.out.println(g.displayState(position));
			long readRecord = db.readRecord(dh, g.stateToHash(position));
			g.longToRecord(position,
					readRecord, storeRecord);
			// This line hashes the current position, fetches the appropriate
			// record from the database and unhashes it
			System.out.println("Record long: " + readRecord);
			System.out.println(storeRecord);
			System.out.println("Primitive value: " + g.primitiveValue(position));
			System.out.println("Strict Primitive value: " + g.strictPrimitiveValue(position));

			System.out.println("Enter hash to jump to (or exit to exit):");
			
			if (scan.hasNext()) {
				String inputString = scan.nextLine();
				if(inputString.equals("exit"))
				{
					return;
				}
				
				long newPos = Long.parseLong(inputString);
				g.hashToState(newPos, position);
			} else
				return;
		}
	}
}
