package Games.PieceGame.Connect4;

import Games.Interfaces.Locator;
import Games.PieceGame.Connect4.Connect4;
import Games.PieceGame.PieceGame;
import Games.PieceGame.RectanglePieceLocator;
import Helpers.Piece;
import Helpers.Primitive;
import Helpers.Tuple;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ParentFunc implements PairFlatMapFunction<Tuple2<Long, Tuple<Byte, Object>>, Long, Tuple<Byte, Object>> {

    boolean childPrim;
    int width;
    int height;
    Piece placed;
    Connect4 game;
    int tier;
    Locator locator;

    public ParentFunc(PieceGame g) {
        this.game = (Connect4) g;
        placed = game.getPiece().opposite();
        childPrim = true;
        this.tier = game.getTier();
        width = game.width;
        height = game.height;
        locator = game.getLocator();
    }

    @Override
    public Iterator<Tuple2<Long, Tuple<Byte, Object>>> call(Tuple2<Long, Tuple<Byte, Object>> longTuple2) {
        List<Tuple2<Long, Tuple<Byte, Object>>> retList;
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
            retList = parentsWL((Piece[]) longTuple2._2().y, b);
        } else {
            retList = parentsNotWL((Piece[]) longTuple2._2().y, b);
        }

        return retList.iterator();
    }


    private List<Tuple2<Long, Tuple<Byte, Object>>> parentsWL(Piece[] pos, Byte val) {
        List<Tuple2<Long, Tuple<Byte, Object>>> retList = new ArrayList<>();
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

    private List<Tuple2<Long, Tuple<Byte, Object>>> parentsNotWL(Piece[] pos, Byte val) {
        List<Tuple2<Long, Tuple<Byte, Object>>> retList = new ArrayList<>();
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
