package edu.berkeley.gamesman.verification;

import static org.kohsuke.args4j.ExampleMode.ALL;

import java.io.File;
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

	@Option(name = "-o", usage = "log output to this file")
	private String outputFileName;

	@Option(name = "-v", usage = "type of verifier")
	private String verifier;

	@Option(name = "-s", usage = "number of states to verify")
	private int totalStates;

	@Option(name = "-t", usage = "time allowed to verify")
	private int totalTime;
	
	// receives other command line parameters than options
	@Argument
	
	private List<String> arguments = new ArrayList<String>();

	public static void main(String args[]) {
		Connect4CmdLineParser cmdLineParser = new Connect4CmdLineParser();
		if (!cmdLineParser.doMain(args))
			return;
		GameVerifier verifier;
		switch (GameVerifierType.fromString(cmdLineParser.verifier)) {
		case RANDOM:
			verifier = new RandomGameVerifier(Connect4GameState.class,
					cmdLineParser.database, cmdLineParser.outputFileName,
					cmdLineParser.totalStates, cmdLineParser.totalTime);
			break;
		case BACKTRACK:
			verifier = new BacktrackGameVerifier(Connect4GameState.class,
					cmdLineParser.database, cmdLineParser.outputFileName,
					cmdLineParser.totalStates, cmdLineParser.totalTime);
			break;
		default:
			throw new IllegalArgumentException("Invalid verifier name: "
					+ cmdLineParser.verifier);
		}

		while (verifier.hasNext()) {
			try {
				verifier.printStatusBar();
				verifier.next();
				if (!verifier.verifyGameState()) {
					verifier.writeIncorrectStateToFile();
					/*System.out.println("Incorrect Value: "
							+ verifier.getCurrentValue()
							+ " Current Game State: "
							+ verifier.getCurrentState());*/
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

		verifier.printStatusBar();
		verifier.printIncorrectStateSummary();
		verifier.writeIncorrectStatesSummaryToFile();
		verifier.closeOutputFile();
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
