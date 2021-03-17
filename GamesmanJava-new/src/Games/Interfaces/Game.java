package Games.Interfaces;

public interface Game {



    /**
     * Used to start of the first position of the game
     * @return The starting position of the game
     */
    Object getStartingPosition();

    /**
     * @return The name of the game
     */
    String getName();

    /**
     * @return The name of the variant
     */
    String getVariant();

    /**
     * @return The name of the variant
     */
    int getNumTiers();

    /**
     * @return If the position and location are the same
     */
    boolean positionIsLocation();

    /**
     * Initializes any data that may be needed to start a new solve
     */
    void solveStarting();

    /**
     * Adjusts data so that the game is changed state such that a move is made
     */
    void solveStepDown();

    /**
     * Adjusts data so that the  game is changed state such that a move is UNMADE
     */
    void solveStepUp();

    /**
     * Prints the board state
     * @param board The current board state
     */
    void printBoard(Object board);
}
