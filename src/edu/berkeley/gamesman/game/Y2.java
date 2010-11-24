package edu.berkeley.gamesman.game;

import edu.berkeley.gamesman.core.Configuration;

public class Y2 extends ConnectGame {
	private class Place {
		final Place[] neighbors;
		final int[] whereAmI;
		private final int pos;
		final boolean edge0, edge1, edge2;
		private int neighborNum = 0;
		final int row, col;
		final int shape;
		final int edge;
		final static int INNER = 0, OUTER = 1, RECT = 2;

		Place(int shape, int row, int col, int pos, int numNeighbors,
				boolean edge0, boolean edge1, boolean edge2) {
			this(shape, -1, row, col, pos, numNeighbors, edge0, edge1, edge2);
		}

		Place(int shape, int edge, int row, int col, int pos, int numNeighbors,
				boolean edge0, boolean edge1, boolean edge2) {
			this.shape = shape;
			this.edge = edge;
			this.row = row;
			this.col = col;
			this.pos = pos;
			neighbors = new Place[numNeighbors];
			whereAmI = new int[numNeighbors];
			this.edge0 = edge0;
			this.edge1 = edge1;
			this.edge2 = edge2;
		}

		void addNeighbor(Place p) {
			neighbors[neighborNum] = p;
			for (int i = 0; i < p.neighborNum; i++) {
				if (p.neighbors[i] == this) {
					whereAmI[neighborNum] = i;
					p.whereAmI[i] = neighborNum;
					break;
				}
			}
			neighborNum++;
		}

		public String toString() {
			switch (shape) {
			case INNER:
				return "inner: " + row + "," + col;
			case OUTER:
				return "outer" + edge + ": " + row + "," + col;
			case RECT:
				return "rect" + edge + ": " + row + "," + col;
			default:
				return null;
			}
		}

		public char get() {
			return Y2.this.get(pos);
		}
	}

	private final int centerRows;
	private final int outerRows;
	private final int boardSize;
	private final Place[][][] rectangle;
	private final Place[][] innerTriangle;
	private final Place[][][] outerTriangles;
	private static final String yRep22 = "                   D\n\n\n"
			+ "         L                   E\n" + "                   M\n\n\n"
			+ "          R        A        N\n\n"
			+ "   K                                 F\n"
			+ "               B       C\n\n"
			+ "         Q                    O\n" + "                   P\n"
			+ "   J                                 G\n"
			+ "             I           H\n";
	private static final String yRep31 = "                 G\n\n\n"
			+ "         O       A       H\n\n\n"
			+ "     N       B       C       I\n\n\n"
			+ "         D       E       F\n\n"
			+ "M                                 J\n"
			+ "             L       K\n";
	private static final String yRep41 = "                             K\n\n"
			+ "                     V               L\n"
			+ "                             A\n\n"
			+ "               U                           M\n"
			+ "                        B         C\n\n\n"
			+ "           T       D         E         F       N\n\n\n"
			+ "              G         H         I        J\n\n"
			+ "       S                                          O\n"
			+ "                   R         Q         P\n";
	private final int[] convertIn;
	private final int[] convertOut;

	public Y2(Configuration conf) {
		super(conf);
		centerRows = conf.getInteger("gamesman.game.centerRows", 2);
		outerRows = conf.getInteger("gamesman.game.outerRows", 2);
		boardSize = centerRows * (centerRows + 1) / 2
				+ (centerRows * 2 + outerRows - 1) * outerRows / 2 * 3;
		int i = 0;
		innerTriangle = new Place[centerRows][];
		for (int row = 0; row < centerRows; row++) {
			innerTriangle[row] = new Place[row + 1];
			for (int col = 0; col <= row; col++) {
				if (outerRows == 0) {
					if ((row == 0 || row == centerRows - 1)
							&& (col == 0 || col == centerRows - 1))
						innerTriangle[row][col] = new Place(Place.INNER, row,
								col, i++, 2, col == 0, col == row,
								row == centerRows - 1);
					else if ((row == 0 || row == centerRows - 1)
							|| (col == 0 || col == row))
						innerTriangle[row][col] = new Place(Place.INNER, row,
								col, i++, 4, col == 0, col == row,
								row == centerRows - 1);
					else
						innerTriangle[row][col] = new Place(Place.INNER, row,
								col, i++, 6, false, false, false);
				} else {
					if ((row == 0 || row == centerRows - 1)
							&& (col == 0 || col == centerRows - 1))
						innerTriangle[row][col] = new Place(Place.INNER, row,
								col, i++, 5, false, false, false);
					else
						innerTriangle[row][col] = new Place(Place.INNER, row,
								col, i++, 6, false, false, false);
				}
			}
		}
		outerTriangles = new Place[3][outerRows][];
		rectangle = new Place[3][outerRows][centerRows - 1];
		boolean[] edges = new boolean[3];
		for (int edge = 0; edge < 3; edge++) {
			int lastEdge = (edge + 2) % 3;
			int nextEdge = (edge + 1) % 3;
			edges[lastEdge] = false;
			for (int row = 0; row < outerRows; row++) {
				outerTriangles[edge][row] = new Place[row + 1];
				edges[edge] = (row == outerRows - 1);
				for (int col = 0; col <= row; col++) {
					int numNeighbors;
					edges[nextEdge] = col == 0 && edges[edge];
					if (edges[nextEdge])
						numNeighbors = 3;
					else if (edges[edge])
						numNeighbors = 4;
					else
						numNeighbors = 6;
					outerTriangles[edge][row][col] = new Place(Place.OUTER,
							edge, row, col, i++, numNeighbors, edges[0],
							edges[1], edges[2]);
				}
				edges[nextEdge] = false;
				for (int col = 0; col < centerRows - 1; col++) {
					int numNeighbors;
					if (edges[edge])
						numNeighbors = 4;
					else
						numNeighbors = 6;
					rectangle[edge][row][col] = new Place(Place.RECT, edge,
							row, col, i++, numNeighbors, edges[0], edges[1],
							edges[2]);
				}
			}
		}

		if (i != boardSize) {
			throw new Error("Wrong number of places");
		}

		for (int row = 0; row < centerRows; row++) {
			for (int col = 0; col <= row; col++) {
				Place s = innerTriangle[row][col];
				if (row == 0 && col == 0) {
					if (outerRows > 0) {
						s.addNeighbor(rectangle[0][0][0]);
						s.addNeighbor(outerTriangles[0][0][0]);
						s.addNeighbor(rectangle[1][0][centerRows - 2]);
					}
					s.addNeighbor(innerTriangle[row + 1][col + 1]);
					s.addNeighbor(innerTriangle[row + 1][col]);
				} else if (col == 0) {
					s.addNeighbor(innerTriangle[row - 1][col]);
					s.addNeighbor(innerTriangle[row][col + 1]);
					if (row < centerRows - 1) {
						s.addNeighbor(innerTriangle[row + 1][col + 1]);
						s.addNeighbor(innerTriangle[row + 1][col]);
						if (outerRows > 0) {
							s.addNeighbor(rectangle[0][0][row]);
						}
					} else if (outerRows > 0) {
						s.addNeighbor(rectangle[2][0][0]);
						s.addNeighbor(outerTriangles[2][0][0]);
					}
					if (outerRows > 0) {
						s.addNeighbor(rectangle[0][0][row - 1]);
					}
				} else if (col == row) {
					if (outerRows > 0) {
						s.addNeighbor(rectangle[1][0][centerRows - 1 - row]);
					}
					if (row < centerRows - 1) {
						if (outerRows > 0) {
							s.addNeighbor(rectangle[1][0][centerRows - 2 - row]);
						}
						s.addNeighbor(innerTriangle[row + 1][col + 1]);
						s.addNeighbor(innerTriangle[row + 1][col]);
					} else if (outerRows > 0) {
						s.addNeighbor(outerTriangles[1][0][0]);
						s.addNeighbor(rectangle[2][0][centerRows - 2]);
					}
					s.addNeighbor(innerTriangle[row][col - 1]);
					s.addNeighbor(innerTriangle[row - 1][col - 1]);
				} else if (row == centerRows - 1) {
					s.addNeighbor(innerTriangle[row][col - 1]);
					s.addNeighbor(innerTriangle[row - 1][col - 1]);
					s.addNeighbor(innerTriangle[row - 1][col]);
					s.addNeighbor(innerTriangle[row][col + 1]);
					if (outerRows > 0) {
						s.addNeighbor(rectangle[2][0][col]);
						s.addNeighbor(rectangle[2][0][col - 1]);
					}
				} else {
					s.addNeighbor(innerTriangle[row - 1][col - 1]);
					s.addNeighbor(innerTriangle[row - 1][col]);
					s.addNeighbor(innerTriangle[row][col + 1]);
					s.addNeighbor(innerTriangle[row + 1][col + 1]);
					s.addNeighbor(innerTriangle[row + 1][col]);
					s.addNeighbor(innerTriangle[row][col - 1]);
				}
			}
		}
		for (int edge = 0; edge < 3; edge++) {
			int lastEdge = (edge + 2) % 3;
			int nextEdge = (edge + 1) % 3;
			for (int row = 0; row < outerRows; row++) {
				for (int col = 0; col <= row; col++) {
					Place s = outerTriangles[edge][row][col];
					if (col == 0) {
						if (row < outerRows - 1) {
							s.addNeighbor(rectangle[nextEdge][row + 1][centerRows - 2]);
						}
						s.addNeighbor(rectangle[nextEdge][row][centerRows - 2]);
					} else {
						s.addNeighbor(outerTriangles[edge][row][col - 1]);
						s.addNeighbor(outerTriangles[edge][row - 1][col - 1]);
					}
					if (row == 0) {
						switch (edge) {
						case 0:
							s.addNeighbor(innerTriangle[0][0]);
							break;
						case 1:
							s.addNeighbor(innerTriangle[centerRows - 1][centerRows - 1]);
							break;
						case 2:
							s.addNeighbor(innerTriangle[centerRows - 1][0]);
							break;
						}
					} else {
						if (row == col) {
							s.addNeighbor(rectangle[edge][row - 1][0]);
						} else {
							s.addNeighbor(outerTriangles[edge][row - 1][col]);
						}
					}
					if (row == col) {
						s.addNeighbor(rectangle[edge][row][0]);
					} else {
						s.addNeighbor(outerTriangles[edge][row][col + 1]);
					}
					if (row < outerRows - 1) {
						s.addNeighbor(outerTriangles[edge][row + 1][col + 1]);
						s.addNeighbor(outerTriangles[edge][row + 1][col]);
					}
				}
				for (int col = 0; col < centerRows - 1; col++) {
					Place s = rectangle[edge][row][col];
					if (col == 0) {
						if (row < outerRows - 1)
							s.addNeighbor(outerTriangles[edge][row + 1][row + 1]);
						s.addNeighbor(outerTriangles[edge][row][row]);
					} else {
						if (row < outerRows - 1)
							s.addNeighbor(rectangle[edge][row + 1][col - 1]);
						s.addNeighbor(rectangle[edge][row][col - 1]);
					}
					if (row == 0) {
						switch (edge) {
						case 0:
							s.addNeighbor(innerTriangle[col][0]);
							s.addNeighbor(innerTriangle[col + 1][0]);
							break;
						case 1:
							s.addNeighbor(innerTriangle[centerRows - col - 1][centerRows
									- col - 1]);
							s.addNeighbor(innerTriangle[centerRows - col - 2][centerRows
									- col - 2]);
							break;
						case 2:
							s.addNeighbor(innerTriangle[centerRows - 1][col]);
							s.addNeighbor(innerTriangle[centerRows - 1][col + 1]);
							break;
						}
					} else {
						s.addNeighbor(rectangle[edge][row - 1][col]);
						if (col == centerRows - 2)
							s.addNeighbor(outerTriangles[lastEdge][row - 1][0]);
						else
							s.addNeighbor(rectangle[edge][row - 1][col + 1]);
					}
					if (col == centerRows - 2) {
						s.addNeighbor(outerTriangles[lastEdge][row][0]);
					} else {
						s.addNeighbor(rectangle[edge][row][col + 1]);
					}
					if (row < outerRows - 1) {
						s.addNeighbor(rectangle[edge][row + 1][col]);
					}
				}
			}
		}

		convertIn = new int[boardSize];
		convertOut = new int[boardSize];

		int innerSize = centerRows * (centerRows + 1) / 2;
		for (i = 0; i < innerSize; i++) {
			convertIn[i] = i;
			convertOut[i] = i;
		}
		i = innerSize;
		for (int row = outerRows - 1; row >= 0; row--) {
			for (int prevEdge = 0; prevEdge < 3; prevEdge++) {
				int edge = prevEdge + 1;
				if (edge >= 3)
					edge -= 3;
				convertIn[outerTriangles[prevEdge][row][0].pos] = i;
				convertOut[i] = outerTriangles[prevEdge][row][0].pos;
				i++;
				for (int col = centerRows - 2; col >= 0; col--) {
					convertIn[rectangle[edge][row][col].pos] = i;
					convertOut[i] = rectangle[edge][row][col].pos;
					i++;
				}
				for (int col = row; col > 0; col--) {
					convertIn[outerTriangles[edge][row][col].pos] = i;
					convertOut[i] = outerTriangles[edge][row][col].pos;
					i++;
				}
			}
		}
	}

	@Override
	protected int getBoardSize() {
		return boardSize;
	}

	@Override
	protected boolean isWin(char c) {
		for (int i = 0; i < outerRows + centerRows; i++) {
			Place p = getPlaceOnEdge(i);
			if (p.get() == c) {
				int startFrom = 0;
				boolean edge1Seen = p.edge1;
				OUTER: while (true) {
					Place nextNeighbor = null;
					int next = startFrom;
					while (true) {
						if (next >= p.neighborNum) {
							if (p.edge1)
								edge1Seen = true;
							if (p.edge0 || p.edge2)
								break OUTER;
							next = 0;
						}
						Place neighbor = p.neighbors[next];
						if (neighbor.get() == c) {
							nextNeighbor = neighbor;
							break;
						}
						next++;
					}
					startFrom = p.whereAmI[next] + 1;
					p = nextNeighbor;
				}
				if (p.edge2)
					return edge1Seen;
				else if (p.edge0)
					i = whereOnEdge(p);
				else
					throw new Error("Loop should not have terminated");
			}
		}
		return false;
	}

	Place getPlaceOnEdge(int i) {
		if (outerRows == 0)
			return innerTriangle[i][0];
		else if (i == outerRows + centerRows - 1)
			return outerTriangles[2][outerRows - 1][0];
		else if (i < outerRows)
			return outerTriangles[0][outerRows - 1][i];
		else
			return rectangle[0][outerRows - 1][i - outerRows];
	}

	int whereOnEdge(Place p) {
		if (outerRows == 0) {
			if (p.shape == Place.INNER && p.col == 0)
				return p.row;
			else
				throw new Error("Not on edge 0");
		} else if (p.shape == Place.OUTER) {
			if (p.edge == 2) {
				if (p.row == outerRows - 1 && p.col == 0)
					return centerRows + outerRows - 1;
				else
					throw new Error("Not on edge 0");
			} else if (p.edge == 0) {
				if (p.row == outerRows - 1)
					return p.col;
				else
					throw new Error("Not on edge 0");
			} else
				throw new Error("Not on edge 0");
		} else if (p.shape == Place.RECT) {
			if (p.edge == 0 && p.row == outerRows - 1) {
				return p.col + outerRows;
			} else {
				throw new Error("Not on edge 0");
			}
		} else
			throw new Error("Not on edge 0");
	}

	@Override
	public String displayState() {
		String str = null;
		if (centerRows == 2 && outerRows == 2) {
			str = yRep22;
		} else if (centerRows == 3 && outerRows == 1) {
			str = yRep31;
		} else if (centerRows == 4 && outerRows == 1) {
			str = yRep41;
		} else
			return convertOutString(makeCharArray());
		char[] arr = convertOutString(makeCharArray()).toCharArray();
		char[] newArr = str.toCharArray();
		for (int i = 0; i < newArr.length; i++) {
			if (newArr[i] >= 'A' && newArr[i] <= 'Z') {
				newArr[i] = arr[newArr[i] - 'A'];
				if (newArr[i] == ' ')
					newArr[i] = '-';
			}
		}
		return new String(newArr);
	}

	@Override
	public String convertOutString(char[] arr) {
		char[] resultArr = new char[arr.length];
		for (int i = 0; i < arr.length; i++) {
			resultArr[i] = arr[convertOut[i]];
		}
		return new String(resultArr);
	}

	@Override
	public int translateOut(int i) {
		return convertIn[i];
	}

	@Override
	public char[] convertInString(String s) {
		char[] arr = s.toCharArray();
		char[] resultArr = new char[arr.length];
		for (int i = 0; i < arr.length; i++)
			resultArr[i] = arr[convertIn[i]];
		return resultArr;
	}

	@Override
	public String describe() {
		return "Y " + centerRows + "x" + outerRows;
	}

	@Override
	protected void setToCharArray(char[] myPieces) {
	}
}
