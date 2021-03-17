package Games.PieceGame.Functions;

import Games.PieceGame.Connect4.Connect4;
import Games.PieceGame.PieceGame;
import Helpers.Piece;

import Helpers.Primitive;
import Helpers.Tuple;
import org.apache.spark.api.java.function.Function;
import scala.Tuple2;

public class PrimitiveFilter implements Function<Tuple2<Long, Object>, Boolean> {

    Piece placed;
    PieceGame game;

    public PrimitiveFilter(Piece placed, PieceGame game) {

        this.placed = placed.opposite();
        this.game = game;
    }

    @Override
    public Boolean call(Tuple2<Long, Object> longTuple2) {
        Tuple<Primitive, Integer> tup = game.isPrimitive((Piece[]) longTuple2._2, placed);
        return tup.x != Primitive.NOT_PRIMITIVE;
    }
}
