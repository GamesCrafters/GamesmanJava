package Tier;

import Games.Connect4;
import Helpers.Piece;
import Helpers.Primitive;
import Helpers.Tuple;
import org.apache.commons.math3.util.CombinatoricsUtils;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Objects;
import java.util.Scanner;

public class TierReader {

    int h;
    int w;
    int win;
    long[] offsets;
    private final long[][][] savedRearrange;
    File folder;
    boolean comp;

    public static void main (String[] args) {
        File folder = new File("SPARK_OUT");
        File[] solves = folder.listFiles();
        Scanner scanner = new Scanner(System.in);
        if (solves == null) {
            return;
        }
        System.out.println("Choose file to play with:");
        for (int i = 0; i < solves.length; i ++) {
            System.out.printf("%-8d%s%n", i, solves[i].getName());
        }
        int choice;
        while (true) {
            if (!scanner.hasNextInt()) {
                scanner.next();
                System.out.println("Enter a valid int");
                continue;
            }
            choice = scanner.nextInt();
            if (choice < 0 || choice >= solves.length) {
                System.out.println("Enter a valid choice");
            } else {
                break;
            }
        }
        boolean computer;
        System.out.println("Play vs computer? (y/n)");
        while (true) {
            String next = scanner.next();
            if (next.equalsIgnoreCase("y")) {
                computer = true;
                break;
            }
            if (next.equalsIgnoreCase("n")) {
                computer = false;
                break;
            }
            System.out.println("Enter 'y' or 'n'");

        }
        TierReader reader = new TierReader(solves[choice], computer);
        reader.play();
    }

    TierReader(File folder, boolean comp) {
        if (!folder.exists()) {
            throw new IllegalStateException("Error reading data");
        }
        String name = folder.getName();

        w = Integer.parseInt(String.valueOf(name.charAt(0)));
        h = Integer.parseInt(String.valueOf(name.charAt(3)));
        win = Integer.parseInt(String.valueOf(name.charAt(7)));

        this.folder = folder;
        this.comp = comp;
        savedRearrange = new long[2 + w*h / 2][2 + w*h/2][w*h + 1];
        for (int i = 0; i < 2 + w*h / 2; i++) {
            for (int j = 0; j < 2 + w*h/2; j++) {
                for (int k = 0; k < w*h + 1; k++) {
                    savedRearrange[i][j][k] = -1;
                }
            }
        }
        setOffsets();
    }

    private void play () {
        Scanner scanner = new Scanner(System.in);
        Piece[] board = new Piece[w*h];
        Arrays.fill(board, Piece.EMPTY);
        Connect4 game = new Connect4(w, h, win);
        int tier = 0;
        Piece nextp = Piece.BLUE;
        while (true) {
            System.out.println("____________________________________________________");
            printBoard(board);
            Tuple<Primitive, Integer> value = getValue(board, tier);
            System.out.printf("%s in %s%n", value.x, value.y);
            System.out.print("Move: ");
            int move;
            while (true) {
                try  {
                    move = Integer.parseInt(scanner.next());
                    break;
                } catch (NumberFormatException ignored) {
                    System.out.println("Invalid move");
                    System.out.print("Move: ");
                }
            }
            board = game.doMove(board, move - 1, nextp);
            nextp = nextp.opposite();
        }

    }

    private Tuple<Primitive, Integer> getValue(Piece[] board, int tier) {
        File data = null;
        for (File f: Objects.requireNonNull(folder.listFiles())) {
            if (f.getName().equals("tier_" + tier)) {
                data = f;
                break;
            }
        }
        if (data == null) {
            throw new IllegalStateException("Cannot find data for tier " + tier);
        }

        RandomAccessFile raf;
        try {
            raf = new RandomAccessFile(data, "r");
        } catch (Exception e) {
            throw new IllegalStateException("Cannot find data for tier " + tier);
        }

        long loc = calculateLocation(board, tier);
        byte b;
        try {
            raf.seek(loc);
            b = (byte) raf.readUnsignedByte();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot find data for tier " + tier + " at loc " + loc);
        }
        return toTuple(b);
    }

    private void printBoard(Piece[] board) {
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

    private Tuple<Primitive, Integer> toTuple(Byte b) {
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
                throw new IllegalStateException("Should only have these options");
        }
        return new Tuple<>(p, remoteness);
    }

    private long rearrange(int x, int o, int s) {
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

        return temper;

    }

    private void setOffsets() {
        Piece[] startingPosition = new Piece[w*h];
        offsets = new long[w*h +1];
        offsets[0] = 1;
        for (int i = 1; i < offsets.length; i++) {
            if (i % 2 == 0) {
                offsets[i] = offsets[i-1] +  rearrange(i/2, i/2, startingPosition.length);
            } else {
                offsets[i] = offsets[i-1] + rearrange((i/2) + 1, i/2, startingPosition.length);
            }
        }
        System.arraycopy(offsets, 0, offsets, 1, offsets.length - 1);
        offsets[0] = 0;

    }

    private long calculateLocationSym(Piece[] position, int numPieces) {
        long location = offsets[numPieces];
        int numX = (numPieces / 2) + (numPieces % 2);
        int numO = numPieces / 2;
        int numBlanks = position.length - numPieces;
        int s = position.length;
        for (int c = 0; c < w; c ++) {
            for (int he = h - 1; he >= 0; he--) {
                int i = c*h + he;
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
        return location;
    }

    private long calculateLocation(Piece[] position, int numPieces) {
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
        return Math.min(location, calculateLocationSym(position, numPieces));
    }
}
