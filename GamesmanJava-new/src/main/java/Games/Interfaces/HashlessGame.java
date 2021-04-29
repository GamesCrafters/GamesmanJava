package Games.Interfaces;

import org.apache.spark.api.java.function.*;
import scala.Tuple2;

import java.util.Iterator;

/**
 * Hashless games are those whose positions and hash are the same,
 * this means it will always be a Long object
 */
public abstract class HashlessGame implements TieredGame<Long> {

    /**
     * @return a FlatMapFunction with the following input and output:
     * Input: A Long corresponding to a game position
     * Output: A list of Longs consisting of all children positions of the input, represented by an iterator
     */
    public abstract FlatMapFunction<Long, Long> getDownwardFunc();

    /**
     * @return A Function taking in a long and creating a Boolean with the following input and output:
     * Input: A Long representing the board of a position
     * Output: A Boolean representing if that position is primitive
     */
    public abstract Function<Long, Boolean> getPrimitiveCheck();

    /**
     * @return A PairFunction mapping long to long, byte with the following input and output:
     * Input: A Long corresponding to a PRIMITIVE position
     * Output: A Tuple2<Long, Byte> with the Long representing the same position and the Byte representing the value
     *         usually gotten with Primitive.toByte().
     *
     * This function is only called by the solver after filtering an RDD with getPrimitiveCheck() to make sure that
     * all positions are primitive.
     *
     * Calling this function with non primitive positions is undefined
     */
    public abstract PairFunction<Long, Long, Byte> getPrimitiveFunc();

    /**
     * @param id The current id of the solve
     * @return A VoidFunction that will iterate over the current tiers solves with the following behavior:
     * Input: An Iterator<Tuple2<Long, Byte>>, with each Tuple2 representing the hash and finalized value of a position
     * Behavior: Creates a file {OUTPUT_FOLDER}/{GAME_NAME}_(GAME_VARIANT}/tier_{TIER_NUM}/part_{PARTITION_ID}
     *
     * See Games.PieceGame.Functions.OutputFunc for a good example of this behavior and how the necessary information
     * should be passed
     */
    public abstract VoidFunction<Iterator<Tuple2<Long, Byte>>> getOutputFunction(String id);

    /**
     * @return A PairFlatMapFunction mapping long, byte to a long, byte list with the following input and output
     * Input: A Long representing a position and a Byte representing the positions value
     * Output: An Iterator<Tuple2<Long, Byte>> where each tuple represents a possible parent position of the input
     *         position. The Byte value of the parent positions is computed as if the input position is its only child.
     */
    public abstract PairFlatMapFunction<Tuple2<Long, Byte>, Long, Byte> getParentFunction();

    /**
     * @return A function mapping two Bytes to a final Byte with the following input and output
     * Input: Two separate Bytes, each representing a possible value of the same position
     * Output: Whichever of the two Bytes is a better position, based on win > tie > loss
     */
    public abstract Function2<Byte, Byte, Byte> getCombineFunc();

}
