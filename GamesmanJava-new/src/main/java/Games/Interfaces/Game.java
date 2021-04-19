package Games.Interfaces;

public interface TieredGame<GameObject> {



    /**
     * Used to start of the first position of the game
     * @return The starting position of the game
     */
    GameObject getStartingPosition();

    /**
     * @return The name of the game
     */
    String getName();

    /**
     * @return The name of the variant
     */
    String getVariant();

    /**
     * @return The value of the highest tier in the game
     */
    int getMaxTiers();


    /**
     * Initializes any data that may be needed to start a new solve
     */
    void refresh();

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
    void printBoard(GameObject board);
}
