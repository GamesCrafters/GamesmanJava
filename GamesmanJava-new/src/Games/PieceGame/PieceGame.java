package Games.PieceGame;

import Games.Interfaces.KeyValueGame;
import Helpers.Piece;
import Helpers.Primitive;
import Helpers.Tuple;
import Tier.PrimitiveFilter;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import scala.Tuple2;

import java.util.List;

/**
 * Represents a game played where position is represented by a Piece[]
 */
public abstract class PieceGame extends KeyValueGame {

    Piece nextP;
    int tier;

    @Override
    public PairFlatMapFunction<Tuple2<Long, Object>, Long, Object> getDownwardFunc() {
        return new DownwardThread(nextP, tier, this);
    }

    @Override
    public Function<Tuple2<Long, Object>, Boolean> getPrimitiveFunc() {
        return new PrimitiveFilter(nextP, this);
    }

    @Override
    public void solveStarting() {
        tier = 0;
        nextP = Piece.RED;
    }

    @Override
    public void solveStepDown() {
        tier += 1;
        nextP = nextP.opposite();
    }

    @Override
    public long calculateLocation(Object board) {
        return calculateLocation((Piece[]) board);
    }

    public int getTier() {
        return tier;
    }

    @Override
    public void printBoard(Object board) {
        printBoard((Piece[]) board);
    }

    /**
     * Calculates the location of a certain board state
     * @return The byte offset of the state
     */
    abstract public long calculateLocation(Piece[] board);

    /**
     * Use a created move and place it on the board
     * @param position Board state before moving
     * @param move The move to make
     * @param p The Piece to place
     * @return The new position after making the move
     */
    abstract public Piece[] doMove(Piece[] position, int move, Piece p);

    /**
     * Creates a list of valid moves from a position
     * @param position The position to generate moves for
     * @return A newly created list that contains the int moves
     */
    abstract public List<Integer> generateMoves(Piece[] position);

    /**
     * Determines if a position is a primitive, and if so returns its remoteness as well
     * @param position Position to check
     * @param placed The last piece placed
     * @return A tuple of Primitive value and remoteness
     */
    abstract public Tuple<Primitive, Integer> isPrimitive(Piece[] position, Piece placed);

    /**
     * Determines if a position is a primitive, and if so returns its remoteness as well.
     * Speed improved by checking the location placed only
     * @param position Position to check
     * @param placed The last piece placed
     * @param location The location (index of position) where the last piece was placed
     * @return A tuple of Primitive value and remoteness
     */
    abstract public Tuple<Primitive, Integer> isPrimitive(Piece[] position, Piece placed, int location);

    /**
     * The symmetrical move to another move, used to help remove symmetries
     * @param move Move to flip
     * @return A new move that is symmetric
     */
    abstract public int symMove(int move);

    /**
     * @return The current size of the board
     */
    abstract public int getSize();


    /**
     * Prints the current board state
     */
    abstract public void printBoard(Piece[] board);
}
