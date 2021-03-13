package Tier;

import Games.Connect4;
import Helpers.LocationCalc;
import Helpers.Piece;
import Helpers.Primitive;
import Helpers.Tuple;
import org.apache.commons.math3.util.CombinatoricsUtils;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.Arrays;
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

    LocationCalc locator;

    public ParentFunc(int w, int h, int win, Piece nextP, boolean isPrimitive, Connect4 game, int tier) {
        locator = new LocationCalc(w, h);
        width = w;
        height = h;
        this.win = win;
        placed = nextP;
        childPrim = isPrimitive;
        this.game = game;
        this.tier = tier;
    }

    @Override
    public Iterator<Tuple2<Long, Tuple<Byte, Piece[]>>> call(Tuple2<Long, Tuple<Byte, Piece[]>> longTuple2) {
        List<Tuple2<Long, Tuple<Byte, Piece[]>>> retList;
        Tuple<Primitive, Integer> oldVal = Tuple.byteToTuple(longTuple2._2().x);
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
        Byte b = Primitive.toByte(newP, newRemote);
        if (childPrim) {
            retList = parentsWL(longTuple2._2().y, b);
        } else {
            retList = parentsNotWL(longTuple2._2().y, b);
        }

        return retList.iterator();
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
                        retList.add(new Tuple2<>(locator.calculateLocation(newpos, tier), new Tuple<>(val, newpos)));
                    }
                }
                break;
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
                    retList.add(new Tuple2<>(locator.calculateLocation(newpos, tier), new Tuple<>(val, newpos)));
                } else {
                    break;
                }
            }
        }
        return retList;
    }


}
