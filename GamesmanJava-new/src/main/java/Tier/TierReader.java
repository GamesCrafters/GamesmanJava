package Tier;

import Games.PieceGame.Connect4.Connect4;
import Games.PieceGame.PieceGame;
import Games.PieceGame.RectanglePieceLocator;
import Games.PieceGame.TicTacToe.TicTacToe;
import Helpers.Piece;
import Helpers.Primitive;
import Helpers.Tuple;
import org.apache.commons.lang.ArrayUtils;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.*;
import java.util.stream.Stream;

public class TierReader {

    int h;
    int w;
    int win;
    RectanglePieceLocator locator;
    File folder;
    boolean comp1;
    boolean comp2;
    PieceGame game;

    static HashMap<String, Class<?>> games = new HashMap<>();

    public static void main (String[] args) {
        //add games here
        games.put("Connect_4", Connect4.class);
        games.put("Tic_Tac_Toe", TicTacToe.class);

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
        boolean comp1;
        boolean comp2;
        System.out.println("Player 1 computer? (y/n)");
        while (true) {
            String next = scanner.next();
            if (next.equalsIgnoreCase("y")) {
                comp1 = true;
                break;
            }
            if (next.equalsIgnoreCase("n")) {
                comp1 = false;
                break;
            }
            System.out.println("Enter 'y' or 'n'");
        }

        System.out.println("Player 2 computer? (y/n)");
        while (true) {
            String next = scanner.next();
            if (next.equalsIgnoreCase("y")) {
                comp2 = true;
                break;
            }
            if (next.equalsIgnoreCase("n")) {
                comp2 = false;
                break;
            }
            System.out.println("Enter 'y' or 'n'");
        }
        TierReader reader = new TierReader(solves[choice], comp1, comp2);
        reader.play();
    }

    TierReader(File folder, boolean comp1, boolean comp2) {
        if (!folder.exists()) {
            throw new IllegalStateException("Error reading data");
        }
        String name = folder.getName();

        this.folder = folder;
        this.comp1 = comp1;
        this.comp2 = comp2;
        w = Integer.parseInt(String.valueOf(folder.getName().charAt(10)));
        h = Integer.parseInt(String.valueOf(folder.getName().charAt(14)));
        win = Integer.parseInt(String.valueOf(folder.getName().charAt(23)));
        locator = new RectanglePieceLocator(w, h);

        for (String gameName : games.keySet()) {
            if (name.startsWith(gameName)) {
                try {
                    game = (PieceGame) games.get(gameName)
                            .getConstructor(new Class[]{int.class,int.class,int.class})
                            .newInstance(w, h, win);
                } catch (Exception ex) {
                    throw new IllegalStateException("Invalid game name");
                }
                break;
            }
        }
    }

    private void play () {
        Scanner scanner = new Scanner(System.in);
        Piece[] board = new Piece[w*h];
        Arrays.fill(board, Piece.EMPTY);

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
            int move;
            boolean comp = (nextp == Piece.BLUE ? this.comp1 : this.comp2);
            if (!comp) {
                System.out.print("Move: ");
                move = makePersonMove(board);
            } else {
                move = makeComputerMove(board, game, nextp, tier);
            }
            board = game.doMove(board, move, nextp);
            nextp = nextp.opposite();
            tier += 1;
            if (this.comp1 && this.comp2) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }

    }

    private int makeComputerMove(Piece[] board, PieceGame game, Piece nextp, int tier) {
        Primitive bestResult = Primitive.WIN;
        int number = 0;
        int bestMove = 0;
        List<Integer> l = game.generateMoves(board);
        for (Integer move : l) {
            Piece[] newBoard = game.doMove(board, move, nextp);
            Tuple<Primitive, Integer> value = getValue(newBoard, tier + 1);
            boolean update = false;
            if (bestResult == Primitive.WIN) {
                update = (value.x == Primitive.LOSS || value.x == Primitive.TIE || (value.x == Primitive.WIN && number < value.y));
            } else if (bestResult == Primitive.LOSS) {
                update = (value.x == Primitive.LOSS && number > value.y);
            } else if (bestResult == Primitive.TIE) {
                update = (value.x == Primitive.LOSS);
            } else {
                System.out.println("Primitive not found");
            }
            if (update) {
                bestResult = value.x;
                number = value.y;
                bestMove = move;
            }
        }
        return bestMove;
    }

    private int makePersonMove(Piece[] board) {
        Scanner scanner = new Scanner(System.in);
        int move;
        while (true) {
            try  {
                move = Integer.parseInt(scanner.next());
                if (board[(h * move) - 1] != Piece.EMPTY) {
                    System.out.println("Cannot add to full column");
                } else {
                    break;
                }
            } catch (NumberFormatException ignored) {
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
        return actual_move;
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


        long loc = locator.calculateLocation(board, tier);
        byte b = 0;

        //Loop through part files till b is not nonzero, return value. If did not get a value through all parts, throw exception
        for (File f: Objects.requireNonNull(data.listFiles())) {
            RandomAccessFile raf;
            try {
                raf = new RandomAccessFile(f, "r");
            } catch (Exception e) {
                throw new IllegalStateException("Cannot find data for tier " + tier);
            }
            try {
                raf.seek(loc);
                b = (byte) raf.readUnsignedByte();
                if (b != 0) {
                    break;
                }
            } catch (Exception e) {
                if (ArrayUtils.indexOf(data.listFiles(), f) == Objects.requireNonNull(data.listFiles()).length - 1) {
                    throw new IllegalStateException("Cannot find data for tier " + tier + " at loc " + loc);
                }
            }
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
