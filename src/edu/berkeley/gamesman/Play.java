package edu.berkeley.gamesman;

import java.io.IOException;
import java.util.Collection;
import java.util.Scanner;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.database.DummyDatabase;
import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.util.Pair;

/*
 * Outcomes = edu.berkeley.gamesman.core.Value
 * displayBoard = edu.berkeley.gamesman.game.Game.displayState
 * getCurrentOutcome = {Hash position, fetch record from database, unhash it}
 * getNumberOfMovesForCurrentOutcome = {Look at the size of the collection returned by validMoves}
 * getValidMoves = edu.berkeley.gamesman.game.Game.validMoves {with only one argument}
 */
public final class Play {
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

	public static <S extends State> void playGame(Game<S> g, Database db)
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
		while (g.strictPrimitiveValue(position) == Value.UNDECIDED) {
			System.out.println(g.displayState(position));
			g.longToRecord(position,
					db.readRecord(dh, g.stateToHash(position)), storeRecord);
			// This line hashes the current position, fetches the appropriate
			// record from the database and unhashes it
			System.out.println(storeRecord);
			Collection<Pair<String, S>> moves = g.validMoves(position);
			// moves contains a list of pairs, car=The string that "names" the
			// move, cdr = The position that results
			StringBuilder availableMoves = new StringBuilder(
					"Available Moves: ");
			for (Pair<String, S> move : moves) {
				availableMoves.append(move.car);
				availableMoves.append(", ");
			}
			System.out.println(availableMoves);
			if (scan.hasNext()) {
				String moveString = scan.nextLine();
				for (Pair<String, S> move : moves) {
					if (move.car.toUpperCase().equals(moveString.toUpperCase())) {
						position = move.cdr;
						break;
					}
				}
			} else
				break;
		}
		if (g.strictPrimitiveValue(position) != Value.UNDECIDED) {
			System.out.println(g.displayState(position));
			System.out.println("Game over");
		}
	}
}
