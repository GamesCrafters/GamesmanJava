package Helpers;

public enum Primitive {
    NOT_PRIMITIVE,
    WIN,
    LOSS,
    // DRAW,
    TIE;

    public static byte toByte(Primitive p, Integer i) {
        byte temp = 0;
        switch (p) {
            case NOT_PRIMITIVE:
                temp = -1;
                break;
            case LOSS:
                temp = -128;
                temp +=  i.byteValue();
                break;
            case WIN:
                temp = 127;
                temp -=  i.byteValue();
                break;
            case TIE:
                temp = 0;
                temp += i.byteValue();
                break;
            default:
                throw new IllegalStateException("shouldn't happen");
        }
        return temp;
    }
}
