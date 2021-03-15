package Tier;

import Games.PieceGame.Connect4.Connect4;
import Games.PieceGame.RectanglePieceLocator;
import Helpers.Piece;
import Helpers.Primitive;
import Helpers.Tuple;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Objects;
import java.util.Scanner;

public class TierReader {

    int h;
    int w;
    int win;
    RectanglePieceLocator locator;
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
        locator = new RectanglePieceLocator(w, h);
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
            Piece.printBoard(board, w, h);
            Tuple<Primitive, Integer> value = getValue(board, tier);
            System.out.printf("%s in %s%n", value.x, value.y);
            if (value.y == 0) {
                System.out.println("Game OVER!!!!");
                break;
            }
            System.out.print("Move: ");
            int move;
            while (true) {
                try  {
                    move = Integer.parseInt(scanner.next());
                    if (board[(h * move) - 1] != Piece.EMPTY) {
                        System.out.println("Cannot add to full column");
                    } else {
                        break;
                    }
                } catch (Exception ignored) {
                    System.out.println("Invalid move");
                    System.out.print("Move: ");
                }

            }
            int actual_move = (h * (move - 1));
            for (int i = (h * move) - 1; i != (h * (move - 1)) - 1; i --) {
                if (board[i] != Piece.EMPTY) {
                    actual_move = i + 1;
                    break;
                }
            }
            board = game.doMove(board, actual_move, nextp);
            nextp = nextp.opposite();
            tier += 1;
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

        long loc = locator.calculateLocation(board, tier);
        byte b;
        try {
            raf.seek(loc);
            b = (byte) raf.readUnsignedByte();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot find data for tier " + tier + " at loc " + loc);
        }
        return toTuple(b);
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








}
