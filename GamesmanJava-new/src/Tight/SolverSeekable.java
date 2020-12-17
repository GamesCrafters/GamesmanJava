package Tight;

import Games.Connect4;
import Helpers.Piece;
import Helpers.Primitive;
import Helpers.Tuple;
import org.apache.commons.math3.util.CombinatoricsUtils;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static java.nio.file.StandardOpenOption.*;

public class SolverSeekable extends Thread {

    ParallelRunner.SharedVars sharedVars;

    public SolverSeekable(int w, int h, int wi, Piece[] starter, ParallelRunner.SharedVars sharedVars) {
        this(w, h, wi, sharedVars);
        startingPosition = starter;
        int numBlue = 0;
        int numRed = 0;
        for (Piece piece : starter) {
            if (piece == Piece.BLUE) {
                numBlue += 1;
            } else if (piece == Piece.RED) {
                numRed += 1;
            }
        }
        startingPieces = numBlue + numRed;
        if (numBlue != numRed) {
            startingPiece = Piece.RED;
        }
    }

    public void run () {
        solve();
        System.out.println("Thread" + Thread.currentThread().getId() + " is done and solved " + logs[7]);
    }



    int width;
    int height;
    int win;
    Connect4 game;
    private Piece[] startingPosition;
    long[] offsets;
    private final long[][][] savedRearrange;
    Random rand = new Random();
    int startingPieces = 0;
    Piece startingPiece = Piece.BLUE;
    String fileName;
    SeekableByteChannel channel;
    Map<Long, Byte> cache = new HashMap<>();
    long[] logs = new long[8]; // setOffsets, rearrange, calculateLocation, isPrim, generateMoves, doMoves, Fileio, numSolved
    /** Pieces stored in column major order, starting from bottom right*/
    public SolverSeekable(int w, int h, int wi, ParallelRunner.SharedVars shared) {
        width = w;
        height = h;
        win = wi;
        sharedVars = shared;
        startingPosition = new Piece[w*h];
        Arrays.fill(startingPosition, Piece.EMPTY);
        savedRearrange = new long[2 + w*h / 2][2 + w*h/2][w*h + 1];
        for (int i = 0; i < 2 + w*h / 2; i++) {
            for (int j = 0; j < 2 + w*h/2; j++) {
                for (int k = 0; k < w*h + 1; k++) {
                    savedRearrange[i][j][k] = -1;
                }
            }
        }
        game = new Connect4(width, height, win);
        setOffsets();
        fileName = "connect4_by_" + width + "_by_" + height + "_win_" + win + "_sparse";
        Path path = null;//Path.of(fileName);

        try {
            channel = Files.newByteChannel(path, EnumSet.of(CREATE_NEW, WRITE, SPARSE, READ));
        } catch (Exception e) {
            try {
                channel = Files.newByteChannel(path, EnumSet.of(WRITE, READ));
            } catch (Exception e1) {
                throw new IllegalStateException("File opening went funky");
            }

        }

    }

    private void setOffsets() {
        long temp = System.currentTimeMillis();
        offsets = new long[width*height +1];
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
        logs[0] += System.currentTimeMillis() - temp;
    }

    private long rearrange(int x, int o, int s) {
        long temp = System.currentTimeMillis();
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

        long temper = Math.round(ret);
        savedRearrange[x][o][s] = temper;

        logs[1] += System.currentTimeMillis() - temp;

        return temper;

    }

    private Tuple<Primitive, Integer> getValue(long location) {
        long temp = System.currentTimeMillis();
        byte b;
        if (cache.containsKey(location)) {
            b = cache.get(location);
        } else {
            if (sharedVars.maxLocationWritten < location) {
                return null;
            }
            ByteBuffer buf = ByteBuffer.allocate(1);
            try {
                channel.position(location);
                channel.read(buf);
                buf.position(0);
                b = buf.get();
                logs[6] += System.currentTimeMillis() - temp;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        if (b == 0) {
            return null;
        }
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
                throw new IllegalStateException("shouldn't happen");
        }
        temp = temp << 6;
        temp += p.y;
        return temp.byteValue();
    }

    private long calculateLocationSym(Piece[] position, int numPieces) {
        long temp = System.currentTimeMillis();
        long location = offsets[numPieces];
        int numX = (numPieces / 2) + (numPieces % 2);
        int numO = numPieces / 2;
        int numBlanks = position.length - numPieces;
        int s = position.length;
        for (int c = 0; c < width; c ++) {
            for (int h = height - 1; h >= 0; h--) {
                int i = c*height + h;
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
        }
        logs[2] += System.currentTimeMillis() - temp;
        return location;
    }

    private long calculateLocation(Piece[] position, int numPieces) {
        long temp = System.currentTimeMillis();
        long location = offsets[numPieces];
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
        logs[2] += System.currentTimeMillis() - temp;
        return Math.min(location, calculateLocationSym(position, numPieces));
    }

    private void writeToFile(long location, Byte b, boolean forceWrite) {
        if (location != -1) {
            if (cache.containsKey(location)) {
                throw new IllegalStateException("Errors here");
            }
            cache.put(location, b);
        }
        if (cache.size() > 34888367 / 8 || forceWrite) { //Size of 5x5 34888367
            System.out.println("Thread " + Thread.currentThread().getId()+ " Writing Back");
            try {
                long t = System.currentTimeMillis();
                ArrayList<Long> locations = new ArrayList<>(cache.keySet());
                Collections.sort(locations);
                if (locations.size() != 0 ) {
                    sharedVars.maxLocationWritten = Math.max(locations.get(locations.size() - 1), sharedVars.maxLocationWritten);
                }
                ByteBuffer buf = ByteBuffer.allocate(1);
                for (Long loc : locations) {
                    channel.position(loc);
                    buf.position(0);
                    Byte temp = cache.get(loc);
                    buf.put(temp);
                    buf.position(0);
                    channel.write(buf);
                    sharedVars.solving.remove(loc);
                }
                logs[6] += System.currentTimeMillis() - t;
                cache.clear();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
     }

    public void solve() {
        solve(startingPosition, startingPieces, startingPiece, -1);
        writeToFile(-1, (byte) -1, true);
    }

    private Tuple<Primitive, Integer> solve(Piece[] position, int numPieces, Piece next, int m) {
        long location = calculateLocation(position, numPieces);
        Tuple<Primitive, Integer> solvedVal = getValue(location);
        if (solvedVal != null) {
            return solvedVal;
        }
        if (sharedVars.solving.contains(location)) {
            return null;
        }
        sharedVars.solving.add(location);
        logs[7] += 1;
        Piece placed = next.opposite();
        long t = System.currentTimeMillis();
        Tuple<Primitive, Integer> p = game.isPrimitive(position, placed, m);
        logs[3] += System.currentTimeMillis() - t;
        if (p.x != Primitive.NOT_PRIMITIVE) {
            writeToFile(location, byteValue(p), false);
            return p;
        }
        t = System.currentTimeMillis();
        List<Integer> moves = game.generateMoves(position);
        logs[4] += System.currentTimeMillis() - t;
        ArrayList<Tuple<Primitive, Integer>> nextPositionValues = new ArrayList<>(moves.size());
        for (int move : moves) {
            t = System.currentTimeMillis();
            Piece[] newPosition = game.doMove(position, move, next);
            logs[5] += System.currentTimeMillis() - t;
            nextPositionValues.add(solve(newPosition, numPieces + 1, placed, move));
        }
        int lossRemote = Integer.MAX_VALUE;
        int tieRemote = -1;
        int winRemote = -1;
        for (Tuple<Primitive, Integer> val: nextPositionValues) {
            if (val == null) {
                sharedVars.solving.remove(location);
                return null;
            }
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
            writeToFile(location, byteValue(temp), false);
            return temp;
        } else if (tieRemote != -1) {
            Tuple<Primitive, Integer> temp = new Tuple<>(Primitive.TIE, tieRemote + 1);
            writeToFile(location, byteValue(temp), false);
            return temp;
        } else {
            Tuple<Primitive, Integer> temp = new Tuple<>(Primitive.LOSS, winRemote + 1);
            writeToFile(location, byteValue(temp), false);
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
        long loc = calculateLocation(startingPosition, 0);
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
    // setOffsets, rearrange, calculateLocation, isPrim, generateMoves, doMoves. fileIO
    private void printLogs() {
        System.out.printf("setOffsets took %s%n", (double) logs[0] / 1000);
        System.out.printf("rearrange took %s%n", (double) logs[1] / 1000);
        System.out.printf("calculateLocation took %s%n",(double) logs[2] / 1000);
        System.out.printf("isPrim took %s%n", (double) logs[3] / 1000);
        System.out.printf("generateMoves took %s%n", (double) logs[4] / 1000);
        System.out.printf("doMoves took %s%n", (double) logs[5] / 1000);
        System.out.printf("fileIO took %s%n", (double) logs[6] / 1000);
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

    public List<Piece[]> findNStartingPoints(int n) {
        ArrayList<Piece[]> ret = new ArrayList<>();
        if (n <= 0) {
            return ret;
            //throw new IllegalArgumentException("Must have at least 1 starting position");
        }
        if (n == 1) {
            ret.add(startingPosition);
            return ret;
        }
        List<Integer> lst = game.generateMoves(startingPosition);
        for (int i = 0; i < lst.size(); i++) {
            ret.add(game.doMove(startingPosition, lst.get(i), Piece.BLUE));
        }
        return ret;
    }
}


