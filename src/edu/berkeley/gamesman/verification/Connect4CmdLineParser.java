package edu.berkeley.gamesman.verification;

import static org.kohsuke.args4j.ExampleMode.ALL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class Connect4CmdLineParser {
	@Option(name = "-d", usage = "database file")
	private String database;

	@Option(name = "-i", usage = "initial game state to verify from")
	private String initialGameState;

	@Option(name = "-o", usage = "log output to this file")
	private String outputFileName;

	@Option(name = "-s", usage = "number of states to verify")
	private int totalStates;

	@Option(name = "-t", usage = "time allowed to verify")
	private int totalTime;

	@Option(name = "-v", usage = "type of verifier")
	private String verifier;

	@Option(name = "-debug", usage = "print debug text")
	private static String debuggingS = "false";

	@Option(name = "-prob", usage = "verify probabilistically")
	private static String probabilisticS = "true";

	@Option(name = "-simple", usage = "simple output")
	private static String simpleOutputS = "true";

	static boolean debugging;
	static boolean probabilistic;
	static boolean simpleoutput;

	// receives other command line parameters than options
	@Argument
	private List<String> arguments = new ArrayList<String>();

	public Connect4CmdLineParser() {
		this.totalStates = -1;
		this.totalTime = -1;
	}

	public static void main(String args[]) {
		Connect4CmdLineParser cmdLineParser = new Connect4CmdLineParser();
		ArrayList<String> errors = new ArrayList<String>();

		if (!cmdLineParser.doMain(args))
			return;

		debugging = debuggingS.equalsIgnoreCase("true");
		probabilistic = !probabilisticS.equalsIgnoreCase("false");
		simpleoutput = simpleOutputS.equalsIgnoreCase("true");

		GameVerifier verifier;
		switch (GameVerifierType.fromString(cmdLineParser.verifier)) {
		case RANDOM:
			verifier = new RandomGameVerifier(Connect4GameState.class,
					cmdLineParser.database, cmdLineParser.outputFileName,
					cmdLineParser.totalStates, cmdLineParser.totalTime,
					cmdLineParser.initialGameState);
			break;
		case BACKTRACK:
			verifier = new BacktrackGameVerifier(Connect4GameState.class,
					cmdLineParser.database, cmdLineParser.outputFileName,
					cmdLineParser.totalStates, cmdLineParser.totalTime,
					cmdLineParser.initialGameState);
			break;
		default:
			throw new IllegalArgumentException("Invalid verifier name: "
					+ cmdLineParser.verifier);
		}

		while (verifier.hasNext()) {
			try {
				if (!simpleoutput) {
					verifier.printStatusBar();
				}
				verifier.next();
				if (!verifier.verifyGameState()) {
					verifier.writeIncorrectStateToFile();
					if (simpleoutput) {
						errors.add("Incorrect Value: "
								+ verifier.getCurrentValue()
								+ " Expected Value: "
								+ verifier.calculateValueOfCurrentState()
								+ " Current Game State: "
								+ verifier.getCurrentState());
					} else {
						System.out.println("Incorrect Value: "
								+ verifier.getCurrentValue()
								+ " Current Game State: "
								+ verifier.getCurrentState());
					}
				} else {
					/*
					 * System.out.println("Correct Value: " +
					 * verifier.getCurrentValue() + " Current Game State: " +
					 * verifier.getCurrentState());
					 */
				}
			} catch (IOException e) {
				System.err.println(e.getMessage());
				System.err.println("Cannot access database");
				System.exit(1);
			}
		}

		if (simpleoutput) {
			verifier.printIncorrectStateSimpleSummary();
			for (String str : errors) {
				System.out.println(str);
			}
		} else {
			verifier.printStatusBar();
			verifier.printIncorrectStateSummary();
			verifier.writeIncorrectStatesSummaryToFile();
			verifier.closeOutputFile();
		}
	}

	private boolean doMain(String[] args) {
		CmdLineParser parser = new CmdLineParser(this);

		try {
			// parse the arguments.
			parser.parseArgument(args);

			// you can parse additional arguments if you want.
			// parser.parseArgument("more","args");

			// after parsing arguments, you should check
			// if enough arguments are given.
			if (!arguments.isEmpty())
				throw new CmdLineException("argument is given");

		} catch (CmdLineException e) {
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

			return false;
		}

		// this will redirect the output to the specified output
		// System.out.println(out);
		return true;
	}

}
