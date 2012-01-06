package edu.berkeley.gamesman.database.analysis;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class TierOutput {
	private boolean noHeader;
	private boolean outputMin;
	private boolean outputMax;
	private boolean outputAverage;
	private boolean outputTotal;
	private final NumberFormat formatter;

	
	private List<Double> minTierValues;
	private List<Double> maxTierValues;
	private List<Double> avgTierValues;

	public TierOutput(boolean noHeader, boolean outputMin, boolean outputMax,
			boolean outputAvg, boolean outputTotal) {
		this.noHeader = noHeader;
		this.outputMin = outputMin;
		this.outputMax = outputMax;
		this.outputAverage = outputAvg;
		this.outputTotal = outputTotal;
		this.formatter = new DecimalFormat("0.000");

		minTierValues = new ArrayList<Double>();
		maxTierValues = new ArrayList<Double>();
		avgTierValues = new ArrayList<Double>();
	}

	/**
	 * Outputs the minimum read times for each tier in measurements.
	 * 
	 * @param measurements
	 *            mapping of tiers to Measurements (wrapping around the minimum,
	 *            maximum, and average read time values)
	 */
	private void outputMin(Measurements measurements) {
		if (outputMin) {
			Double minTierValue = measurements.getMin();
			if (outputTotal) {
				minTierValues.add(minTierValue);
			}
			printOutput(minTierValue);
		}
	}

	/**
	 * Outputs the maximum read times for each tier in measurements.
	 * 
	 * @param measurements
	 *            mapping of tiers to Measurements (wrapping around the minimum,
	 *            maximum, and average read time values)
	 */
	private void outputMax(Measurements measurements) {
		if (outputMax) {
			Double maxTierValue = measurements.getMax();
			if (outputTotal) {
				maxTierValues.add(maxTierValue);
			}
			printOutput(maxTierValue);
		}
	}

	/**
	 * Outputs the average read times for each tier in measurements.
	 * 
	 * @param measurements
	 *            mapping of tiers to Measurements (wrapping around the minimum,
	 *            maximum, and average read time values)
	 */
	private void outputAverage(Measurements measurements) {
		if (outputAverage) {
			Double avgTierValue = measurements.getAverage();
			if (outputTotal) {
				avgTierValues.add(avgTierValue);
			}
			printOutput(avgTierValue);
		}
	}

	/**
	 * Outputs the aggregate read times for all the tiers.
	 */
	private void outputTotal() {
		if (!noHeader) {			
			System.out.print("Total\t");
		}
		if (outputMin) {
			printOutput(Collections.min(minTierValues));
		}
		if (outputMax) {
			printOutput(Collections.max(maxTierValues));
		}
		if (outputAverage) {			
			printOutput(getAverage(avgTierValues));
		}
	}

	private void printOutput(Double readTime) {
		System.out.print(formatter.format(readTime) + "\t");
	}
	
	/**
	 * Returns the average of the values in the collection
	 * 
	 * @param collection
	 *            a collection of longs
	 * @return the average of the values in the collection
	 */
	private double getAverage(Collection<Double> collection) {
		double sum = 0;
		for (Double i : collection) {
			sum += i;
		}
		return sum / collection.size();
	}

	private void outputHeading() {
		if (outputMin) {
			System.out.print("\tMin(ms)");
		}
		if (outputMax) {
			System.out.print("\tMax(ms)");
		}
		if (outputAverage) {
			System.out.print("\tAvg(ms)");
		}
	}
	
	/**
	 * Outputs the minimum, maximum, average, and aggregate read times for the
	 * specified tier to Measurements mapping. Note: Some of these categories
	 * will not display if the flag is set to false.
	 * 
	 * @param tierMeasurements
	 *            mapping of tiers to Measurements (wrapping around the minimum,
	 *            maximum, and average read time of the tier)
	 */
	public void outputMeasurements(Map<Integer, Measurements> tierMeasurements) {
		if (!noHeader) {
			outputHeading();
			System.out.println();
		}
		for (Entry<Integer, Measurements> measurementEntry : tierMeasurements.entrySet()) {
			if (!noHeader) {
				System.out.print("Tier " + measurementEntry.getKey() + "\t");
			}
			Measurements measurements = measurementEntry.getValue();
			if (outputMin) {
				outputMin(measurements);
			}
			if (outputMax) {
				outputMax(measurements);
			}
			if (outputAverage) {
				outputAverage(measurements);
			}
			System.out.println();
		}
		if (outputTotal) {
			outputTotal();
		}
	}

}
