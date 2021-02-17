package Tier;

import Helpers.Piece;
import Helpers.Primitive;
import Helpers.Tuple;
import org.apache.spark.api.java.function.Function2;
import scala.Tuple2;

public class ParentCombineFunc implements Function2<Tuple<Byte, Piece[]>, Tuple<Byte, Piece[]>, Tuple<Byte, Piece[]>> {


    @Override
    public Tuple<Byte, Piece[]> call(Tuple<Byte, Piece[]> byteTuple, Tuple<Byte, Piece[]> byteTuple2){
        return new Tuple<>(call(byteTuple.x, byteTuple2.x), byteTuple.y);
    }

    public Byte call(Byte b1, Byte b2) {
        if (b1.equals(b2)) {
            return b1;
        }

        Tuple<Primitive, Integer> tup1 = toTuple(b1);
        Tuple<Primitive, Integer> tup2 = toTuple(b2);
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
        if (tup1.y < tup2.y) {
            return b1;
        } else {
            return b2;
        }
    }


    private Tuple<Primitive, Integer> toTuple(Byte b) {
        int val = Byte.toUnsignedInt(b);
        int remoteness = (val << 26) >>> 26;
        Primitive p;
        switch((val) >>> 6) {
            case 0:
                p = Primitive.NOT_PRIMITIVE;
                break;
            case 1:
                p = Primitive.LOSS;
                break;
            case 2:
                p = Primitive.WIN;
                break;
            case 3:
                p = Primitive.TIE;
                break;
            default:
               throw new IllegalStateException("Should only have these options");
        }
        return new Tuple<>(p, remoteness);
    }


}
