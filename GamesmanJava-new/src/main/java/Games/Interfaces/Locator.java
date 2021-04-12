package Games.Interfaces;

import Helpers.Piece;

public interface Locator {


    /**
     * Returns the long location of the board, any initialization should be done
     * when initializing the class
     * @param position The board
     * @param numPieces The number of pieces on the board
     * @return The byte offset of this board
     */
    long calculateLocation(Object position, int numPieces);


}
