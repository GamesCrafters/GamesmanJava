package Tier;

import Games.PieceGame.Connect4.Connect4;
import Games.PieceGame.RectanglePieceLocator;
import Helpers.Piece;
import Helpers.Primitive;
import Helpers.Tuple;
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
    RectanglePieceLocator locator;
    int tier;
    JavaPairRDD<Long, Byte> pastPrimValues;
    public FindValueThread(int w, int h, int win, Piece placed, int tier, JavaPairRDD<Long, Byte> pastPrimValues) {
        locator = new RectanglePieceLocator(w,h);
        this.w = w;
        this.h = h;
        this.win = win;
        this.game = new Connect4(w,h,win);
        this.placed = placed;
        this.tier = tier;
        this.pastPrimValues = pastPrimValues;
    }

    @Override
    public Byte call(Piece[] pieces) {
        Tuple<Primitive, Integer> p = game.isPrimitive(pieces, placed);
        if (p.x != Primitive.NOT_PRIMITIVE) {
            return Primitive.toByte(p.x, p.y);
        }
        List<Byte> childrenBytes = new ArrayList<>();
        List<Integer> moves = game.generateMoves(pieces);
        for (Integer move: moves) {
            Long childLong = locator.calculateLocation(game.doMove(pieces, move, placed), tier);
            childrenBytes.add(pastPrimValues.lookup(childLong).get(0));
        }
        int lossRemote = Integer.MAX_VALUE;
        int tieRemote = -1;
        int winRemote = -1;
        for (Byte b: childrenBytes) {
            Tuple<Primitive, Integer> val = Tuple.byteToTuple(b);
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
            return Primitive.toByte(temp.x, temp.y);
        } else if (tieRemote != -1) {
            Tuple<Primitive, Integer> temp = new Tuple<>(Primitive.TIE, tieRemote + 1);
            return Primitive.toByte(temp.x, temp.y);
        } else {
            Tuple<Primitive, Integer> temp = new Tuple<>(Primitive.LOSS, winRemote + 1);
            return Primitive.toByte(temp.x, temp.y);
        }
    }



}
