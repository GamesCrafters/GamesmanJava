package edu.berkeley.gamesman.verification;

import java.io.IOException;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.game.TierGame;
import edu.berkeley.gamesman.game.util.TierState;

/**
 * 
 * @author adegtiar
 * @author rchengyue
 */
public class TemporaryTester {

	public static void main(String[] args) {
		// check db values: independent checker primitive -
		// matches db.staterecord == 0
		// check tree consistency -
		// (lose has all wins, tie has no lose, w has a lose)
		Database db;
		try {
			db = Database.openDatabase("database54.db");
		} catch (IOException e1) {
			throw new RuntimeException(e1);
		} catch (ClassNotFoundException e1) {
			throw new RuntimeException(e1);
		}
		DatabaseHandle dbHandle = db.getHandle(true);
		Configuration conf = db.conf;

		TierGame mGame = (TierGame) conf.getGame();

		// we'll be using string representation instead
		// TierState startingPosition =
		// mGame.startingPositions().iterator().next();
		String somePositionString = "XOXO  X             ";

		TierState somePositionState = mGame.stringToState(somePositionString);
		// index into the db
		long hashedState = mGame.stateToHash(somePositionState);
		long recordValue;
		try {
			recordValue = db.readRecord(dbHandle, hashedState);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} // long representation of a record (remoteness, value)

		Record stateRecord = mGame.newRecord();

		// interprets as a record.
		mGame.longToRecord(somePositionState, recordValue, stateRecord);

		System.out.println(stateRecord);
	}

}
