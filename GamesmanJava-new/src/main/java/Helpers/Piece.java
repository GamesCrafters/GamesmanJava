package Helpers;

public enum Piece {
    EMPTY,
    RED,
    BLUE;

    /**
     * @return The opposite color, red if blue, blue if red. Empty throws exception
     */
    public Piece opposite() {
        switch(this) {
            case RED: return BLUE;
            case BLUE: return RED;
            default: throw new IllegalStateException("This should never happen: " + this + " has no opposite.");
        }
    }

    /**
     * Prints the given board assuming column major order from left to right
     * @param board The board
     * @param w Width of the board
     * @param h Height of the board
     */
    public static void printBoard(Piece[] board, int w, int h) {
        StringBuilder stb = new StringBuilder();
        for (int r = h - 1; r >= 0; r--) {
            for (int c = w - 1; c >= 0; c--) {
                switch(board[r + c * h]) {
                    case RED:
                        stb.append("|O");
                        break;
                    case BLUE:
                        stb.append("|X");
                        break;
                    case EMPTY:
                        stb.append("| ");
                }
            }
            stb.append("|\n");
        }
        for (int c = w - 1; c >= 0; c--) {
            stb.append(' ');
            stb.append(c + 1);
        }

        System.out.println(stb.toString());
    }
}
