package Games.PieceGame.Functions;

import Games.PieceGame.Connect4.Connect4;
import Games.PieceGame.PieceGame;
import Helpers.Piece;

import Helpers.Primitive;
import Helpers.Tuple;
import org.apache.spark.api.java.function.PairFunction;
import scala.Tuple2;


public class PrimValueThread implements PairFunction<Tuple2<Long, Object>, Long, Tuple<Byte,Object>> {



    Piece nextP;
    PieceGame game;

    public PrimValueThread(Piece nextP, PieceGame game) {
        this.nextP = nextP;
        this.game = game;
    }

    @Override
    public Tuple2<Long, Tuple<Byte, Object>> call(Tuple2<Long, Object> longTuple2){
        Tuple<Primitive, Integer> temp = game.isPrimitive((Piece[]) longTuple2._2, nextP);
        return new Tuple2<>(longTuple2._1, new Tuple<>(Primitive.toByte(temp.x, temp.y), longTuple2._2));
    }

}
