package Games.Interfaces;

import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import scala.Tuple2;

/**
 * A Game whose position and location are not the same
 */
public abstract class KeyValueGame implements Game {

    /**
     * Calculates the location of a certain board state
     * @return The byte offset of the state
     */
    abstract public long calculateLocation(Object board);

    @Override
    public boolean positionIsLocation() {
        return false;
    }

    /**
     * Returns the function used to calculate the children of a position
     * @return A PairFlatMapFunction that takes in a location and board, and returns a list of locations and boards
     */
    abstract public PairFlatMapFunction<Tuple2<Long, Object>, Long, Object> getDownwardFunc();

    /**
     * Returns the function used to determine if a board is primitive
     * @return A Function that takes in a location and board, and returns if its primitive
     */
    abstract public Function<Tuple2<Long, Object>, Boolean> getPrimitiveFunc();

}
