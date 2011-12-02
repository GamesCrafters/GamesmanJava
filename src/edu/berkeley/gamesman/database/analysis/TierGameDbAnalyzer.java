package edu.berkeley.gamesman.database.analysis;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.game.TierGame;

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
	 */
	public void analyzeDb() throws IOException {
		while (!isDoneAnalyzing()) {
			benchmarkGame(db.conf.getGame());
		}
	}

	/**
	 * 
	 * @return
	 */
	public Map<Integer, Measurements> getTierMeasurements() {
		return tierMeasurements;
	}

	/**
	 * 
	 * @param game
	 * @throws IOException
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

	private <S extends State> S randomChild(Game<S> game, S state) {
		return null;
	}

	/**
	 * 
	 * @param tier
	 * @param game
	 * @param position
	 * @throws IOException
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
	 * 
	 * @param game
	 * @param position
	 * @return
	 * @throws IOException
	 */
	<S extends State> long measureRead(Game<S> game, S position)
			throws IOException {
		long startTime = System.currentTimeMillis();
		long hash = game.stateToHash(position);
		db.readRecord(dh, hash);
		long endTime = System.currentTimeMillis();
		return endTime - startTime;
	}

	boolean isDoneAnalyzing() {
		for (Measurements measurements : tierMeasurements.values()) {
			if (measurements.getNumSamples() < NUM_SAMPLES) {
				return false;
			}
		}
		return true;
	}
}
