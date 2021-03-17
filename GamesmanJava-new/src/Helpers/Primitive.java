package Helpers;

public enum Primitive {
    NOT_PRIMITIVE((byte)-1),
    WIN((byte)127),
    LOSS((byte)-64),
    DRAW((byte)-63),
    TIE((byte)64);

    private byte value;

    Primitive(byte b) {
        this.value = b;
    }
    /*
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

     */
}
