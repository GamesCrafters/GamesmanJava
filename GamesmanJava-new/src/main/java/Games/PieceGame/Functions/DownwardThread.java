package Games.PieceGame.Functions;

import Games.Interfaces.Locator;
import Games.PieceGame.PieceGame;
import Helpers.Piece;

import Helpers.Primitive;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import scala.Tuple2;

import java.util.*;


public class DownwardThread implements PairFlatMapFunction<Tuple2<Long, Piece[]>, Long, Piece[]> {


    Piece nextP;
    int tier;
    PieceGame game;
    Locator locator;

    public DownwardThread(Piece nextP, int tier, PieceGame game, Locator locator) {
        this.nextP = nextP;
        this.tier = tier;
        this.game = game;
        this.locator = locator;
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
