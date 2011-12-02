package edu.berkeley.gamesman.database.analysis;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Keeps track of a number of {@code Measurements} samples, and allows for
 * simple statistics to be calculated.
 * 
 * @author adegtiar
 * @author rchengyue
 */
public class Measurements {
	private List<Long> sampleMeasurements;

	/**
	 * Creates an empty set of measurements.
	 */
	public Measurements() {
		sampleMeasurements = new LinkedList<Long>();
	}

	/**
	 * @return the max of the samples
	 */
	public long getMax() {
		return Collections.max(sampleMeasurements);
	}

	/**
	 * 
	 * @return the min of the samples
	 */
	public long getMin() {
		return Collections.min(sampleMeasurements);
	}

	/**
	 * @return the average of the samples
	 */
	public long getAverage() {
		long sum = 0;
		for (Long sampleMeasurement : sampleMeasurements)
			sum += sampleMeasurement;
		return sum / getNumSamples();
	}

	/**
	 * @return the total number of samples added
	 */
	public int getNumSamples() {
		return sampleMeasurements.size();
	}

	/**
	 * Adds the given measurement sample.
	 * 
	 * @param sample
	 *            the measurement to add
	 */
	public void add(long sample) {
		sampleMeasurements.add(sample);
	}
}
