package edu.berkeley.gamesman.database.analysis;

import static org.kohsuke.args4j.ExampleMode.ALL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.util.Util;

public class DatabaseAnalyzerCommandLineParser {
	private CmdLineParser parser;

	@Option(name = "-n", usage = "number of samples to take for each tier")
	private static int numReads;

	@Option(name = "-t", usage = "output per-tier info")
	private static String tiersS;

	@Option(name = "--noheader", usage = "no headers for fields")
	private static String noHeaderS = "false";

	@Option(name = "--outputmin", usage = "output minimum read time of values for each tier")
	private static String outputMinS = "false";

	@Option(name = "--outputmax", usage = "output maximum read time of values for each tier")
	private static String outputMaxS = "false";

	@Option(name = "--outputaverage", usage = "output average read time of values for each tier")
	private static String outputAverageS = "false";

	@Option(name = "--outputtotal", usage = "output the aggregate read time for all tiers")
	private static String outputTotalS = "false";

	static Database database;
	static Integer[] tiers;
	static boolean noHeader;
	static boolean outputMin;
	static boolean outputMax;
	static boolean outputAverage;
	static boolean outputTotal;

	// receives other command line parameters than options
	@Argument(metaVar = "database", usage = "the TierGame database")
	private List<String> arguments = new ArrayList<String>();

	public DatabaseAnalyzerCommandLineParser() {
		numReads = 100;
		tiers = new Integer[0];
	}

	/**
	 * Parses the arguments. Note: If no arguments are passed into outputMin,
	 * outputMax, and outputAverage, all of these arguments are set to true. If
	 * no arguments are passed into tiers and outputTotal, tiers will be set to
	 * an empty array (interpreted as analyzing all tiers) and outputTotal will
	 * be set to true.
	 * 
	 * @param args
	 *            Strings passed into the program as arguments
	 * @return true if the program successfully parses the arguments; false
	 *         otherwise (and prints an exception)
	 */
	private boolean doMain(String[] args) {
		parser = new CmdLineParser(this);

		try {
			// parse the arguments.
			parser.parseArgument(args);

			// set to output min, max, and average of each tier as default
			if (outputMinS == "false" && outputMaxS == "false"
					&& outputAverageS == "false") {
				outputMinS = "true";
				outputMaxS = "true";
				outputAverageS = "true";
			}

			// set to analyze all tiers as default
			if (tiersS == null && outputTotalS == "false") {
				tiersS = null;
				outputTotalS = "true";
			}

			// after parsing arguments, you should check
			// if enough arguments are given.
			if (arguments.isEmpty())
				throw new CmdLineException(parser, "no database specified");

		} catch (CmdLineException e) {
			printError(e);
			return false;
		}

		// this will redirect the output to the specified output
		// System.out.println(out);
		return true;
	}

	private void printError(Exception e) {
		// if there's a problem in the command line,
		// you'll get this exception. this will report
		// an error message.
		System.err.println(e.getMessage());
		System.err.println("java SampleMain [options...] arguments...");
		// print the list of available options
		parser.printUsage(System.err);
		System.err.println();

		// print option sample. This is useful some time
		System.err.println("  Example: java SampleMain"
				+ parser.printExample(ALL));
	}

	/**
	 * Sets all the values of the required fields (from the options specified).
	 * 
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public void setValues() throws IOException, ClassNotFoundException {
		noHeader = noHeaderS.equalsIgnoreCase("true");

		outputMin = outputMinS.equalsIgnoreCase("true");
		outputMax = outputMaxS.equalsIgnoreCase("true");
		outputAverage = outputAverageS.equalsIgnoreCase("true");
		outputTotal = outputTotalS.equalsIgnoreCase("true");

		if (tiersS != null) {
			tiers = Util.parseIntegers(tiersS.split(","));
		}

		database = Database.openDatabase(arguments.get(0));
	}

	// /**
	// * Prints out debugging statements for each option value.
	// */
	// private void debug() {
	// System.out.print("Tiers: [");
	// for (Integer tier : tiers) {
	// System.out.print(tier + " ");
	// }
	// System.out.println("]");
	// System.out.println("Database: " + database);
	// System.out.println("Num reads: " + numReads);
	// System.out.println("No header: " + noHeader);
	// System.out.println("Output min: " + outputMin);
	// System.out.println("Output max: " + outputMax);
	// System.out.println("Output average: " + outputAverage);
	// System.out.println("Output total: " + outputTotal);
	// }

	/**
	 * Analyzes the specified TierCutDatabase and outputs the minimum, maximum,
	 * average, and aggregate read time values for the tiers.
	 * 
	 * @param args
	 *            -d <String> the TierCutDatabase on which the program will do
	 *            analysis on
	 *            <p>
	 *            -n <Integer> the number of samples for read times in each tier
	 *            <p>
	 *            -t <String> the specific tiers of the TierCutDatabase to run
	 *            this analysis on
	 *            <p>
	 *            --noheader <String> true if no headers before the output
	 *            <p>
	 *            --outputmin <String> true if want to output the minimum read
	 *            times for the TierCutDatabase
	 *            <p>
	 *            --outputmax <String> true if want to output the maximum read
	 *            times for the TierCutDatabase
	 *            <p>
	 *            --outputaverage <String> true if want to output the average
	 *            read times for the TierCutDatabase
	 *            <p>
	 *            --outputtotal <String> true if want to output the aggregate
	 *            read times for the TierCutDatabase
	 */
	public static void main(String args[]) {
		DatabaseAnalyzerCommandLineParser cmdLineParser = new DatabaseAnalyzerCommandLineParser();

		if (!cmdLineParser.doMain(args)) {
			System.exit(1);
		}

		try {
			cmdLineParser.setValues();
		} catch (Exception e) {
			cmdLineParser.printError(e);
			System.exit(2);
		}

		TierGameDbAnalyzer databaseAnalyzer = new TierGameDbAnalyzer(database,
				numReads, tiers);
		try {
			databaseAnalyzer.analyzeDb();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}

		TierOutput tierOutput = new TierOutput(noHeader, outputMin, outputMax,
				outputAverage, outputTotal);
		tierOutput.outputMeasurements(databaseAnalyzer.getTierMeasurements());
	}
}
