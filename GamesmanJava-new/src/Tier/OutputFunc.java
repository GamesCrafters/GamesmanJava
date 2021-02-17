package Tier;

import Helpers.Piece;
import Helpers.Tuple;
import org.apache.spark.api.java.function.VoidFunction;
import scala.Tuple2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.Iterator;

import static java.nio.file.StandardOpenOption.*;
import static java.nio.file.StandardOpenOption.READ;

public class OutputFunc implements VoidFunction<Iterator<Tuple2<Long, Tuple<Byte, Piece[]>>>> {

    SeekableByteChannel channel;
    Path path = null;
    int tier;
    OutputFunc(int tier) {
        this.tier = tier;
    }

    @Override
    public void call(Iterator<Tuple2<Long, Tuple<Byte, Piece[]>>> iter) throws IOException {
        path = Paths.get("tier_" + tier);
        try {
            channel = Files.newByteChannel(path, EnumSet.of(CREATE_NEW, WRITE, SPARSE, READ));
        } catch (Exception e) {
            try {
                channel = Files.newByteChannel(path, EnumSet.of(WRITE, READ));
            } catch (Exception e1) {
                throw new IllegalStateException("File opening went funky");
            }

        }
        while (iter.hasNext()) {
            Tuple2<Long, Tuple<Byte, Piece[]>> tup = iter.next();
            ByteBuffer buf = ByteBuffer.allocate(1);
            channel.position(tup._1());
            Byte temp = tup._2().x;
            buf.put(temp);
            buf.position(0);
            channel.write(buf);
        }
    }
}
