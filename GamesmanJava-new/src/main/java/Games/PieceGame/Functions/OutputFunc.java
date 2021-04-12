package Games.PieceGame.Functions;

import Helpers.Piece;
import Helpers.Tuple;
import org.apache.spark.TaskContext;
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

public class OutputFunc implements VoidFunction<Iterator<Tuple2<Long, Tuple<Byte, Object>>>> {

    SeekableByteChannel channel;
    Path path = null;
    int tier;
    String id;
    public OutputFunc(String id, int tier) {
        this.tier = tier;
        this.id = id;
    }

    @Override
    public void call(Iterator<Tuple2<Long, Tuple<Byte, Object>>> iter) throws IOException {
        if (!iter.hasNext()) {
            return;
        }
        path = Paths.get(id + "/tier_" + tier);
        path = Paths.get(String.format("%s/tier_%d/part_%s", id, tier, TaskContext.getPartitionId()));
        try {
            channel = Files.newByteChannel(path, EnumSet.of(CREATE_NEW, WRITE, SPARSE, READ));
        } catch (Exception e) {
            channel = Files.newByteChannel(path, EnumSet.of(WRITE, READ));
        }
        while (iter.hasNext()) {
            Tuple2<Long, Tuple<Byte, Object>> tup = iter.next();
            ByteBuffer buf = ByteBuffer.allocate(1);
            channel.position(tup._1());
            Byte temp = tup._2().x;
            buf.put(temp);
            buf.position(0);
            channel.write(buf);
        }
    }
}
