package Games.Interfaces;

import Helpers.Tuple;
import org.apache.spark.api.java.function.*;
import scala.Tuple2;

import java.util.Iterator;

/**
 * A TieredGame whose position and location are not the same
 */
public abstract class KeyValueGame<GameObject> implements TieredGame<GameObject> {

    /**
     * Calculates the location of a certain board state
     * @return The byte offset of the state
     */
    abstract public long calculateLocation(GameObject board);

    /**
     * @return a PairFlatMapFunction with the following input and output:
     * Input: A Tuple2<Long, GameObject> corresponding to a games hash and position
     * Output: A list of Tuple2<Long, GameObject> consisting of all children positions of the input, represented by
     *         an iterator
     */
    abstract public PairFlatMapFunction<Tuple2<Long, GameObject>, Long, GameObject> getDownwardFunc();

    /**
     * @return A Function taking in a Tuple2<Long, GameObject> and creating a Boolean with the following
     *         input and output:
     * Input: A Tuple2<Long, GameObject> corresponding to a games hash and position
     * Output: A Boolean representing if that position is primitive
     */
    abstract public Function<Tuple2<Long, GameObject>, Boolean> getPrimitiveCheck();

    /**
     * @return A PairFunction mapping Tuple2<Long, GameObject> to Tuple2<Long, Tuple<Byte, GameObject>> with the
     *         following input and output:
     * Input: A Tuple2<Long, GameObject> corresponding to a games hash and position
     * Output: A Tuple2<Long, Tuple<Byte, GameObject>> with the Long and GameObject representing the same position
     *         and the Byte representing the value
     *         usually gotten with Primitive.toByte().
     *
     * This function is only called by the solver after filtering an RDD with getPrimitiveCheck() to make sure that
     * all positions are primitive.
     *
     * Calling this function with non primitive positions is undefined
     */
    abstract public PairFunction<Tuple2<Long, GameObject>, Long, Tuple<Byte,GameObject>> getPrimitiveFunc();

    /**
     * @param id The current id of the solve
     * @return A VoidFunction that will iterate over the current tiers solves with the following behavior:
     * Input: An Iterator<Tuple2<Long, Tuple<Byte, GameObject>>>, with each Tuple2 representing the hash, finalized
     *        value, and board state of a position
     * Behavior: Creates a file {OUTPUT_FOLDER}/{GAME_NAME}_(GAME_VARIANT}/tier_{TIER_NUM}/part_{PARTITION_ID}
     *
     * See Games.PieceGame.Functions.OutputFunc for a good example of this behavior and how the necessary information
     * should be passed
     */
    public abstract VoidFunction<Iterator<Tuple2<Long, Tuple<Byte, GameObject>>>> getOutputFunction(String id);

    /**
     * @return A PairFlatMapFunction mapping long, byte, GameObject to a long, byte, GameObject list with the
     *         following input and output
     * Input: A Long representing a position's hash, a Byte representing the positions value, and a GameObject
     *        representing the board state
     * Output: An Iterator<Tuple2<Long, Tuple<Byte, GameObject>>> where each tuple represents a possible parent position
     *         of the input position. The Byte value of the parent positions is computed as if the input position is
     *         its only child.
     */
    abstract public PairFlatMapFunction<Tuple2<Long, Tuple<Byte, GameObject>>, Long, Tuple<Byte, GameObject>> getParentFunction();

    /**
     * @return A function mapping two Bytes to a final Byte with the following input and output
     * Input: Two separate Tuple<Byte, GameObject>, each representing a possible value of the same position
     * Output: Whichever of the two Tuple<Byte, GameObject> has a Byte that is a better position, based on
     *         win > tie > loss
     */
    public Function2<Tuple<Byte, GameObject>, Tuple<Byte, GameObject>, Tuple<Byte, GameObject>> getCombineFunc() {
        return new ParentCombineFunc<>();
    }

}
