package Games.PieceGame.TicTacToe;

import Games.Interfaces.Locator;
import Games.PieceGame.PieceGame;
import Games.PieceGame.RectanglePieceLocator;
import Helpers.Piece;
import Helpers.Primitive;
import Helpers.Tuple;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class TicTacToe extends PieceGame implements Serializable {
    public int width;
    public int height;
    public int win;
    RectanglePieceLocator locator;
    Piece[] gameStartingPosition;

    public TicTacToe(String[] args) {
        this(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]));
    }

    public TicTacToe(int w, int h, int wi) {
        width = w;
        height = h;
        win = wi;
        locator = new RectanglePieceLocator(w,h);
        gameStartingPosition = new Piece[w*h];
        Arrays.fill(gameStartingPosition, Piece.EMPTY);
    }

    /**
     * Used to start of the first position of the game
     *
     * @return The starting position of the game
     */
    @Override
    public Piece[] getStartingPosition() {
        return gameStartingPosition;
    }

    /**
     * @return The name of the game
     */
    @Override
    public String getName() {
        return "Tic_Tac_Toe";
    }

    /**
     * @return The name of the variant
     */
    @Override
    public String getVariant() {
        return String.format("%d_x_%d_win_in_%d", width, height, win);
    }

    /**
     * @return The value of the highest tier in the game
     */
    @Override
    public int getMaxTiers() {
        return width*height;
    }

    @Override
    public Locator getLocator() {
        return locator;
    }

    /**
     * Calculates the location of a certain board state
     *
     * @param board
     * @return The byte offset of the state
     */
    @Override
    public long calculateLocation(Piece[] board) {
        return locator.calculateLocation(board, getTier());
    }

    /**
     * Calculates the location of a certain board state given a number of pieces
     *
     * @param board
     * @param numPiece
     * @return The byte offset of the state
     */
    @Override
    public long calculateLocation(Piece[] board, int numPiece) {
        return locator.calculateLocation(board, numPiece);
    }

    /**
     * Use a created move and place it on the board
     *
     * @param position Board state before moving
     * @param move     The move to make
     * @param p        The Piece to place
     * @return The new position after making the move
     */
    @Override
    public Piece[] doMove(Piece[] position, int move, Piece p) {
        Piece[] newPosition = new Piece[getSize()];
        System.arraycopy(position, 0, newPosition, 0, position.length);
        newPosition[move] = p;
        return newPosition;
    }

    /**
     * Creates a list of valid moves from a position
     *
     * @param position The position to generate moves for
     * @return A newly created list that contains the int moves
     */
    @Override
    public List<Integer> generateMoves(Piece[] position) {
        List<Integer> ret = new ArrayList<>();
        for (int i = 0; i < getSize(); i++) {
            if (position[i] == Piece.EMPTY) {
                ret.add(i);
            }
        }
        return ret;
    }

    /**
     * Determines if a position is a primitive, and if so returns its remoteness as well
     *
     * @param position Position to check
     * @param placed   The last piece placed
     * @return A tuple of Primitive value and remoteness
     */
    @Override
    public Tuple<Primitive, Integer> isPrimitive(Piece[] position, Piece placed) {
        boolean full = true;
        for (int pos = 0; pos < getSize(); pos++) {
            int row = pos / width;
            int col = pos % width;
            Piece pieceAtPos = position[pos];
            if (pieceAtPos == Piece.EMPTY) {
                full = false;
            }
            if (pieceAtPos != placed) {
                continue;
            }
            //check horizontal
            if (col <= width - win) {
                for (int h = pos + 1; h < pos + win; h++) {
                    if (position[pos] != placed) {
                        break;
                    }
                    if (h == pos + win - 1) {
                        return new Tuple<>(Primitive.LOSS, 0);
                    }
                }
            }
            //check vertical
            if (row <= height - win) {
                for (int r = pos + width; r < pos + (win*width); r+=width) {
                    if (position[pos] != placed) {
                        break;
                    }
                    if (r == pos + ((win - 1)*width)) {
                        return new Tuple<>(Primitive.LOSS, 0);
                    }
                }
            }
            //check TL-BR diagonal
            if (row <= height - win && col <= width - win) {
                for (int tlbr = pos + width + 1; tlbr < pos + (win*width) + win; tlbr+=width + 1) {
                    if (position[pos] != placed) {
                        break;
                    }
                    if (tlbr == pos + ((win - 1)*width) + (win - 1)) {
                        return new Tuple<>(Primitive.LOSS, 0);
                    }
                }
            }
            //check BL-TR diagonal
            if (row >= win && col <= width - win) {
                for (int bltr = pos - width + 1; bltr < pos - (win*width) + win; bltr+=1 - width) {
                    if (position[pos] != placed) {
                        break;
                    }
                    if (bltr == pos - ((win - 1)*width) + (win - 1)) {
                        return new Tuple<>(Primitive.LOSS, 0);
                    }
                }
            }
        }
        if (full) {
            return new Tuple<>(Primitive.TIE, 0);
        } else {
            return new Tuple<>(Primitive.NOT_PRIMITIVE, 0);
        }
    }

    /**
     * Determines if a position is a primitive, and if so returns its remoteness as well.
     * Speed improved by checking the location placed only
     *
     * @param position Position to check
     * @param placed   The last piece placed
     * @param loc The location (index of position) where the last piece was placed
     * @return A tuple of Primitive value and remoteness
     */
    @Override
    public Tuple<Primitive, Integer> isPrimitive(Piece[] position, Piece placed, int loc) {
        if (loc == -1) {
            return isPrimitive(position, placed);
        }
        boolean full = true;
        int row = loc / width;
        int col = loc % width;
        //check horizontal
        int hiar = 1;
        //count horizontal right
        for (int hr = loc + 1; hr < Math.min((row + 1)*width, loc + win); hr++) {
            Piece pieceAtPos = position[hr];
            if (pieceAtPos == placed) {
                hiar += 1;
            } else if (pieceAtPos == Piece.EMPTY && full) {
                full = false;
                break;
            } else {
                break;
            }
        }
        if (hiar >= win) {
            return new Tuple<>(Primitive.LOSS, 0);
        }
        //count horizontal left
        for (int hl = loc - 1; hl >= Math.max(row*width, loc - win + 1); hl--) {
            Piece pieceAtPos = position[hl];
            if (pieceAtPos == placed) {
                hiar += 1;
            } else if (pieceAtPos == Piece.EMPTY && full) {
                full = false;
                break;
            } else {
                break;
            }
        }
        if (hiar >= win) {
            return new Tuple<>(Primitive.LOSS, 0);
        }
        //check vertical
        int viar = 1;
        //count vertical down
        for (int vd = loc + width; vd < Math.min(getSize() - width + col, loc+(width*win)); vd+=width) {
            Piece pieceAtPos = position[vd];
            if (pieceAtPos == placed) {
                viar += 1;
            } else if (pieceAtPos == Piece.EMPTY && full) {
                full = false;
                break;
            } else {
                break;
            }
        }
        if (viar >= win) {
            return new Tuple<>(Primitive.LOSS, 0);
        }
        //count vertical up
        for (int vu = loc - width; vu >= Math.max(col, loc-(width*(win - 1))); vu-=width) {
            Piece pieceAtPos = position[vu];
            if (pieceAtPos == placed) {
                viar += 1;
            } else if (pieceAtPos == Piece.EMPTY && full) {
                full = false;
                break;
            } else {
                break;
            }
        }
        if (viar >= win) {
            return new Tuple<>(Primitive.LOSS, 0);
        }
        //check diagonal tlbr
        int tlbriar = 1;
        //count br
        for (int br = loc + width + 1; br < Math.min(loc + (width - (col + 1))*(width + 1), loc + win*(width + 1)); br+=width + 1) {
            Piece pieceAtPos = position[br];
            if (pieceAtPos == placed) {
                tlbriar += 1;
            } else if (pieceAtPos == Piece.EMPTY && full) {
                full = false;
                break;
            } else {
                break;
            }
        }
        if (tlbriar >= win) {
            return new Tuple<>(Primitive.LOSS, 0);
        }
        //count tl
        for (int tl = loc - width - 1; tl >= Math.max(loc - col*(width + 1), loc - (win - 1)*(width + 1)); tl-=width) {
            Piece pieceAtPos = position[tl];
            if (pieceAtPos == placed) {
                tlbriar += 1;
            } else if (pieceAtPos == Piece.EMPTY && full) {
                full = false;
                break;
            } else {
                break;
            }
        }
        if (tlbriar >= win) {
            return new Tuple<>(Primitive.LOSS, 0);
        }
        //check diagonal bltr
        int bltriar = 1;
        //count br
        for (int tr = loc - (width - 1); tr < Math.min(loc - (width - (col + 1))*(width - 1), loc - win*(width - 1)); tr+=1 - width) {
            Piece pieceAtPos = position[tr];
            if (pieceAtPos == placed) {
                bltriar += 1;
            } else if (pieceAtPos == Piece.EMPTY && full) {
                full = false;
                break;
            } else {
                break;
            }
        }
        if (bltriar >= win) {
            return new Tuple<>(Primitive.LOSS, 0);
        }
        //count tl
        for (int bl = loc + (width - 1); bl >= Math.max(loc + col*(width - 1), loc + (win - 1)*(width - 1)); bl+=width - 1) {
            Piece pieceAtPos = position[bl];
            if (pieceAtPos == placed) {
                bltriar += 1;
            } else if (pieceAtPos == Piece.EMPTY && full) {
                full = false;
                break;
            } else {
                break;
            }
        }
        if (bltriar >= win) {
            return new Tuple<>(Primitive.LOSS, 0);
        }
        //check if actually full
        if (full) {
            for (int pos = 0; pos < getSize(); pos++) {
                if (position[pos] == Piece.EMPTY) {
                    full = false;
                    break;
                }
            }
        }
        if (full) {
            return new Tuple<>(Primitive.TIE, 0);
        } else {
            return new Tuple<>(Primitive.NOT_PRIMITIVE, 0);
        }
    }

    /**
     * The symmetrical move to another move, used to help remove symmetries
     *
     * @param move Move to flip
     * @return A new move that is symmetric
     */
    @Override
    public int symMove(int move) {
        int row = move / width;
        int col = move % width;
        return (height - (row + 1))*width + (width - (col + 1));
    }

    /**
     * @return The current size of the board
     */
    @Override
    public int getSize() {
        return width*height;
    }

    /**
     * Prints the current board state
     *
     * @param board
     */
    @Override
    public void printBoard(Piece[] board) {
        StringBuilder stb = new StringBuilder();
        for (int r = height - 1; r >= 0; r--) {
            for (int c = width - 1; c >= 0; c--) {
                switch(board[r + c * height]) {
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
        for (int c = width - 1; c >= 0; c--) {
            stb.append(' ');
            stb.append(c + 1);
        }

        System.out.println(stb.toString());
    }

    private long getHash(Piece[] board) {
        long hash = 0;
        long oppHash = 0;
        for(int i = 0; i < width; i++)
        {
            int val = 1;
            for(int j = 0; j < height;j++)
            {
                if(board[i*height+j] == Piece.BLUE) val = val << 1;
                else if(board[i*height+j] == Piece.RED) val = (val << 1) + 1;
            }
            hash += ((long)(val))<<(i*(height+1));
            oppHash += ((long)(val))<<((width-i-1)*(height+1));
        }
        return Math.min(hash, oppHash);
    }

    private static void recurse(HashSet<Long> primitives, Piece[] position, TicTacToe t, String move, Piece piece) {
        if(move.length() < 5) System.out.println(move);
        List<Integer> moves = t.generateMoves(position);
        for(int i = 0; i < moves.size();i++)
        {
            if(moves.get(i) == -1) return;
            Piece[] newPos = t.doMove(position, moves.get(i), piece);
            if(t.isPrimitive(newPos, piece, moves.get(i)).x != Primitive.NOT_PRIMITIVE)
            {
                primitives.add(t.getHash(newPos));
            }
            else
            {
                recurse(primitives,newPos, t, move+i, piece == Piece.BLUE ? Piece.RED : Piece.BLUE);
            }
        }
    }

    public static void main(String[] args) {
        TicTacToe t = new TicTacToe(new String[]{"3","3","3"});
        Piece[] pos = t.getStartingPosition();
        HashSet<Long> hashset = new HashSet<>();
        recurse(hashset, pos, t, "", Piece.BLUE);
        System.out.printf("Number of primitives: %d", hashset.size());
    }
}
