package Helpers;

import java.io.Serializable;

public class Tuple<X, Y> implements Serializable {
    public final X x;
    public final Y y;
    public Tuple(X x, Y y) {
        this.x = x;
        this.y = y;
    }
}