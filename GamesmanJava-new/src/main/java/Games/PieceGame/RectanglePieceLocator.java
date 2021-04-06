package Games.PieceGame;

import Helpers.Piece;
import org.apache.commons.math3.util.CombinatoricsUtils;

import java.io.Serializable;

public class RectanglePieceLocator implements Games.Interfaces.Locator, Serializable {

    private final long[] offsets;
    private final long[][][] savedRearrange;
    private final int w;
    private final int h;

    public RectanglePieceLocator(int w, int h) {
        this.w = w;
        this.h = h;
        savedRearrange = new long[2 + w*h / 2][2 + w*h/2][w*h + 1];
        for (int i = 0; i < 2 + w*h / 2; i++) {
            for (int j = 0; j < 2 + w*h/2; j++) {
                for (int k = 0; k < w*h + 1; k++) {
                    savedRearrange[i][j][k] = -1;
                }
            }
        }

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

    @Override
    public long calculateLocation(Object position, int numPieces) {
        return calculateLocation((Piece[]) position, numPieces);
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
}
