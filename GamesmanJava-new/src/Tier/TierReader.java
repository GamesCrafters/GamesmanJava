package Tier;

import Helpers.Piece;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;

public class TierReader {

    int height;
    int width;
    int win;
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
            if (next.toLowerCase().equals("y")) {
                computer = true;
                break;
            }
        }
        TierReader reader = new TierReader(solves[choice], computer);
        reader.play();
    }

    TierReader(File folder, boolean comp) {
        if (!folder.exists()) {
            throw new IllegalStateException("Error reading data");
        }
        String name = folder.getName();
        width = name.charAt(0);
        height = name.charAt(3);
        win = name.charAt(7);
        this.folder = folder;
        this.comp = comp;
    }

    private void play () {
        Piece[] board = new Piece[width*height];
        Arrays.fill(board, Piece.EMPTY);

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
}
