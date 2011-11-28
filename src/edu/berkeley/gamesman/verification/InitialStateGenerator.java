package edu.berkeley.gamesman.verification;

import static org.kohsuke.args4j.ExampleMode.ALL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.database.Database;

/**
 * 
 * @author adegtiar
 */
public class InitialStateGenerator {
	@Option(name = "-d", usage = "database file")
	private String database;

	@Option(name = "-o", usage = "output list of states to this file")
	private String outputFileName;

	static List<BoardTile> initializedTiles = new ArrayList<BoardTile>();

	// receives other command line parameters than options
	@Argument
	private List<String> arguments = new ArrayList<String>();

	public static void main(String args[]) throws IOException {
		InitialStateGenerator parsedArgs = new InitialStateGenerator();
		if (!parsedArgs.doMain(args))
			return;

		System.out.println(parsedArgs.arguments);

		Database db;
		try {
			db = Database.openDatabase(parsedArgs.database);
		} catch (IOException e1) {
			throw new RuntimeException(e1);
		} catch (ClassNotFoundException e1) {
			throw new RuntimeException(e1);
		}
		StateGenerator generator = new StateGenerator(db.conf, initializedTiles);
		for (GameState state : generator) {
			System.out.println(state);
		}

		db.close();
	}

	private boolean doMain(String[] args) {
		CmdLineParser parser = new CmdLineParser(this);

		try {
			// parse the arguments.
			parser.parseArgument(args);

			for (String rowColPair : arguments) {
				if (!rowColPair.matches("[0-9]+,[0-9]+")) {
					throw new CmdLineException("invalid row,col pair: "
							+ rowColPair);
				}
				initializedTiles.add(BoardTile.fromString(rowColPair));
			}

			if (database == null) {
				throw new CmdLineException("no database specified");
			}
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

	/**
	 * 
	 * @author adegtiar
	 */
	static class StateGenerator implements Iterable<GameState> {
		private int width;
		private int height;
		private int pieces;
		private List<BoardTile> initializedTiles;
		private Configuration conf;

		StateGenerator(Configuration conf, List<BoardTile> initializedTiles) {
			this.conf = conf;
			width = conf.getInteger("gamesman.game.width", 7);
			height = conf.getInteger("gamesman.game.height", 6);
			pieces = conf.getInteger("gamesman.game.pieces", 4);
			this.initializedTiles = initializedTiles;
		}

		@Override
		public Iterator<GameState> iterator() {
			return new StateIterator();
		}

		/**
		 * Iterates over the initial states in the StateGenerator.
		 * 
		 * @author adegtiar
		 */
		private class StateIterator implements Iterator<GameState> {
			private Map<BoardTile, Connect4Piece> tileMap;
			private boolean mSeeked;
			private boolean mHasNext;
			private GameState mNext;

			@Override
			public boolean hasNext() {
				if (!mSeeked) {
					mSeeked = true;
					do {
						if (!nextPermutation()) {
							mHasNext = false;
							mNext = null;
							return false;
						}
					} while (!isValidState());
					mNext = makeState();
					mHasNext = true;
				}
				return mHasNext;
			}

			@Override
			public GameState next() {
				if (!mSeeked) {
					throw new IllegalStateException(
							"next() called without calling hasNext()");
				}
				if (mHasNext) {
					mSeeked = false;
				}
				return mNext;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

			private GameState makeState() {
				Connect4GameState state = new Connect4GameState(conf);
				for (Entry<BoardTile, Connect4Piece> initTilePair : tileMap
						.entrySet()) {
					BoardTile tile = initTilePair.getKey();
					state.setPiece(tile.row, tile.col, initTilePair.getValue());
				}
				return state;
			}

			/**
			 * Generates the next permutation of the tileMap.
			 * 
			 * @return true if a new permutation was generated and false if
			 *         there are no more permutations
			 */
			private boolean nextPermutation() {
				if (tileMap == null) {
					tileMap = new HashMap<BoardTile, Connect4Piece>();
					for (BoardTile tile : initializedTiles) {
						tileMap.put(tile, Connect4Piece.BLANK);
					}
					return true;
				} else {
					Connect4Piece lastPiece = Connect4Piece.lastPiece();
					for (Connect4Piece piece : tileMap.values()) {
						if (!lastPiece.equals(piece)) {
							incrementPermutation();
							return true;
						}
					}
					// Last position.
					return false;
				}
			}

			private void incrementPermutation() {
				for (BoardTile tile : initializedTiles) {
					if (!incrementPermutation(tile)) {
						break;
					}
				}
			}

			/**
			 * 
			 * @param tile
			 *            the tile to rotate
			 * @return true if the Permutation reset this tile back to blank
			 */
			private boolean incrementPermutation(BoardTile tile) {
				Connect4Piece oldPiece = tileMap.get(tile);
				int numPieces = Connect4Piece.values().length;
				Connect4Piece newPiece = Connect4Piece.values()[(oldPiece.ordinal() + 1)
				                								% numPieces];
				tileMap.put(tile, newPiece);
				return oldPiece.equals(Connect4Piece.lastPiece());
			}

			/**
			 * @return true if the current permutation is valid, otherwise false
			 */
			private boolean isValidState() {
				if (tileMap == null) {
					return false;
				}
				int numX = 0, numO = 0;
				for (Connect4Piece piece : tileMap.values()) {
					switch (piece) {
					case X:
						numX++;
						break;
					case O:
						numO++;
						break;
					}
				}
				return numX - numO == 0 || numX - numO == 1;
			}

		}
	}

	/**
	 * Represents a tile on a board.
	 * 
	 * @author adegtiar
	 */
	static private class BoardTile {
		private int row;
		private int col;

		BoardTile(int row, int col) {
			this.row = row;
			this.col = col;
		}

		static BoardTile fromString(String tileStringPair) {
			String[] rowColPair = tileStringPair.split(",");
			return new BoardTile(Integer.parseInt(rowColPair[0]),
					Integer.parseInt(rowColPair[1]));
		}

		@Override
		public String toString() {
			return String.format("(%d,%d)", row, col);
		}
		
		@Override
		public boolean equals(Object other) {
			if (! (other instanceof BoardTile)) {
				return false;
			}
			BoardTile otherTile = (BoardTile) other;
			return row == otherTile.row && col == otherTile.col; 
		}
		
		@Override
		public int hashCode() {
			return row + col * 13;
		}
	}
}