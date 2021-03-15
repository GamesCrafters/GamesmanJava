package Tier;

import Games.PieceGame.Connect4.Connect4;
import Games.PieceGame.RectanglePieceLocator;
import Helpers.Piece;

import Helpers.Primitive;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import scala.Tuple2;

import java.util.*;


public class DownwardThread implements PairFlatMapFunction<Tuple2<Long, Piece[]>, Long, Piece[]> {


    int w;
    int h;
    int win;
    Piece nextP;
    int tier;
    Connect4 game;
    RectanglePieceLocator locator;

    public DownwardThread(int w, int h, int win, Piece nextP, int tier) {
        this.locator = new RectanglePieceLocator(w, h);
        this.w = w;
        this.h = h;
        this.win = win;
        this.nextP = nextP;
        this.tier = tier;
        game = new Connect4(w, h, win);

    }

    @Override
    public Iterator<Tuple2<Long, Piece[]>> call(Tuple2<Long, Piece[]> longTuple2){
        if (game.isPrimitive(longTuple2._2(), nextP.opposite()).x != Primitive.NOT_PRIMITIVE) {
            return Collections.emptyIterator();
        }
        List<Tuple2<Long, Piece[]>> nextTier = new ArrayList<>();
        List<Integer> moves = game.generateMoves(longTuple2._2);
        for (int move: moves) {
            Piece[] newPosition = game.doMove(longTuple2._2, move, nextP);
            nextTier.add(new Tuple2<>(locator.calculateLocation(newPosition, tier),newPosition));
        }
        return nextTier.iterator();
    }


}
