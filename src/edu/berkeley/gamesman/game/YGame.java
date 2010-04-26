package edu.berkeley.gamesman.game;

import edu.berkeley.gamesman.core.*;

/**
 * The game Y
 * 
 * @author dnspies
 */
public class YGame extends ConnectGame {
	private final class Space {
		// t = triangle, r = row c = column
		final int t, r, c;
		final int charNum;
		int iter;
		final boolean[] isOnEdge = new boolean[3];
		final Space[] connectedSpaces;
		
		Space(int t, int r, int c, int charNum) {
			this.t = t;
			this.r = r;
			this.c = c;
			// index into board
			this.charNum = charNum;
			this.iter = 0;
			connectedSpaces = null; // TODO Correct this. First initiate with
			// correct size, then fill it in (in
			// constructor of YGame) when all spaces
			// have been initialized
		}

		char getChar() {
			return board[charNum];
		}

		void setChar(char c) {
			board[charNum] = c;
		}
	}
	
	// Added a stack data structure for use in the isWin() function. - Rohit
	private final class Stack {
		
		// stackNode object. Contains the Space and next pointer.
		private final class stackNode {
			Space data;
			stackNode next;
			
			stackNode(Space s) {
				data = s;
				next = null;
			}
		}

		stackNode top;
		
		// Stack constructor
		Stack() {
			top = null;
		}
			
		void push(Space s) {
			if(top == null) {
				top = new stackNode(s);
				top.next = null;
			}
			else {
				stackNode temp = new stackNode(s);
				temp.next = top;
				top = temp;
			}
		}
			
		Space pop() {
			if(top == null)
				return null;
			stackNode temp = top;
			top = top.next;
			return temp.data;
		}
	} // End stack class
		
	private Space[][][] yBoard;

	private char[] board;

	private int boardSide;

	private int boardSize;

	/**
	 * @param conf
	 *            The configuration object
	 */
	public void initialize(Configuration conf) {
		super.initialize(conf);
		boardSide = conf.getInteger("game.sideLength", 4);
		yBoard = new Space[3][boardSide - 1][];
		int n = 0;
		for (int t = 0; t < 3; t++) {
			int i;
			for (i = 0; i < boardSide - 1; i++) {
				yBoard[t][i] = new Space[i + 1];
				for (int c = 0; c <= i; c++)
					yBoard[t][i][c] = new Space(t, i, c, n++);
			}
			i = boardSide - 2;
			for (int c = 0; c <= i; c++) {
				yBoard[t][i][c].isOnEdge[t] = true;
			}
			yBoard[t][i][i].isOnEdge[(t + 1) % 3] = true;
		}
		boardSize = boardSide * (boardSide - 1) / 2 * 3;
		board = new char[boardSize];
	}

	/**
	 * Given any two spaces who know their position, this method tells whether
	 * they are connected.
	 * 
	 * @param s1
	 *            First Space
	 * @param s2
	 *            Second Space
	 * @return Are they connected?
	 */
	private boolean connected(Space s1, Space s2) {
		if (s1.t == s2.t) {
			if (s1.r == s2.r && Math.abs(s1.c - s2.c) == 1)
				return true;
			else if (s1.c == s2.c && Math.abs(s1.r - s2.r) == 1)
				return true;
			else
				// correct diagonal
				return Math.abs(s1.r - s2.r) == 1
						&& (s1.r - s2.r == s1.c - s2.c);
		}
		
		// if they are in separate triangles, switch them.
		// add 3 because we don't want to mod a negative value.
		// if they are in separate triangles, switch them.

		else if ((s2.t + 3 - s1.t) % 3 == 2) {
			Space temp = s2;
			s2 = s1;
			s1 = temp;
		}
		// between triangles
		return s2.c == 0 && s1.c == s1.r && s2.r >= s1.r && s2.r - s1.r <= 1;
	}

	@Override
	protected int getBoardSize() {
		return boardSize;
	}

	@Override
	protected char[] getCharArray() {
		return board;
	}
	
	protected Space[] getNeighbors(int t, int r, int c) {
		Space[] neighbors = new Space[6];
		int charnum = yBoard[t][r][c].charNum; // don't know what this is atm...just pass it in to space constructor for now.
		// probably the character piece at the neighbor so i'd have to look it up...
		if(yBoard[t][r][c].isOnEdge[t]) {
			neighbors[0] = new Space(t, r-1, c, charnum);
			neighbors[1] = new Space(t, r, c+1, charnum);
			neighbors[2] = new Space(t, r+1, c+1, charnum);
			neighbors[3] = new Space(t, r+1, c, charnum);
			neighbors[4] = new Space( (t-1+3) % 3, r, r, charnum);
			neighbors[5] = new Space( (t-1+3) % 3, r-1, r-1, charnum);
		}
		else {
			neighbors[0] = new Space(t, r-1, c, charnum);
			neighbors[1] = new Space(t, r, c+1, charnum);
			neighbors[2] = new Space(t, r+1, c+1, charnum);
			neighbors[3] = new Space(t, r+1, c, charnum);
			neighbors[4] = new Space(t, r, c-1, charnum);
			neighbors[5] = new Space(t, r-1, c-1, charnum);
		}
		return neighbors;
		
	}
	
	@Override
	// Uses the stack class in order to avoid recursion.
	protected boolean isWin(char c) {
		Stack spaces = new Stack();
		// start point
		int t = 0;
		int r = 0;
		int col = 0;		
		
		// Need to loop until an end condition i.e. until we know there is no path from all 3 sides.
		
		
		if(yBoard[t][r][col].charNum == c) {
			// Push space on the stack.
			spaces.push(yBoard[t][r][col]);
			
			// Need to check if the space is on the right or bottom triangle so we can set a boolean flag.
			
			// Go to the next space.
			t = yBoard[t][r][col].connectedSpaces[yBoard[t][r][col].iter].t;
			r = yBoard[t][r][col].connectedSpaces[yBoard[t][r][col].iter].r;
			col = yBoard[t][r][col].connectedSpaces[yBoard[t][r][col].iter].c;
		}
		// No piece at its neighbor, so determine the next piece to try.
		else {
			Space prev = spaces.pop();
			while(prev != null) {
				prev.iter++;
				// Keep popping off the stack if iter >= 6 since that means we've exhausted all neighbors.
				if(prev.iter < 6) {
					break;
				}
				prev = spaces.pop();
			}
			// If nothing is on the stack, then we just move to the next space on the left edge.
			if(prev == null) {
				r++;
				// need to move to a different triangle possibly...r++ may not be enough
			}
			// Go to the next connected space.
			else {
				t = prev.connectedSpaces[prev.iter].t;
				r = prev.connectedSpaces[prev.iter].r;
				col = prev.connectedSpaces[prev.iter].c;
			}
		}
	
		return false;
	}

	@Override
	protected void setToCharArray(char[] myPieces) {
		if (board != myPieces)
			for (int i = 0; i < boardSize; i++)
				board[i] = myPieces[i];
	}

	@Override
	public String displayState() {
		StringBuffer s = new StringBuffer();
		s.append("    "+yBoard[0][2][0]+" ----- "+yBoard[0][2][1]+" ----- "+yBoard[0][2][2]+"----"+yBoard[2][2][0]+"\n");
		s.append("   /  \\   /   \\   /  \\ / |\n");
		s.append("   "+yBoard[1][2][2]+"--- "+yBoard[0][1][0]+"------ "+yBoard[0][1][1]+"    "+yBoard[2][1][0]+"   /\n");
		s.append("   | \\ /  \\   /    /    /\n");
		s.append("   \\   "+yBoard[1][1][1]+"    "+yBoard[0][0][0]+"----"+yBoard[2][0][0]+"     /\n");
		s.append("    \\    \\ /  /  |    "+yBoard[2][2][1]+"\n");
		s.append("     \\     "+yBoard[1][0][0]+"-----"+yBoard[2][1][1]+"   /\n");
		s.append("      "+yBoard[1][2][1]+"    |  /  |  /\n");
		s.append("       \\   "+yBoard[1][1][0]+"     | /\n");
		s.append("        \\  |     "+yBoard[2][2][2]+"\n");
		s.append("         \\ |  /\n");
		s.append("           "+yBoard[1][2][0]+"  \n");
		
		return s.toString();
	}

	@Override
	public String describe() {
		return "Y - " + boardSide;
	}
}
