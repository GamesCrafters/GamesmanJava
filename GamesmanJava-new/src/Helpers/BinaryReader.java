package Helpers;

import java.util.Scanner;

public class BinaryReader {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while(true) {
            System.out.println("Enter byte");
            Byte b = scanner.nextByte();
            Tuple<Primitive, Integer> value = toTuple(b);
            System.out.printf("%s in %s%n", value.x, value.y);
        }

    }
    private static Tuple<Primitive, Integer> toTuple(Byte b) {
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
