package Helpers;

import java.util.Scanner;

public class BinaryReader {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while(true) {
            System.out.println("Enter byte");
            Byte b = scanner.nextByte();
            System.out.println(b);
            Tuple<Primitive, Integer> value = Tuple.byteToTuple(b);
            System.out.printf("%s in %s%n", value.x, value.y);
        }

    }
}
