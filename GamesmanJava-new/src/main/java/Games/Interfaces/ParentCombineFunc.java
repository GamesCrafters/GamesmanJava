package Games.Interfaces;

import Helpers.Tuple;
import org.apache.spark.api.java.function.Function2;

public class ParentCombineFunc implements Function2<Tuple<Byte, Object>, Tuple<Byte, Object>, Tuple<Byte, Object>> {


    @Override
    public Tuple<Byte, Object> call(Tuple<Byte, Object> byteTuple, Tuple<Byte, Object> byteTuple2){
        return new Tuple<>(call(byteTuple.x, byteTuple2.x), byteTuple.y);
    }
    public Byte call(Byte b1, Byte b2) {
        return b1 > b2 ? b1 : b2;
    }

    /*
    public Byte call(Byte b1, Byte b2) {
        if (b1.equals(b2)) {
            return b1;
        }

        Tuple<Primitive, Integer> tup1 = Tuple.byteToTuple(b1);
        Tuple<Primitive, Integer> tup2 = Tuple.byteToTuple(b2);
        // WINS
        if (tup1.x == Primitive.WIN || tup2.x == Primitive.WIN) {
            if (tup2.x != Primitive.WIN) {
                return b1;
            } else if (tup1.x != Primitive.WIN) {
                return b2;
            // Both are wins
            } else if (tup1.y < tup2.y) {
                return b1;
            } else {
                return b2;
            }
        }
        // TIES
        if (tup1.x == Primitive.TIE || tup2.x == Primitive.TIE) {
            if (tup2.x != Primitive.TIE) {
                return b1;
            } else if (tup1.x != Primitive.TIE) {
                return b2;
                // Both are ties
            } else if (tup1.y < tup2.y) {
                return b1;
            } else {
                return b2;
            }
        }

        //Both are LOSS
        if (tup1.y > tup2.y) {
            return b1;
        } else {
            return b2;
        }
    }
    */



}
