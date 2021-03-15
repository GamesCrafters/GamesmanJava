package Games.Interfaces;

import Helpers.Piece;

public interface Game {

    /**
     * Returns the locator used to find the hash location of a board state
     * @return A locator class object
     */
    Locator getLocator();

    /**
     * Used to start of the first position of the game
     * @return The starting position of the game
     */
    Object getStartingPosition();
}
