package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.hasher.PermutationHash;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * Cuboids...
 * 
 * @author Jeremy Fleischman
 */
public class Rubik extends TwistyPuzzle<CubeState> {
	private final int WIDTH, HEIGHT, DEPTH;

	private final int[] VALID_DIRS;

	private final String[] VALID_FACES;

	public Rubik(Configuration conf) {
		super(conf);
		WIDTH = conf.getInteger("gamesman.game.width", 2);
		HEIGHT = conf.getInteger("gamesman.game.height", 2);
		DEPTH = conf.getInteger("gamesman.game.depth", 2);
		VALID_FACES = conf.getProperty("gamesman.game.faces", "FUR").split(
				", *");
		VALID_DIRS = conf.getInts("gamesman.game.dirs", new int[] { 1, 3 });
		// TODO - generalize to arbitrary cuboids!
		// TODO should we treat a 2x2x3 as different from a 3x2x2
		// we could require that WIDTH <= HEIGHT <= DEPTH,
		// or cycle them to make that true
	}

	@Override
	public String describe() {
		return String.format(
				"%dx%dx%d cuboid (legal faces: %s, legal dirs: %s)", WIDTH,
				HEIGHT, DEPTH, Arrays.toString(VALID_FACES), Arrays
						.toString(VALID_DIRS));
	}

	@Override
	public String displayState(CubeState pos) {
		return pos.display(false);
	}

	@Override
	public String displayHTML(CubeState pos) {
		return pos.display(true);
	}

	private static final int cornerCount = 8;

	private static final long[] THREE_TO_X = new long[cornerCount];

	// not thread safe for this to be static
	private final PermutationHash cpHasher = new PermutationHash(
			cornerCount - 1, false);
	static { // memoize some useful values for (un)hashing
		THREE_TO_X[0] = 1;
		for (int i = 1; i < cornerCount; i++)
			THREE_TO_X[i] = 3 * THREE_TO_X[i - 1];
	}

	@Override
	public long stateToHash(CubeState state) {
		int[] cp = new int[state.pieces.length - 1];
		for (int i = 0; i < cp.length; i++)
			cp[i] = state.pieces[i];
		long hash = cpHasher.hash(cp);
		hash *= THREE_TO_X[6];
		for (int i = 0; i < 6; i++)
			hash += state.orientations[i] * THREE_TO_X[i];
		return hash;
	}

	@Override
	public long numHashes() {
		return THREE_TO_X[6] * cpHasher.numHashes();
	}

	@Override
	public void hashToState(long hash, CubeState cs) {
		// corner orientations
		int[] co = cs.orientations;
		int totalorient = 0;
		for (int i = 0; i < 6; i++) {
			co[i] = (int) (hash % 3);
			hash /= 3;
			totalorient += co[i];
		}
		co[6] = Util.nonNegativeModulo((3 - totalorient), 3);
		co[7] = 0;

		// corner permutations
		int[] cp = cs.pieces;
		cpHasher.unhash(hash, cp);
		cp[cornerCount - 1] = 7;
	}

	@Override
	public Value primitiveValue(CubeState pos) {
		return pos.isSolved() ? Value.WIN : Value.UNDECIDED;
	}

	@Override
	public Collection<CubeState> startingPositions() {
		return Arrays.asList(new CubeState());
	}

	@Override
	public String stateToString(CubeState pos) {
		return Util.join(",", pos.pieces) + ";"
				+ Util.join(",", pos.orientations);
	}

	@Override
	public CubeState stringToState(String pos) {
		String[] pieces_orientations = pos.split(";");
		return new CubeState(Util.parseInts(pieces_orientations[0].split(",")),
				Util.parseInts(pieces_orientations[1].split(",")));
	}

	private void cycle_pieces(int p1, int p2, int[] pieces) {
		int temp = pieces[p1];
		pieces[p1] = pieces[p2 - 4];
		pieces[p2 - 4] = pieces[p2];
		pieces[p2] = pieces[p1 + 4];
		pieces[p1 + 4] = temp;
	}

	private static final String dirToString = "  2'";

	@Override
	public Collection<Pair<String, CubeState>> validMoves(CubeState pos) {
		ArrayList<Pair<String, CubeState>> next = new ArrayList<Pair<String, CubeState>>();
		for (int times : VALID_DIRS) {
			for (String face : VALID_FACES) {
				int[] pieces = pos.pieces.clone();
				int[] orientations = pos.orientations.clone();
				for (int i = 0; i < times; i++) {
					if (face.equals("F")) {
						cycle_pieces(0, 5, pieces);
						cycle_pieces(0, 5, orientations);
						orientations[0] = (orientations[0] + 2) % 3;
						orientations[4] = (orientations[4] + 1) % 3;
						orientations[5] = (orientations[5] + 2) % 3;
						orientations[1] = (orientations[1] + 1) % 3;
					} else if (face.equals("U")) {
						int temp = pieces[0];
						pieces[0] = pieces[2];
						pieces[2] = pieces[3];
						pieces[3] = pieces[1];
						pieces[1] = temp;

						temp = orientations[0];
						orientations[0] = orientations[2];
						orientations[2] = orientations[3];
						orientations[3] = orientations[1];
						orientations[1] = temp;
					} else if (face.equals("R")) {
						cycle_pieces(2, 4, pieces);
						cycle_pieces(2, 4, orientations);
						orientations[2] = (orientations[2] + 2) % 3;
						orientations[6] = (orientations[6] + 1) % 3;
						orientations[4] = (orientations[4] + 2) % 3;
						orientations[0] = (orientations[0] + 1) % 3;
					}
				}
				next.add(new Pair<String, CubeState>(face
						+ ("" + dirToString.charAt(times)).trim(),
						new CubeState(pieces, orientations)));
			}
		}
		return next;
	}

	@Override
	public int maxChildren() {
		return VALID_DIRS.length * VALID_FACES.length;
	}

	@Override
	public CubeState newState() {
		return new CubeState();
	}

	@Override
	public int validMoves(CubeState pos, CubeState[] children) {
		int c = 0;
		for (int times : VALID_DIRS) {
			for (String face : VALID_FACES) {
				for (int i = 0; i < pos.pieces.length; i++)
					children[c].pieces[i] = pos.pieces[i];
				for (int i = 0; i < pos.orientations.length; i++)
					children[c].orientations[i] = pos.orientations[i];
				int[] pieces = children[c].pieces;
				int[] orientations = children[c].orientations;
				for (int i = 0; i < times; i++) {
					if (face.equals("F")) {
						cycle_pieces(0, 5, pieces);
						cycle_pieces(0, 5, orientations);
						orientations[0] = (orientations[0] + 2) % 3;
						orientations[4] = (orientations[4] + 1) % 3;
						orientations[5] = (orientations[5] + 2) % 3;
						orientations[1] = (orientations[1] + 1) % 3;
					} else if (face.equals("U")) {
						int temp = pieces[0];
						pieces[0] = pieces[2];
						pieces[2] = pieces[3];
						pieces[3] = pieces[1];
						pieces[1] = temp;

						temp = orientations[0];
						orientations[0] = orientations[2];
						orientations[2] = orientations[3];
						orientations[3] = orientations[1];
						orientations[1] = temp;
					} else if (face.equals("R")) {
						cycle_pieces(2, 4, pieces);
						cycle_pieces(2, 4, orientations);
						orientations[2] = (orientations[2] + 2) % 3;
						orientations[6] = (orientations[6] + 1) % 3;
						orientations[4] = (orientations[4] + 2) % 3;
						orientations[0] = (orientations[0] + 1) % 3;
					}
				}
				c++;
			}
		}
		return c;
	}
}

class CubeState implements State {
	private static final int[] SOLVED_PIECES = new int[] { 0, 1, 2, 3, 4, 5, 6,
			7 };

	private static final int[] SOLVED_ORIENTATIONS = new int[] { 0, 0, 0, 0, 0,
			0, 0, 0 };

	int[] pieces, orientations;

	public CubeState(int[] pieces, int[] orientations) {
		this.pieces = pieces;
		this.orientations = orientations;
	}

	public CubeState() {
		pieces = SOLVED_PIECES.clone();
		orientations = SOLVED_ORIENTATIONS.clone();
	}

	public boolean isSolved() {
		return Arrays.equals(pieces, SOLVED_PIECES)
				&& Arrays.equals(orientations, SOLVED_ORIENTATIONS);
	}

	private static final char[][] solved_cube = { { 'U', 'R', 'F' },
			{ 'U', 'F', 'L' }, { 'U', 'B', 'R' }, { 'U', 'L', 'B' },
			{ 'D', 'F', 'R' }, { 'D', 'L', 'F' }, { 'D', 'R', 'B' },
			{ 'D', 'B', 'L' } };

	/**
	 * Takes in an array of pieces and their orientations and return a char[][]
	 * containing the stickers on each side.
	 */
	private char[][] spit_out_colors() {
		char[][] actual_colors = new char[8][];
		int location = 0;
		for (int i = 0; i < pieces.length; i++) {
			int piece = pieces[i], orientation = orientations[i];
			char[] current_chunk = new char[3];
			current_chunk[0] = (solved_cube[piece][(orientation) % 3]); // top
			// piece
			current_chunk[1] = (solved_cube[piece][(1 + orientation) % 3]); // right
			// piece
			current_chunk[2] = (solved_cube[piece][(2 + orientation) % 3]); // left
			// piece
			actual_colors[location] = current_chunk;
			location++;
		}
		return actual_colors;
	}

	private static final HashMap<String, String> COLOR_SCHEME = new HashMap<String, String>();
	static {
		COLOR_SCHEME.put("F", "green");
		COLOR_SCHEME.put("U", "gray");
		COLOR_SCHEME.put("R", "red");
		COLOR_SCHEME.put("B", "blue");
		COLOR_SCHEME.put("L", "orange");
		COLOR_SCHEME.put("D", "yellow");
	}

	private static String myFormat(String format, Object... args) {
		// nasty hack so the cube is readable below
		return String.format(format.replaceAll("@", "%c"), args);
	}

	public String display(boolean dotty) {
		String nl = dotty ? "<br align=\"left\" />" : "\n";
		char[][] current_state = spit_out_colors();
		String cube_string = "";
		cube_string += myFormat(
				"                                   ___________%s", nl);
		cube_string += myFormat(
				"                                  |     |     |%s", nl);
		cube_string += myFormat(
				"                   ____________   |  @  |  @  |%s",
				current_state[3][2], current_state[2][1], nl);
		cube_string += myFormat(
				"   /|             /  @  /  @  /|  |_____|_____|%s",
				current_state[3][0], current_state[2][0], nl);
		cube_string += myFormat(
				"  / |            /_____/_____/ |  |     |     |%s", nl);
		cube_string += myFormat(
				" /| |           /  @  /  @  /| |  |  @  |  @  |%s",
				current_state[1][0], current_state[0][0], current_state[7][1],
				current_state[6][2], nl);
		cube_string += myFormat(
				"/ |@|          /_____/_____/ |@|  |_____|_____|%s",
				current_state[3][1], current_state[2][2], nl);
		cube_string += myFormat(
				"|@| |          |     |     |@| |     Back (mirror)%s",
				current_state[1][2], current_state[0][1], nl);
		cube_string += myFormat("| |/|          |  @  |  @  | |/|%s",
				current_state[1][1], current_state[0][2], nl);
		cube_string += myFormat("|/|@|          |_____|_____|/|@|%s",
				current_state[7][2], current_state[6][1], nl);
		cube_string += myFormat("|@| |          |     |     |@| |%s",
				current_state[5][1], current_state[4][2], nl);
		cube_string += myFormat("| |/           |  @  |  @  | |/%s",
				current_state[5][2], current_state[4][1], nl);
		cube_string += myFormat("|/Left (mirror)|_____|_____|/%1$s%1$s%1$s", nl);
		cube_string += myFormat("                   ____________%s", nl);
		cube_string += myFormat("                  /  @  /  @  /%s",
				current_state[7][0], current_state[6][0], nl);
		cube_string += myFormat("                 /_____/_____/%s", nl);
		cube_string += myFormat("                /  @  /  @  /%s",
				current_state[5][0], current_state[4][0], nl);
		cube_string += myFormat("               /_____/_____/%s", nl);
		cube_string += myFormat("               Down (mirror)%s", nl);
		if (dotty)
			for (String face : COLOR_SCHEME.keySet())
				cube_string = cube_string.replaceAll(face, "<font color=\""
						+ COLOR_SCHEME.get(face) + "\">" + face + "</font>");
		return cube_string;
	}

	public void set(State s) {
		if (s instanceof CubeState) {
			CubeState cs = (CubeState) s;
			for (int i = 0; i < pieces.length; i++)
				pieces[i] = cs.pieces[i];
			for (int i = 0; i < orientations.length; i++)
				orientations[i] = cs.orientations[i];
		} else
			throw new Error("Type mismatch");
	}

	public boolean equals(CubeState other) {
		return Arrays.equals(pieces, other.pieces)
				&& Arrays.equals(orientations, other.orientations);
	}
}
