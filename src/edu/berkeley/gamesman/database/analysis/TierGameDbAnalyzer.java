package edu.berkeley.gamesman.database.analysis;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Random;

import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.game.TierGame;
import edu.berkeley.gamesman.util.Pair;

/**
 * Performs a benchmark analysis of TierGame databases.
 * 
 * @author adegtiar
 * @author rchengyue
 */
public class TierGameDbAnalyzer {

	private final int NUM_SAMPLES;
	private Map<Integer, Measurements> tierMeasurements;
	private Database db;
	private DatabaseHandle dh;

	/**
	 * Constructs a new analyzer.
	 * 
	 * @param db
	 *            the database to benchmark
	 * @param numSamples
	 *            the number of samples per tier to take
	 * @param tiers
	 *            the set of tiers to sample from. If empty, samples all tiers
	 * @throws IllegalArgumentException
	 *             if the database is not for a TierGame
	 * @throws IllegalArgumentException
	 *             if one of the tiers is unreachable
	 */
	public TierGameDbAnalyzer(Database db, int numSamples, Integer... tiers) {
		this.NUM_SAMPLES = numSamples;
		this.db = db;
		this.dh = db.getHandle(true);
		// Make sure the database is a TierGame database.
		TierGame game;
		try {
			game = (TierGame) db.conf.getGame();
		} catch (ClassCastException e) {
			throw new IllegalArgumentException(
					"only TierGame databases are supported");
		}
		// Make sure all tiers actually exist in the database.
		int largestRequestedTier = Collections.max(Arrays.asList(tiers));
		int numTiers = game.numberOfTiers();
		if (largestRequestedTier >= numTiers) {
			throw new IllegalArgumentException(String.format(
					"Cannot reach tier %d. Only %d tiers in the database",
					largestRequestedTier, numTiers));
		}
		// Populate the measurements table with the tiers to measure.
		if (tiers.length > 0) {
			for (Integer tier : tiers) {
				tierMeasurements.put(tier, new Measurements());
			}
		} else {
			for (int tier = 0; tier < numTiers; tier++) {
				tierMeasurements.put(tier, new Measurements());
			}
		}
	}

	/**
	 * Analyze the database. After analysis, results can be retrieved using
	 * {@link #getTierMeasurements}.
	 * 
	 * @throws IOException
	 *             on an error reading from the database
	 */
	public void analyzeDb() throws IOException {
		while (!isDoneAnalyzing()) {
			benchmarkGame(db.conf.getGame());
		}
	}

	/**
	 * @return the results of the benchmark analysis.
	 */
	public Map<Integer, Measurements> getTierMeasurements() {
		return tierMeasurements;
	}

	/**
	 * Benchmarks one play-through of a game.
	 * 
	 * @param game
	 *            the {@link Game} to play through
	 * @throws IOException
	 *             on an error reading from the database
	 */
	private <S extends State> void benchmarkGame(Game<S> game)
			throws IOException {
		int currentTier = 0;
		S currentPosition = game.startingPositions().iterator().next();
		while (game.strictPrimitiveValue(currentPosition) != Value.UNDECIDED) {
			sampleTier(currentTier++, game, currentPosition);
			currentPosition = randomChild(game, currentPosition);
		}
	}

	/**
	 * Generates a random child state.
	 * 
	 * @param game
	 *            the game corresponding to the given state
	 * @param state
	 *            the position to pick a child from
	 * @return a randomly pick child of the given state
	 */
	private <S extends State> S randomChild(Game<S> game, S state) {
		Collection<Pair<String, S>> children = game.validMoves(state);
		int elementIndex = new Random().nextInt(children.size());
		for (Pair<String, S> child : children) {
			if (elementIndex == 0) {
				return child.cdr;
			}
		}
		return null;
	}

	/**
	 * Reads a sample measurement from the the given position.
	 * 
	 * @param tier
	 *            the tier
	 * @param game
	 *            the game corresponding to the position
	 * @param position
	 *            the current position
	 * @throws IOException
	 *             on an error reading from the database
	 */
	<S extends State> void sampleTier(int tier, Game<S> game, S position)
			throws IOException {
		Measurements measurements = tierMeasurements.get(tier);
		if (measurements != null && measurements.getNumSamples() < NUM_SAMPLES) {
			long measurementSample = measureRead(game, position);
			measurements.add(measurementSample);
		}
	}

	/**
	 * Reads the given position from the database and measures the time it took.
	 * 
	 * @param game
	 *            the game corresponding to the position
	 * @param position
	 *            the position to read from the database
	 * @return the time it took to read the record, in millis
	 * @throws IOException
	 *             on an error reading from the database
	 */
	<S extends State> long measureRead(Game<S> game, S position)
			throws IOException {
		long startTime = System.currentTimeMillis();
		long hash = game.stateToHash(position);
		db.readRecord(dh, hash);
		long endTime = System.currentTimeMillis();
		return endTime - startTime;
	}

	/**
	 * @return true if the analyzer has sufficient samples, otherwise false
	 */
	boolean isDoneAnalyzing() {
		for (Measurements measurements : tierMeasurements.values()) {
			if (measurements.getNumSamples() < NUM_SAMPLES) {
				return false;
			}
		}
		return true;
	}
}
