package edu.berkeley.gamesman.game;

import java.util.Collection;

import edu.berkeley.gamesman.core.*;
import edu.berkeley.gamesman.game.util.ConnectGameRearranger;
import edu.berkeley.gamesman.util.Pair;

public class YGame extends TieredIterGame {

	private final Space[][][] yBoard;

	private final int boardSide;

	private final int boardSize;

	private final ItergameState currentState = new ItergameState(0, 0);

	private final ConnectGameRearranger cgr = new ConnectGameRearranger();

	public YGame(Configuration conf) {
		super(conf);
		boardSide = conf.getInteger("game.sideLength", 4);
		yBoard = new Space[3][boardSide - 1][];
		for (int t = 0; t < 3; t++) {
			int i;
			for (i = 0; i < boardSide - 1; i++) {
				yBoard[t][i] = new Space[i + 1];
				for (int c = 0; c <= i; c++)
					yBoard[t][i][c] = new Space(t, i, c);
			}
			i = boardSide - 2;
			for (int c = 0; c <= i; c++) {
				yBoard[t][i][c].isOnEdge[t] = true;
			}
			yBoard[t][i][i].isOnEdge[(t + 1) % 3] = true;
		}
		boardSize = boardSide * (boardSide - 1) / 2 * 3;
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
			else if (s1.c == s2.c && Math.abs(s1.c - s2.c) == 1)
				return true;
			else
				return Math.abs(s1.r - s2.r) == 1
						&& (s1.r - s2.r == s1.c - s2.c);
		} else if ((s2.t + 3 - s1.t) % 3 == 2) {
			Space temp = s2;
			s2 = s1;
			s1 = temp;
		}
		return s2.c == 0 && s1.c == s1.r && s2.r >= s1.r && s2.r - s1.r <= 1;
	}

	private Space[] connectedSpaces(Space s) {
		// TODO: Return all spaces s2 for which connected(s,s2) will return
		// true.
		return null;
	}

	private boolean isWin(char[][][] boardPosition, char piece) {
		// TODO: Trace a path between all three edges of the board according to
		// the above definitions of connectivity and edges.
		return false;
	}

	@Override
	public String displayState() {
		// TODO Find a proper way of printing a Y board to screen
		return null;
	}

	@Override
	public ItergameState getState() {
		return currentState;
	}

	@Override
	public int getTier() {
		return currentState.tier;
	}

	@Override
	public boolean hasNextHashInTier() {
		return cgr.hasNext();
	}

	@Override
	public int maxChildren() {
		return boardSize;
	}

	@Override
	public void nextHashInTier() {
		cgr.next();
	}

	@Override
	public long numHashesForTier() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long numHashesForTier(int tier) {
		// TODO Write method
		return 0L;
	}

	@Override
	public int numStartingPositions() {
		return 1;
	}

	@Override
	public int numberOfTiers() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public PrimitiveValue primitiveValue() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setFromString(String pos) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setStartingPosition(int n) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setState(ItergameState pos) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setTier(int tier) {
		// TODO Auto-generated method stub

	}

	@Override
	public String stateToString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Pair<String, ItergameState>> validMoves() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int validMoves(ItergameState[] moves) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String describe() {
		return "Y";
	}

	private static final class Space {
		public Space(int t, int r, int c) {
			this.t = t;
			this.r = r;
			this.c = c;
		}

		final int t, r, c;
		final boolean[] isOnEdge = new boolean[3];
	}

	private final class YRecord extends Record {
		protected YRecord() {
			super(conf);
		}

		protected YRecord(long state) {
			super(conf);
			set(state);
		}

		protected YRecord(PrimitiveValue pVal) {
			super(conf, pVal);
		}

		@Override
		public long getState() {
			if (conf.remotenessStates > 0) {
				return remoteness;
			} else {
				switch (value) {
				case LOSE:
					return 0L;
				case WIN:
					return 1L;
				default:
					return 0L;
				}
			}
		}

		@Override
		public void set(long state) {
			if ((state & 1) == 1)
				value = PrimitiveValue.WIN;
			else
				value = PrimitiveValue.LOSE;
			if (conf.remotenessStates > 0)
				remoteness = (int) state;
		}
	}

	@Override
	public Record newRecord(PrimitiveValue pv) {
		return new YRecord(pv);
	}

	@Override
	public Record newRecord() {
		return new YRecord();
	}

	@Override
	public Record newRecord(long val) {
		return new YRecord(val);
	}

	@Override
	public long recordStates() {
		if (conf.remotenessStates > 0)
			return boardSize + 1;
		else
			return 2;
	}
}
