package Games.Interfaces;

import Helpers.Tuple;
import org.apache.spark.api.java.function.*;
import scala.Tuple2;

import java.util.Iterator;

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
    abstract public Function<Tuple2<Long, Object>, Boolean> getPrimitiveCheck();

    /**
     * Returns the function used to calculate the byte value of a position
     * @return A PairFunction mapping long,object to long, (byte, object)
     */
    abstract public PairFunction<Tuple2<Long, Object>, Long, Tuple<Byte,Object>> getPrimitiveFunc();

    /**
     * Returns the function used to print out the found byte values
     * @param id The current id of the solve
     * @return A VoidFunction that will iterate over the current tiers solves
     */
    public abstract VoidFunction<Iterator<Tuple2<Long, Tuple<Byte, Object>>>> getOutputFunction(String id);

    /**
     * Returns the function used to calculate the parent values of a position
     * @return A PairFlatMapFunction mapping long, (byte, object) to a long, (byte, object) list
     */
    abstract public PairFlatMapFunction<Tuple2<Long, Tuple<Byte, Object>>, Long, Tuple<Byte, Object>> getParentFunction();

    /**
     * Returns the function the combines to parent positions
     * @return A function mapping two Tuples of Byte and Object that have the same long
     */
    public Function2<Tuple<Byte, Object>, Tuple<Byte, Object>, Tuple<Byte, Object>> getCombineFunc() {
        return new ParentCombineFunc();
    }

}
