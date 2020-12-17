package Tier;

import Games.Connect4;
import Helpers.Piece;
import Helpers.Primitive;
import Helpers.Tuple;
import org.apache.commons.math3.util.CombinatoricsUtils;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.function.Function;

import java.util.ArrayList;
import java.util.List;

public class FindValueThread implements Function<Piece[], Byte> {
    int w;
    int h;
    int win;
    Connect4 game;
    Piece placed;
    long[][][] savedRearrange;
    long[] offsets;
    int tier;
    JavaPairRDD<Long, Byte> pastPrimValues;
    public FindValueThread(int w, int h, int win, Piece placed, int tier, JavaPairRDD<Long, Byte> pastPrimValues) {
        this.w = w;
        this.h = h;
        this.win = win;
        this.game = new Connect4(w,h,win);
        this.placed = placed;
        this.tier = tier;
        this.pastPrimValues = pastPrimValues;
        savedRearrange = new long[2 + w*h / 2][2 + w*h/2][w*h + 1];
        for (int i = 0; i < 2 + w*h / 2; i++) {
            for (int j = 0; j < 2 + w*h/2; j++) {
                for (int k = 0; k < w*h + 1; k++) {
                    savedRearrange[i][j][k] = -1;
                }
            }
        }
        game = new Connect4(w, h, win);
        setOffsets();
    }

    @Override
    public Byte call(Piece[] pieces) {
        Tuple<Primitive, Integer> p = game.isPrimitive(pieces, placed);
        if (p.x != Primitive.NOT_PRIMITIVE) {
            return byteValue(p);
        }
        List<Byte> childrenBytes = new ArrayList<>();
        List<Integer> moves = game.generateMoves(pieces);
        for (Integer move: moves) {
            Long childLong = calculateLocation(game.doMove(pieces, move, placed), tier);
            childrenBytes.add(pastPrimValues.lookup(childLong).get(0));
        }
        int lossRemote = Integer.MAX_VALUE;
        int tieRemote = -1;
        int winRemote = -1;
        for (Byte b: childrenBytes) {
            Tuple<Primitive, Integer> val = byteToTuple(b);
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
            return byteValue(temp);
        } else if (tieRemote != -1) {
            Tuple<Primitive, Integer> temp = new Tuple<>(Primitive.TIE, tieRemote + 1);
            return byteValue(temp);
        } else {
            Tuple<Primitive, Integer> temp = new Tuple<>(Primitive.LOSS, winRemote + 1);
            return byteValue(temp);
        }
    }

    private Tuple<Primitive, Integer> byteToTuple(Byte b) {
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
