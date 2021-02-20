package Tier;

import Games.Connect4;
import Helpers.Piece;
import Helpers.Primitive;
import Helpers.Tuple;
import org.apache.commons.math3.util.CombinatoricsUtils;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ParentFunc implements PairFlatMapFunction<Tuple2<Long, Tuple<Byte, Piece[]>>, Long, Tuple<Byte, Piece[]>> {

    boolean childPrim;
    int width;
    int height;
    int win;
    Piece placed;
    Connect4 game;
    int tier;
    long[] offsets;
    private long[][][] savedRearrange;

    public ParentFunc(int w, int h, int win, Piece nextP, boolean isPrimitive, Connect4 game, int tier) {
        width = w;
        height = h;
        this.win = win;
        placed = nextP;
        childPrim = isPrimitive;
        this.game = game;
        savedRearrange = new long[2 + w*h / 2][2 + w*h/2][w*h + 1];
        for (int i = 0; i < 2 + w*h / 2; i++) {
            for (int j = 0; j < 2 + w*h/2; j++) {
                for (int k = 0; k < w*h + 1; k++) {
                    savedRearrange[i][j][k] = -1;
                }
            }
        }
        this.tier = tier;
        setOffsets();

    }

    @Override
    public Iterator<Tuple2<Long, Tuple<Byte, Piece[]>>> call(Tuple2<Long, Tuple<Byte, Piece[]>> longTuple2) {
        List<Tuple2<Long, Tuple<Byte, Piece[]>>> retList;
        Tuple<Primitive, Integer> oldVal = toTuple(longTuple2._2().x);
        Primitive newP;
        switch (oldVal.x) {
            case TIE:
                newP = Primitive.TIE;
                break;
            case WIN:
                newP = Primitive.LOSS;
                break;
            case LOSS:
                newP = Primitive.WIN;
                break;
            default:
                throw new IllegalStateException("At this point all values should be set");
        }
        Integer newRemote = oldVal.y + 1;
        Byte b = toByte(newP, newRemote);
        if (childPrim) {
            retList = parentsWL(longTuple2._2().y, b);
        } else {
            retList = parentsNotWL(longTuple2._2().y, b);
        }

        return retList.iterator();
    }

    private byte toByte(Primitive p, Integer i) {
        Integer temp;
        switch (p) {
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
        temp += i;
        return temp.byteValue();
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

    private List<Tuple2<Long, Tuple<Byte, Piece[]>>> parentsWL(Piece[] pos, Byte val) {
        List<Tuple2<Long, Tuple<Byte, Piece[]>>> retList = new ArrayList<>();
        for (int i = height - 1; i < width * height; i += height) {
            for (int j = i; j != i - height; j --) {
                Piece atJ = pos[j];
                if (atJ == Piece.EMPTY) {
                    continue;
                }
                if (atJ == placed) {
                    Piece[] newpos = pos.clone();
                    newpos[j] = Piece.EMPTY;
                    if (game.isPrimitive(newpos, placed.opposite()).x == Primitive.NOT_PRIMITIVE) {
                        retList.add(new Tuple2<>(calculateLocation(newpos, tier), new Tuple<>(val, newpos)));
                    }
                } else {
                    break;
                }
            }
        }
        return retList;
    }

    private List<Tuple2<Long, Tuple<Byte, Piece[]>>> parentsNotWL(Piece[] pos, Byte val) {
        List<Tuple2<Long, Tuple<Byte, Piece[]>>> retList = new ArrayList<>();
        for (int i = height - 1; i < width * height; i += height) {
            for (int j = i; j != i - height; j --) {
                Piece atJ = pos[j];
                if (atJ == Piece.EMPTY) {
                    continue;
                }
                if (atJ == placed) {
                    Piece[] newpos = pos.clone();
                    newpos[j] = Piece.EMPTY;
                    retList.add(new Tuple2<>(calculateLocation(newpos, tier), new Tuple<>(val, newpos)));
                } else {
                    break;
                }
            }
        }
        return retList;
    }

    private void setOffsets() {
        Piece[] startingPosition = new Piece[width*height];
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
        for (int c = 0; c < width; c ++) {
            for (int he = height - 1; he >= 0; he--) {
                int i = c*height + he;
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
