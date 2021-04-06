package Helpers;

public enum Primitive {
    NOT_PRIMITIVE,
    WIN,
    LOSS,
    TIE;

    public static byte toByte(Primitive p, Integer i) {
        Integer temp;
        switch (p) {
            case NOT_PRIMITIVE:
                temp = 0;
                break;
            case LOSS:
                temp = 1;
                break;
            case WIN:
                temp = 2;
                break;
            case TIE:
                temp = 3;
                break;
            default:
                throw new IllegalStateException("shouldn't happen");
        }
        temp = temp << 6;
        temp += i;
        return temp.byteValue();
    }
}
