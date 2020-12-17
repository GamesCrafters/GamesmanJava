package Tight;

import java.io.*;
import java.util.*;

import Games.Connect4;
import Helpers.Piece;
import Helpers.Primitive;
import Helpers.Tuple;
import org.apache.commons.math3.util.CombinatoricsUtils;


public class SolverRAF {

    public static void main (String[] args) {
        SolverRAF s = new SolverRAF(4, 5, 4);
        s.game = new Connect4(s.width, s.height, s.win);
        long start = System.currentTimeMillis();
        s.solve();
        System.out.println("Time taken : " + (((double) System.currentTimeMillis() - start) / 1000));
        s.play();
    }

    int width;
    int height;
    int win;
    Connect4 game;
    private final Piece[] startingPosition;
    int[] offsets;
    private final int[][][] savedRearrange;
    Random rand = new Random();
    String fileName;
    RandomAccessFile raf;

    /** Pieces stored in column major order, starting from bottom right*/
    public SolverRAF(int w, int h, int wi) {
        width = w;
        height = h;
        win = wi;
        startingPosition = new Piece[w*h];
        Arrays.fill(startingPosition, Piece.EMPTY);
        savedRearrange = new int[2 + w*h / 2][2 + w*h/2][w*h + 1];
        for (int i = 0; i < 2 + w*h / 2; i++) {
            for (int j = 0; j < 2 + w*h/2; j++) {
                for (int k = 0; k < w*h + 1; k++) {
                    savedRearrange[i][j][k] = -1;
                }
            }
        }
        setOffsets();
        fileName = "connect4_by_" + width + "_by_" + height + "_win_" + win;

        try {
            raf = new RandomAccessFile(fileName, "rw");
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Cannot find file");
        }

    }

    private void setOffsets() {
        offsets = new int[width*height +1];
        offsets[0] = 1;
        for (int i = 1; i < offsets.length; i++) {
            if (i % 2 == 0) {
                offsets[i] = offsets[i-1] +  rearrange(i/2, i/2, startingPosition.length);
            } else {
                offsets[i] = offsets[i-1] + rearrange((i/2) + 1, i/2, startingPosition.length);
            }
        }
        System.arraycopy(offsets, 0, offsets, 1, offsets.length - 1);
//        for (int i = offsets.length - 1; i > 0; i--) {
//            offsets[i] = offsets[i - 1];
//        }
        offsets[0] = 0;
    }

    private int rearrange(int x, int o, int s) {
        if (s == 0) {
            return 0;
        }
        if (savedRearrange[x][o][s] != -1) {
            return savedRearrange[x][o][s];
        }
        double sFact;
        double oFact;
        double xFact;
        sFact = CombinatoricsUtils.factorialDouble(s);
        oFact = CombinatoricsUtils.factorialDouble(o);
        xFact = CombinatoricsUtils.factorialDouble(x);


        double diffFact = CombinatoricsUtils.factorialDouble(s - x - o);
        double ret = sFact / (oFact * xFact * diffFact);

        int temper = (int) Math.round(ret);
        savedRearrange[x][o][s] = temper;
        return temper;

    }

    private Tuple<Primitive, Integer> getValue(int location) {
        byte b;
        try {
            raf.seek(location);
            b = raf.readByte();
        } catch (Exception e) {
            return null;
        }
        if (b == 0) {
            return null;
        }
//        if (!memo.containsKey(location)) {
//            return null;
//        }
//        byte b = memo.get(location);
        int val = Byte.toUnsignedInt(b);
        int remoteness = (val << 26) >>> 26;
        Primitive p;
        switch((val) >>> 6) {
            case 0:
                p = Primitive.NOT_PRIMITIVE;
                break;
            case 1:
                p = Primitive.LOSS;
                break;
            case 2:
                p = Primitive.WIN;
                break;
            case 3:
                p = Primitive.TIE;
                break;
            default:
                throw new IllegalStateException("two bits should only have those options");
        }
        return new Tuple<>(p, remoteness);
    }

    private byte byteValue(Tuple<Primitive, Integer> p) {
        Integer temp;
        switch (p.x) {
            case NOT_PRIMITIVE:
                temp = 0;
                break;
            case LOSS:
                temp = 1;
                break;
            case WIN:
                temp = 2;
                break;
            case TIE:
                temp = 3;
                break;
            default:
                throw new IllegalStateException("shhouldnt happpen");
        }
        temp = temp << 6;
        temp += p.y;
        return temp.byteValue();
    }

    private int calculateLocation(Piece[] position, int numPieces) {
        int location = offsets[numPieces];
        int numX = (numPieces / 2) + (numPieces % 2);
        int numO = numPieces / 2;
        int numBlanks = position.length - numPieces;
        int s = position.length;
        for (int i = position.length - 1; i >= 0; i--) {
            if (s == numX || s == numO || s == numBlanks) {
                break;
            }
            switch (position[i]) {
                case BLUE:
                    if (numO > 0) {
                        location += rearrange(numX, numO - 1, s - 1);
                    }
                    if (numBlanks > 0) {
                        location += rearrange(numX, numO, s - 1);
                    }
                    numX -= 1;
                    break;
                case RED:
                    if (numBlanks > 0) {
                        location += rearrange(numX, numO, s - 1);
                    }
                    numO -= 1;
                    break;
                case EMPTY:
                    numBlanks -= 1;
                    break;
            }
            s -= 1;
        }
        return location;
    }

    public void solve() {
        solve(startingPosition, 0, Piece.BLUE, -1);
    }

    private Tuple<Primitive, Integer> solve(Piece[] position, int numPieces, Piece next, int m) {
        int location = calculateLocation(position, numPieces);
        Tuple<Primitive, Integer> solvedVal = getValue(location);
        if (solvedVal != null) {
            return solvedVal;
        }

        Piece placed = next.opposite();
        Tuple<Primitive, Integer> p = game.isPrimitive(position, placed, m);
        if (p.x != Primitive.NOT_PRIMITIVE) {
            try {
                raf.seek(location);
                raf.write(byteValue(p));
            } catch (Exception e) {
                throw new IllegalStateException("bad things happen");
            }
            //memo.put(location, byteValue(p));
            return p;
        }
        List<Integer> moves = game.generateMoves(position);
        ArrayList<Tuple<Primitive, Integer>> nextPositionValues = new ArrayList<>(moves.size());
        for (int move : moves) {
            Piece[] newPosition = game.doMove(position, move, next);
            nextPositionValues.add(solve(newPosition, numPieces + 1, placed, move));
        }
        int lossRemote = Integer.MAX_VALUE;
        int tieRemote = -1;
        int winRemote = -1;
        for (Tuple<Primitive, Integer> val: nextPositionValues) {
            if (val.x == Primitive.LOSS && val.y < lossRemote) {
                lossRemote = val.y;
            } else if (val.x == Primitive.TIE  && val.y > tieRemote) {
                tieRemote = val.y;
            } else if (val.y > winRemote){
                winRemote = val.y;
            }
        }
        if (lossRemote != Integer.MAX_VALUE) {
            Tuple<Primitive, Integer> temp = new Tuple<>(Primitive.WIN, lossRemote + 1);
            try {
                raf.seek(location);
                raf.write(byteValue(temp));
            } catch (Exception e) {
                throw new IllegalStateException("bad things happen");
            }
            //memo.put(location, byteValue(temp));
            return temp;
        } else if (tieRemote != -1) {
            Tuple<Primitive, Integer> temp = new Tuple<>(Primitive.TIE, tieRemote + 1);
            try {
                raf.seek(location);
                raf.write(byteValue(temp));
            } catch (Exception e) {
                throw new IllegalStateException("bad things happen");
            }
            //memo.put(location, byteValue(temp));
            return temp;
        } else {
            Tuple<Primitive, Integer> temp = new Tuple<>(Primitive.LOSS, winRemote + 1);
            try {
                raf.seek(location);
                raf.write(byteValue(temp));
            } catch (Exception e) {
                throw new IllegalStateException("bad things happen");
            }
            //memo.put(location, byteValue(temp));
            return temp;
        }
    }

    public int getSize() {
        return width*height;
    }

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


    public void play() {
        int loc = calculateLocation(startingPosition, 0);
//        if (!memo.containsKey((loc))) {
//            solve();
//        }
        Scanner input = new Scanner(System.in);
        Piece[] board = startingPosition;
        int numPieces = 0;
        Piece nextP = Piece.BLUE;
        while (true) {
            printBoard(board);
            Tuple<Primitive, Integer> prim = game.isPrimitive(board, nextP.opposite());
            if (prim.x != Primitive.NOT_PRIMITIVE) {
                if (prim.x == Primitive.TIE) {
                    System.out.println("Tie Game");

                } else {
                    switch(nextP) {
                        case BLUE:
                            System.out.println("O WINS!");
                            break;
                        case RED:
                            System.out.println("X WINS!");
                            break;
                    }
                }
                break;
            }
            loc = calculateLocation(board, numPieces);
            Tuple<Primitive, Integer> should = getValue(loc);
            System.out.println(should.x);
            if (should.x == Primitive.TIE) {
                System.out.println("Game should Tie");
            } else if (should.x == Primitive.WIN) {
                switch(nextP) {
                    case BLUE:
                        System.out.println("X should win");
                        break;
                    case RED:
                        System.out.println("O should win");
                        break;
                }
            } else {
                switch(nextP) {
                    case RED:
                        System.out.println("X should win");
                        break;
                    case BLUE:
                        System.out.println("O should win");
                        break;
                }
            }
            System.out.println("in " + should.y);
            int next;
            numPieces ++;
            if (nextP == Piece.EMPTY) {
                List<Integer> moves = game.generateMoves(board);
                Collections.shuffle(moves);
                ArrayList<Tuple<Primitive, Integer>> nextPositionValues = new ArrayList<>(moves.size());
                for (int move : moves) {
                    Piece[] newPosition = game.doMove(board, move, nextP);
                    loc = calculateLocation(newPosition, numPieces);
                    nextPositionValues.add(getValue(loc));

                }
                int lossRemote = Integer.MAX_VALUE;
                int tieRemote = -1;
                int winRemote = -1;
                Primitive curPrim = Primitive.WIN;
                int tempMove = -1;
                for (int i = 0; i < nextPositionValues.size(); i++) {
                    Tuple<Primitive, Integer> val = nextPositionValues.get(i);
                    if (val.x == Primitive.LOSS && val.y <= lossRemote) {
                        if (!(lossRemote == val.y && rand.nextBoolean())) {
                            lossRemote = val.y;
                        }
                        curPrim = Primitive.LOSS;
                        tempMove = moves.get(i);
                    } else if (curPrim != Primitive.LOSS && val.x == Primitive.TIE  && val.y >= tieRemote) {
                        if (!(tieRemote == val.y && rand.nextBoolean())) {
                            tieRemote = val.y;
                        }
                        curPrim = Primitive.TIE;
                        tempMove = moves.get(i);
                    } else if ((curPrim == Primitive.WIN) && val.y >= winRemote){
                        if (!(winRemote == val.y && rand.nextBoolean())) {
                            winRemote = val.y;
                        }
                        curPrim = Primitive.WIN;
                        tempMove = moves.get(i);
                    }
                }
                next = tempMove;
                next = (next / height) + 1;


            } else {
                next = input.nextInt();
                while (next > width || next < 1 || board[height - 1 + (next - 1)* height] != Piece.EMPTY) {
                    System.out.println("Please chose a valid location");
                    next = input.nextInt();
                }
            }

            int r = height - 1;
            while (r > 0 && board[(r - 1) + (next - 1)* height] == Piece.EMPTY) {
                r--;
            }
            board[r + (next - 1)*height] = nextP;
            nextP = nextP.opposite();
            System.out.println("-------------------------------------------------");
        }

    }



    /*
      R   R
      R R B
    B B R B
    B B B R
    */
    public void test() {
        Piece[] board = new Piece[] {Piece.RED, Piece.BLUE, Piece.EMPTY,
                Piece.BLUE, Piece.RED, Piece.BLUE,
                Piece.EMPTY, Piece.EMPTY, Piece.EMPTY,
        };
        Tuple<Primitive, Integer> should = game.isPrimitive(board, Piece.BLUE);
        System.out.println(should.x);
        System.out.println(should.y);
    }
}


