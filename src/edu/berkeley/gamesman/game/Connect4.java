package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.TieredGame;
import edu.berkeley.gamesman.hasher.PerfectConnect4Hash;
import edu.berkeley.gamesman.util.DependencyResolver;
import edu.berkeley.gamesman.util.Util;

/**
 * Connect 4!
 * Boards are stored in row-major format, bottom row first
 * e.g
 * O
 * XX
 * 
 * is [[xo][x ]]
 * @author Steven Schlansker
 */
public class Connect4 extends TieredGame<char[][]> {

	private static final long serialVersionUID = -1234567890123456L;

	final char[] pieces = {'X','O'};
	
	int piecesToWin=4;
	
	static {
		DependencyResolver.allowHasher(Connect4.class, PerfectConnect4Hash.class);
	}

	/**
	 * Connect4 Constructor
	 * Creates the hashers we use (does not use the command-line specified one, needs special hasher)
	 * @param conf the configuration
	 */
	public Connect4(Configuration conf){
		super(conf);
		piecesToWin = Integer.parseInt(conf.getProperty("connect4.pieces","4"));
	}
	
	@Override
	public Collection<char[][]> startingPositions() {
		ArrayList<char[][]> boards = new ArrayList<char[][]>();
		boards.add(new char[gameWidth][gameHeight]);
		return boards;
	}

	@Override
	public int getDefaultBoardHeight() {
		return 6;
	}

	@Override
	public int getDefaultBoardWidth() {
		return 7;
	}

	@Override
	public PrimitiveValue primitiveValue(char[][] pos) {
		// Check horizontal wins
		for(int y = 0; y < gameHeight; y++){
			char test = pos[0][y];
			byte numSeen = 1;
			for(int x = 1; x < gameWidth; x++){
				if(pos[x][y] == ' ')
					numSeen = 0;
				if(pos[x][y] == test)
					numSeen++;
				else{
					numSeen = 1;
					test = pos[x][y];
				}
				if(numSeen == piecesToWin)
					return PrimitiveValue.Lose;
			}
		}
		
		// Check vertical wins
		for(int x = 0; x < gameWidth; x++){
			char test = pos[x][0];
			byte numSeen = 1;
			for(int y = 1; y < gameHeight; y++){
				if(pos[x][y] == ' ') break;
				if(pos[x][y] == test)
					numSeen++;
				else{
					numSeen = 1;
					test = pos[x][y];
				}
				if(numSeen == piecesToWin)
					return PrimitiveValue.Lose;
			}
		}
		
		// Check diagonal-right wins
		
		for(int x = -gameHeight + piecesToWin; x < gameWidth - piecesToWin; x++){
			char test = '?';
			byte numSeen = 0;
			for(int y = 0; y < gameHeight; y++){
				int cx = x + y;
				if(cx < 0)
					continue;
				if(cx >= gameWidth)
					break;
				if(pos[cx][y] == ' ')
					numSeen = 0;
				if(test == '?')
					test = pos[cx][y];
				
				if(test == pos[cx][y])
					numSeen++;
				else{
					test = pos[cx][y];
					numSeen = 0;
				}
				if(numSeen == piecesToWin)
					return PrimitiveValue.Lose;
			}
		}
		
		// Check diagonal-left wins
		
		for(int x = piecesToWin-1; x < gameWidth + gameHeight; x++){
			char test = '?';
			byte numSeen = 0;
			for(int y = 0; y < gameHeight; y++){
				int cx = x - y;
				if(cx < 0)
					break;
				if(cx >= gameWidth)
					continue;
				if(pos[cx][y] == ' ')
					numSeen = 0;
				if(test == '?')
					test = pos[cx][y];
				
				if(test == pos[cx][y])
					numSeen++;
				else{
					test = pos[cx][y];
					numSeen = 0;
				}
				if(numSeen == piecesToWin)
					return PrimitiveValue.Lose;
			}
		}
		
		for(char[] row : pos)
			for(char piece : row)
				if(piece == ' ') return PrimitiveValue.Undecided;
		
		return PrimitiveValue.Tie;
	}

	@Override
	public final String stateToString(char[][] pos) {
		StringBuilder str = new StringBuilder((pos.length+1)*(pos[0].length+1));
		for(int y = pos[0].length-1; y >= 0; y--){
			str.append("|");
			for(int x = 0; x < pos.length; x++){
				str.append(pos[x][y]);
			}
			str.append("|\n");
		}
		return str.toString();
	}

	@Override
	public char[][] stringToState(String pos) {
		char[][] board = new char[gameWidth][gameHeight];
		for(int x = 0 ; x < gameWidth; x++){
			for(int y = 0 ; y < gameHeight; y++){
				board[x][y] = pos.charAt(Util.index(x, y, gameWidth));
			}
		}
		//Util.debug("stringToState yields "+Arrays.deepToString(board));
		return board;
	}

	@Override
	public Collection<char[][]> validMoves(char[][] pos) {
		ArrayList<char[][]> nextBoards = new ArrayList<char[][]>();
		
		char[][] board;
		
		if(primitiveValue(pos) != PrimitiveValue.Undecided)
			return nextBoards;
		
		char nextpiece = nextPiecePlaced(pos);
		
		for(int x = 0; x < gameWidth; x++){
			for(int y = 0; y < gameHeight; y++){
				char c = pos[x][y];
				if(c != 'X' && c != 'O'){
					board = pos.clone();
					board[x] = pos[x].clone();
					board[x][y] = nextpiece;
					nextBoards.add(board);
					break;
				}
			}
		}
		
		return nextBoards;
	}
	
	protected char nextPiecePlaced(char[][] pos){
		int numx = 0,numo = 0;
		for(char[] row : pos)
			for(char piece : row){
				if(piece == 'X') numx++;
				if(piece == 'O') numo++;
			}
		if(numx == numo) return 'X';
		if(numx == numo+1) return 'O';
		Util.fatalError("Invalid board: "+Arrays.deepToString(pos));
		return ' '; // Not reached
	}
	
	@Override
	public String toString(){
		return "Connect 4 "+gameWidth+"x"+gameHeight+" ("+piecesToWin+" to win)";
	}

	@Override
	public String describe() {
		return "Connect4|"+gameWidth+"|"+gameHeight+"|"+piecesToWin;
	}

	@Override
	public char[] pieces() {
		return pieces;
	}

}
