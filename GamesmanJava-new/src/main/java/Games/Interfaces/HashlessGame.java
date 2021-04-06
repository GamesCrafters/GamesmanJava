package Games.Interfaces;

import org.apache.spark.api.java.function.*;
import scala.Tuple2;

import java.util.Iterator;

/**
 * Hashless games are those whose positions and hash are the same,
 * this means it will always be a Long object
 */
public abstract class HashlessGame implements Game {

    /**
     * Used to start of the first position of the game
     * @return The starting position of the game, now has to be a long
     */
    abstract public Long getStartingPosition();


    /**
     * Returns the function used to map positions to their children
     * @return A FlatMapFunction taking in a long and creating a list of longs
     */
    public abstract FlatMapFunction<Long, Long> getDownwardFunc();

    /**
     * Returns the function used to check if a position is primitives
     * @return A Function taking in a long and creating a Boolean
     */
    public abstract Function<Long, Boolean> getPrimitiveCheck();

    /**
     * Returns the function used to calculate the byte value of a position
     * @return A PairFunction mapping long to long, byte
     */
    public abstract PairFunction<Long, Long, Byte> getPrimitiveFunc();

    /**
     * Returns the function used to print out the found byte values
     * @param id The current id of the solve
     * @return A VoidFunction that will iterate over the current tiers solves
     */
    public abstract VoidFunction<Iterator<Tuple2<Long, Byte>>> getOutputFunction(String id);

    /**
     * Returns the function used to calculate the parent values of a position
     * @return A PairFlatMapFunction mapping long, byte to a long, byte list
     */
    public abstract PairFlatMapFunction<Tuple2<Long, Byte>, Long, Byte> getParentFunction();

    /**
     * Returns the function that combines two functions to a parent positions
     * @return A function mapping two Bytes to a final Byte
     */
    public abstract Function2<Byte, Byte, Byte> getCombineFunc();

}
