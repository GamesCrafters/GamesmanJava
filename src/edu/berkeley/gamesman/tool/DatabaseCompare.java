package edu.berkeley.gamesman.tool;

import java.util.EnumSet;
import java.util.Set;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Game;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.RecordFields;
import edu.berkeley.gamesman.util.Util;

/**
 * @author Steven Schlansker
 * 
 * @param <S>
 *            The state type of the comparison
 */
public class DatabaseCompare<S> {

	/**
	 * Compare two arguments
	 * 
	 * @param args
	 *            the arguments
	 * @throws ClassNotFoundException
	 *             A class could not be loaded
	 */
	public void compare(String args[]) throws ClassNotFoundException {
		Configuration c1 = new Configuration(args[0]);
		Configuration c2 = new Configuration(args[1]);

		Database db1 = c1.openDatabase();
		Database db2 = c2.openDatabase();

		System.out
				.println("Comparing the databases by walking through the hash space of d1...");

		c1 = db1.getConfiguration();
		c2 = db2.getConfiguration();

		Game<S> g1 = Util.checkedCast(c1.getGame());
		Game<S> g2 = Util.checkedCast(c2.getGame());

		for (long hash1 = 0; hash1 < db1.getConfiguration().getGame()
				.lastHash(); hash1++) {
			long hash2 = g2.stateToHash(g1.hashToState(hash1));

			Set<RecordFields> toCheck = EnumSet
					.copyOf(c1.storedFields.keySet());
			toCheck.retainAll(c2.storedFields.keySet());

			Record r1 = db1.getRecord(hash1);
			Record r2 = db2.getRecord(hash2);

			for (RecordFields rf : toCheck) {
				if (r1.get(rf) != r2.get(rf)) {
					Util.fatalError("Database does not match at position\n"
							+ g1.displayState(g1.hashToState(hash1)) + "\n"
							+ rf + " " + r1.get(rf) + " != " + r2.get(rf));
				}
			}
		}
	}

	/**
	 * Call the database comparator
	 * 
	 * @param args
	 *            the arguments to use
	 */
	public static void main(String args[]) {
		try {
			new DatabaseCompare<Object>().compare(args);
			System.out.println("Compare successful!");
		} catch (ClassNotFoundException e) {
			Util.fatalError("Unable to load database class!", e);
		}
	}

}
