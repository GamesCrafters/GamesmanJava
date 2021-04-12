package Helpers;

import java.io.Serializable;

public class Tuple<X, Y> implements Serializable {
    public final X x;
    public final Y y;
    public Tuple(X x, Y y) {
        this.x = x;
        this.y = y;
    }



    public static Tuple<Primitive, Integer> byteToTuple(Byte b) {
        Primitive p;
        Integer remoteness;
        Integer x = Byte.toUnsignedInt(b);
        if (x >= 128) {
            if (x >= 192) {
                p = Primitive.WIN;
                remoteness = 255-x;
            } else {
                p = Primitive.TIE;
                remoteness = x-128;
            }
        } else {
            if (x <= 63) {
                p = Primitive.LOSS;
                remoteness = x;
            } else {
                p = Primitive.NOT_PRIMITIVE;
                remoteness = 0;
            }
        }

        return new Tuple<>(p, remoteness);
    }

}