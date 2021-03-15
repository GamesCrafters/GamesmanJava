package Tier;

import Games.Connect4.Connect4;
import Helpers.Piece;

import Helpers.Primitive;
import Helpers.Tuple;
import org.apache.spark.api.java.function.Function;
import scala.Tuple2;

public class PrimitiveFilter implements Function<Tuple2<Long, Piece[]>, Boolean> {
    int width;
    int height;
    int win;
    Piece placed;
    Connect4 game;

    public PrimitiveFilter(int w, int h, int win, Piece placed, Connect4 game) {
        width = w;
        height = h;
        this.win = win;
        this.placed = placed;
        this.game = game;
    }

    @Override
    public Boolean call(Tuple2<Long, Piece[]> longTuple2) {
        Tuple<Primitive, Integer> tup = game.isPrimitive(longTuple2._2, placed);
        return tup.x != Primitive.NOT_PRIMITIVE;
    }
}
