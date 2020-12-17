package Tight;

import Helpers.Piece;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ParallelRunner {

    public static void main(String[] args) {
        int w = 4;
        int h = 4;
        int win = 4;
        long start = System.currentTimeMillis();
        SharedVars sharedVars = new SharedVars();
        int n = Runtime.getRuntime().availableProcessors();
        n = 2;
        System.out.println(n + " Threads");
        SolverSeekable topLevel = new SolverSeekable(w, h, win, sharedVars);
        List<Piece[]> starters = topLevel.findNStartingPoints(n);
        ArrayList<SolverSeekable> threads = new ArrayList<>(starters.size());
        for (Piece[] starter : starters) {
            SolverSeekable s = new SolverSeekable(w, h, win, starter, sharedVars);
            threads.add(s);
            s.start();
        }
        for (SolverSeekable thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException ignored) {}
        }
        System.out.println("Threads Time taken : " + (((double) System.currentTimeMillis() - start) / 1000));
        sharedVars.solving.clear();
        topLevel.solve();
        System.out.println("Total Time taken : " + (((double) System.currentTimeMillis() - start) / 1000));
        topLevel.play();
    }

    public static class SharedVars {
        public volatile HashSet<Long> solving = new HashSet<>();
        public volatile long maxLocationWritten = -1;
    }

}
