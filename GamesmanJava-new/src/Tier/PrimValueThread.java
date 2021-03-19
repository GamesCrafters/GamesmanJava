package Tier;

import Games.Connect4;
import Helpers.Piece;

import Helpers.Primitive;
import Helpers.Tuple;
import org.apache.spark.api.java.function.PairFunction;
import scala.Tuple2;


public class PrimValueThread implements PairFunction<Tuple2<Long, Piece[]>, Long, Tuple<Byte, Piece[]>> {


    int w;
    int h;
    int win;
    Piece nextP;
    int tier;
    Connect4 game;

    public PrimValueThread(int w, int h, int win, Piece nextP, int tier, Connect4 game) {
        this.w = w;
        this.h = h;
        this.win = win;
        this.nextP = nextP;
        this.tier = tier;
        this.game = game;
    }

    @Override
    public Tuple2<Long, Tuple<Byte, Piece[]>> call(Tuple2<Long, Piece[]> longTuple2){
        return new Tuple2<>(longTuple2._1, new Tuple<>(game.isPrimitive(longTuple2._2, nextP).value, longTuple2._2));
    }

}
