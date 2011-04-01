package edu.berkeley.gamesman.verification;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.game.TierGame;
import edu.berkeley.gamesman.game.util.TierState;

/**
 * 
 * Verifies the game tree based on a given database and outputs the erroneous
 * values to a log file.
 * 
 * @author adegtiar
 * @author rchengyue
 */
public abstract class GameVerifier implements Iterator<GameState> {

	protected Class<? extends GameState> stateClass;
	private RandomAccessFile outFile;
	private long incorrectStatesCount;
	protected GameState currentGameState;
	protected Database db;
	protected Configuration conf;
	protected TierGame mGame;
	protected DatabaseHandle dbHandle;
	protected ProgressBar progressBar;

	/**
	 * The number of <tt>GameState</tt> to verify.
	 */
	protected final int totalStateCount;
	protected int stateCount;

	protected GameVerifier(Class<? extends GameState> stateClass,
			String database, String outputFileName, int stateTotalCount) {
		this.stateClass = stateClass;
		try {
			db = Database.openDatabase(database);
		} catch (IOException e1) {
			throw new RuntimeException(e1);
		} catch (ClassNotFoundException e1) {
			throw new RuntimeException(e1);
		}
		dbHandle = db.getHandle(true);
		conf = db.conf;

		try {
			this.outFile = new RandomAccessFile(new File(outputFileName), "rw");
		} catch (FileNotFoundException e) {
			System.err.println(e.getMessage());
			System.err.println("Cannot find file: " + outFile.toString());
			System.exit(1);
		}

//		this.incorrectStates = new HashSet<GameState>();
		this.mGame = (TierGame) conf.getGame();
		this.totalStateCount = stateTotalCount;
		this.progressBar = new ProgressBar(stateTotalCount);

		// Write header to outFile to save room for update
		writeIncorrectStatesSummaryToFile();
	}

	public GameState getInitialGameState() {
		try {
			return stateClass.getConstructor(Configuration.class).newInstance(
					conf);
		} catch (IllegalArgumentException e) {
			throw new Error(e);
		} catch (SecurityException e) {
			throw new Error(e);
		} catch (InstantiationException e) {
			throw new Error(e);
		} catch (IllegalAccessException e) {
			throw new Error(e);
		} catch (InvocationTargetException e) {
			throw new Error(e.getCause());
		} catch (NoSuchMethodException e) {
			throw new Error(e);
		}
	}

	/**
	 * Checks the value for a <tt>GameState</tt> and ensures that it is correct.
	 * 
	 * @return whether or not the state is correct (i.e. win has a lose, lose
	 *         has all wins, tie has no lose).
	 * @throws IOException
	 *             if cannot query the state from the database.
	 */
	protected boolean verifyGameState() throws IOException {

		Value dbValue = getValueOfState(currentGameState.getBoardString());
		Value calculatedValue;
		if (currentGameState.isPrimitive())
			calculatedValue = currentGameState.getValue();
		else
			calculatedValue = calculateValueOfCurrentState();

		if (dbValue != calculatedValue) {
//			incorrectStates.add(currentGameState);
			incorrectStatesCount++;
		}

		return dbValue == calculatedValue;
	}

	/**
	 * Calculates the value of a non-primitive state. Does this by checking the
	 * values of the children in the db.
	 * 
	 * @return the value of the current state.
	 */
	private Value calculateValueOfCurrentState() {
		assert (!currentGameState.isPrimitive());

		Iterator<String> childrenStringIterator = currentGameState
				.generateChildren();

		Value bestValueSoFar = Value.LOSE;

		while (childrenStringIterator.hasNext()) {
			String childString = childrenStringIterator.next();

			Value childValue = getValueOfState(childString);

			switch (childValue) {
			case TIE:
				/*
				 * System.out.println("TIE CHILD!");
				 * System.out.println(childString);
				 */
				bestValueSoFar = Value.TIE;
				break;
			case LOSE:
				/*
				 * System.out.println("LOSE CHILD!");
				 * System.out.println(childString);
				 */
				return Value.WIN;
				/*
				 * case WIN: System.out.println("WIN CHILD!");
				 * System.out.println(childString);
				 */
			}
		}

		return bestValueSoFar;
	}

	/**
	 * Gets the value of a state String.
	 * 
	 * @param stateString
	 *            the String representation of a state.
	 * @return the value of the state represented by the String.
	 */
	public Value getValueOfState(String stateString) {
		TierState tierState = mGame.stringToState(stateString);

		// index into the db
		long hashedState = mGame.stateToHash(tierState);
		long recordValue = 0;
		try {
			recordValue = db.readRecord(dbHandle, hashedState);
		} catch (IOException e) {
			System.err.println("Cannot read from db: " + stateString);
			System.exit(1);
		}

		// long representation of a record (remoteness, value)
		Record stateRecord = mGame.newRecord();

		// interprets the long as a record
		mGame.longToRecord(tierState, recordValue, stateRecord);

		return stateRecord.value;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException(
				"GameVerifier does not support remove");
	}

	public GameState getCurrentState() {
		return currentGameState;
	}

	public Value getCurrentValue() {
		return getValueOfState(currentGameState.getBoardString());
	}

	/*
	 * PRINTING AND WRITING METHODS
	 */

	public void printStatusBar() {
		if (stateCount % 10000 == 0 || hasNext())
			System.out.println("Verified " + stateCount + " states");
	}

	/**
	 * Prints the incorrect states summary (width, height, and number of
	 * incorrect states) to standard out.
	 */
	public void printIncorrectStateSummary() {
		System.out.println("Width: "
				+ conf.getInteger("gamesman.game.width", 7) + " Height: "
				+ conf.getInteger("gamesman.game.height", 6));
		System.out.println("Incorrect States: " + incorrectStatesCount);
	}

	/**
	 * Writes the value of the current incorrect GameState and the String
	 * representation of the current incorrect GameState to the output file.
	 */
	public void writeIncorrectStateToFile() {
		// Write current GameState to outFile
		try {
			outFile.write(("Incorrect Value: "
					+ getValueOfState(currentGameState.getBoardString())
							.toString() + " Current Game State: "
					+ currentGameState.toString() + '\n').getBytes());
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.err.println("Cannot write to file: " + outFile.toString());
			System.exit(1);
		}
	}

	/**
	 * Write the incorrect states summary (width, height, and number of
	 * incorrect states) to the output file.
	 */
	public void writeIncorrectStatesSummaryToFile() {
		try {
			outFile.seek(0);
			outFile.write(("Width: "
					+ +conf.getInteger("gamesman.game.width", 7) + " Height: " + conf
					.getInteger("gamesman.game.height", 6)).getBytes());
			outFile.write(("Incorrect States: " + incorrectStatesCount + '\n')
					.getBytes());
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.err.println("Cannot write to file: " + outFile.toString());
			System.exit(1);
		}
	}

	/**
	 * Closes the output file.
	 */
	public void closeOutputFile() {
		try {
			outFile.close();
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.err.println("Cannot close file: " + outFile.toString());
			System.exit(1);
		}
	}

}
