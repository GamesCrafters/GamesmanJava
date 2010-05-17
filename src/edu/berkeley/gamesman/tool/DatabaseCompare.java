package edu.berkeley.gamesman.tool;

import edu.berkeley.gamesman.core.*;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.util.Util;

/**
 * @author Steven Schlansker
 * 
 * @param <S>
 *            The state type of the comparison
 */
public class DatabaseCompare<S extends State> {

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

		Database db1 = c1.openDatabase(null,false);
		Database db2 = c2.openDatabase(null,false);

		System.out
				.println("Comparing the databases by walking through the hash space of d1...");

		c1 = db1.getConfiguration();
		c2 = db2.getConfiguration();

		boolean bothContainValue = c1.valueStates > 0 && c2.valueStates > 0, bothContainRemoteness = c1.remotenessStates > 0
				&& c2.remotenessStates > 0, bothContainScore = c1.scoreStates > 0
				&& c2.scoreStates > 0;

		Game<S> g1 = Util.checkedCast(c1.getGame());
		Game<S> g2 = Util.checkedCast(c2.getGame());

		for (long hash1 = 0; hash1 < db1.getConfiguration().getGame()
				.numHashes(); hash1++) {
			long hash2 = g2.stateToHash(g1.hashToState(hash1));

			Record r1 = db1.getRecord(hash1);
			Record r2 = db2.getRecord(hash2);

			if (bothContainValue && r1.value != r2.value)
				Util.fatalError("Database does not match at position\n"
						+ g1.displayState(g1.hashToState(hash1)) + "\n" + r1
						+ " != " + r2);
			if (bothContainRemoteness && r1.remoteness != r2.remoteness)
				Util.fatalError("Database does not match at position\n"
						+ g1.displayState(g1.hashToState(hash1)) + "\n" + r1
						+ " != " + r2);
			if (bothContainValue && r1.score != r2.score)
				Util.fatalError("Database does not match at position\n"
						+ g1.displayState(g1.hashToState(hash1)) + "\n" + r1
						+ " != " + r2);
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
			new DatabaseCompare<State>().compare(args);
			System.out.println("Compare successful!");
		} catch (ClassNotFoundException e) {
			Util.fatalError("Unable to load database class!", e);
		}
	}

}
