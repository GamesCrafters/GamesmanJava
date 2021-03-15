package Games.Interfaces;

import Helpers.Piece;
import Helpers.Primitive;
import Helpers.Tuple;

import java.util.List;

/**
 * Represents a game played using the Helpers.Piece class.
 */
public interface PieceGame {


    /**
     * Used to start of the first position of the game
     * @return The starting position of the game
     */
    Piece[] getStartingPositions();

    /**
     * Use a created move and place it on the board
     * @param position Board state before moving
     * @param move The move to make
     * @param p The Piece to place
     * @return The new position after making the move
     */
    Piece[] doMove(Piece[] position, int move, Piece p);

    /**
     * Creates a list of valid moves from a position
     * @param position The position to generate moves for
     * @return A newly created list that contains the int moves
     */
    List<Integer> generateMoves(Piece[] position);

    /**
     * Determines if a position is a primitive, and if so returns its remoteness as well
     * @param position Position to check
     * @param placed The last piece placed
     * @return A tuple of Primitive value and remoteness
     */
    Tuple<Primitive, Integer> isPrimitive(Piece[] position, Piece placed);

    /**
     * Determines if a position is a primitive, and if so returns its remoteness as well.
     * Speed improved by checking the location placed only
     * @param position Position to check
     * @param placed The last piece placed
     * @param location The location (index of position) where the last piece was placed
     * @return A tuple of Primitive value and remoteness
     */
    Tuple<Primitive, Integer> isPrimitive(Piece[] position, Piece placed, int location);

    /**
     * The symmetrical move to another move, used to help remove symmetries
     * @param move Move to flip
     * @return A new move that is symmetric
     */
    int symMove(int move);

    /**
     * @return The current size of the board
     */
    int getSize();

    /**
     * @return The name of the game
     */
    String getName();

    /**
     * @return The name of the variant
     */
    String getVariant();

    /**
     * Prints the current board state
     */
    void printBoard(Piece[] board);
}
