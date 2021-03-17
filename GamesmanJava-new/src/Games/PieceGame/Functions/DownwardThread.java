package Games.PieceGame.Functions;

import Games.Interfaces.Locator;
import Games.PieceGame.PieceGame;
import Helpers.Piece;

import Helpers.Primitive;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import scala.Tuple2;

import java.util.*;


public class DownwardThread implements PairFlatMapFunction<Tuple2<Long, Object>, Long, Object> {


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
    public Iterator<Tuple2<Long, Object>> call(Tuple2<Long, Object> longTuple2){
        if (game.isPrimitive((Piece[]) longTuple2._2(), nextP.opposite()).x != Primitive.NOT_PRIMITIVE) {
            return Collections.emptyIterator();
        }
        List<Tuple2<Long, Object>> nextTier = new ArrayList<>();
        List<Integer> moves = game.generateMoves((Piece[]) longTuple2._2);
        for (int move: moves) {
            Piece[] newPosition = game.doMove((Piece[]) longTuple2._2, move, nextP);
            nextTier.add(new Tuple2<>(locator.calculateLocation(newPosition, tier),newPosition));
        }
        return nextTier.iterator();
    }


}
