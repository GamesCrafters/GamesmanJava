package edu.berkeley.gamesman.game.util;

import java.util.ArrayList;
import java.util.Collection;

import edu.berkeley.gamesman.util.Pair;

/**
 * @author DNSpies
 */
public final class SpaceRearranger implements Cloneable {
    private static final class HashPlace {
        private final int index;
        private boolean isPiece;
        private long hash;
        private long addPiece;

        private HashPlace(int index, int pieces, long lastHash, boolean isPiece) {
            this.index = index;
            setFromLast(pieces, lastHash, isPiece);
        }

        private void setFromLast(int pieces, long lastHash, boolean isPiece) {
            this.isPiece = isPiece;
            if (lastHash == 0) {
                if (isPiece)
                    this.hash = 0;
                else
                    this.hash = 1;
                addPiece = 0;
            } else {
                if (isPiece) {
                    this.hash = lastHash * index / pieces;
                } else {
                    this.hash = lastHash * index / (index - pieces);
                }
                addPiece = hash * (index - pieces) / (pieces + 1);
            }
        }

        public String toString() {
            return String.valueOf(isPiece);
        }

        private void set(int pieces, long hash, boolean isPiece) {
            this.isPiece = isPiece;
            this.hash = hash;
            if (hash == (0)) {
                addPiece = 0;
            } else {
                addPiece = hash * (index - pieces) / (pieces + 1);
            }
        }
    }

    /**
     * The number of possible arrangements of the given number of pieces and
     * spaces
     */
    public final long arrangements;
    private final int numPlaces, numPieces;
    private long hash = 0;
    private int openSpace = 0, openPiece = 0;
    private boolean hasNext = true;
    private final HashPlace[] places;

    /**
     * @param s A character representation of the board (in false true and ' ')
     */
    public SpaceRearranger(final boolean[] s) {
        numPlaces = s.length;
        places = new HashPlace[numPlaces];
        int numPieces = 0;
        long lastHash = 0;
        boolean onFSpace = true;
        boolean onFPiece = true;
        for (int i = 0; i < s.length; i++) {
            if (s[i]) {
                if (onFPiece) {
                    onFSpace = false;
                    openPiece++;
                }
                numPieces++;
                places[i] = new HashPlace(i, numPieces, lastHash, true);
                lastHash = places[i].hash;
            } else {
                if (onFSpace) {
                    openSpace++;
                } else
                    onFPiece = false;
                places[i] = new HashPlace(i, numPieces, lastHash, false);
                lastHash = places[i].hash;
            }
        }
        if (onFPiece)
            hasNext = false;
        this.numPieces = numPieces;
        if (numPlaces == numPieces)
            arrangements = 1;
        else
            arrangements = lastHash * numPlaces / (numPlaces - numPieces);
    }

    /**
     * @param pieces The number of pieces on the board
     * @param spaces The number of spaces on the board
     */
    public SpaceRearranger(int pieces, int spaces) {
        numPlaces = pieces + spaces;
        if (pieces == 0 || spaces == 0)
            hasNext = false;
        places = new HashPlace[numPlaces];
        numPieces = pieces;
        openPiece = pieces;
        openSpace = 0;
        long lastHash = 0;
        for (int i = 0; i < numPlaces; i++) {
            if (pieces > 0) {
                places[i] = new HashPlace(i, i + 1, 0, true);
                lastHash = places[i].hash;
                pieces--;
            } else {
                places[i] = new HashPlace(i, numPieces, lastHash, false);
                lastHash = places[i].hash;
            }
        }
        if (numPlaces == numPieces)
            arrangements = 1;
        else
            arrangements = lastHash * numPlaces / (numPlaces - numPieces);
    }

    /**
     * @param s A character representation of the board (in false true and ' ')
     */
    public SpaceRearranger(String s) {
        this(makeBoolArray(s));
    }

    private static boolean[] makeBoolArray(String s) {
        boolean[] arr = new boolean[s.length()];
        for (int i = 0; i < s.length(); i++) {
            arr[i] = s.charAt(i) != ' ';
        }
        return arr;
    }

    /**
     * @return A collection of all the hashes after each possible move is made.
     */
    public ArrayList<Pair<Integer, Long>> getChildren() {
        ArrayList<Pair<Integer, Long>> result = new ArrayList<Pair<Integer, Long>>(
                numPlaces - numPieces);
        long move = hash;
        for (int i = numPlaces - 1; i >= 0; i--) {
            if (places[i].isPiece)
                move = move + (places[i].addPiece - (places[i].hash));
            else
                result.add(new Pair<Integer, Long>(i, move
                        + (places[i].addPiece)));
        }
        return result;
    }

    /**
     * @return Whether these characters have another arrangement.
     */
    public boolean hasNext() {
        return hasNext;
    }

    /**
     * Each time next() is called, the pieces assume their new positions in the
     * next hash and a list of all the pieces that were changed is returned.
     * It's expected that the calling program will use this list to speed up
     * win-checking (if possible).
     * 
     * @return The indices of all the pieces that were changed paired with the
     *         characters they were changed to
     */
    public Collection<Pair<Integer, Boolean>> next() {
        ArrayList<Pair<Integer, Boolean>> changedPlaces = new ArrayList<Pair<Integer, Boolean>>(
                2 * Math.min(openPiece - 1, openSpace) + 2);
        int place = 0;
        int i;
        HashPlace lastPlace = null;
        if (openSpace > 0 && openPiece > 1) {
            for (i = 0; i < openPiece - 1; i++) {
                if (!get(place))
                    changedPlaces.add(new Pair<Integer, Boolean>(place, true));
                places[place].setFromLast(i + 1, 0, true);
                lastPlace = places[place++];
            }
            for (i = 0; i < openSpace; i++) {
                if (get(place))
                    changedPlaces.add(new Pair<Integer, Boolean>(place, false));
                places[place].setFromLast(openPiece - 1, lastPlace.hash, false);
                lastPlace = places[place++];
            }
        } else if (openSpace == 0 && openPiece == 1) {
            lastPlace = null;
        } else {
            place = openSpace + openPiece - 2;
            lastPlace = places[place++];
        }
        changedPlaces.add(new Pair<Integer, Boolean>(place, false));
        if (lastPlace == null)
            places[place].setFromLast(openPiece - 1, 0, false);
        else
            places[place].setFromLast(openPiece - 1, lastPlace.hash, false);
        lastPlace = places[place++];
        changedPlaces.add(new Pair<Integer, Boolean>(place, true));
        places[place].setFromLast(openPiece, lastPlace.hash, true);
        if (openPiece > 1) {
            openSpace = 0;
            openPiece--;
        } else {
            openSpace++;
            place++;
            if (place >= numPlaces)
                hasNext = false;
            else
                while (get(place) == true) {
                    openPiece++;
                    place++;
                    if (place >= numPlaces) {
                        hasNext = false;
                        break;
                    }
                }
        }
        hash++;
        return changedPlaces;
    }

    /**
     * @param piece The index of the piece to return
     * @return The character of the piece.
     */
    public boolean get(int piece) {
        return places[piece].isPiece;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder(numPlaces);
        for (int i = 0; i < places.length; i++)
            str.append(places[i].isPiece ? 'T' : ' ');
        return str.toString();
    }

    @Override
    public SpaceRearranger clone() {
        return new SpaceRearranger(toString());
    }

    /**
     * @return The current hash value
     */
    public long getHash() {
        return hash;
    }

    /**
     * Sets the board to the position represented by the given hash
     * 
     * @param hash The hash
     * @return The new layout of the board
     */
    public String unHash(long hash) {
        this.hash = hash;
        openPiece = 0;
        openSpace = 0;
        long tryHash = arrangements * ((numPlaces - numPieces));
        int numPieces = this.numPieces;
        for (int numPlaces = this.numPlaces; numPlaces > 0; numPlaces--) {
            tryHash = tryHash / ((numPlaces));
            if (hash >= tryHash) {
                hash = hash - (tryHash);
                places[numPlaces - 1].set(numPieces, tryHash, true);
                tryHash = tryHash * ((numPieces));
                numPieces--;
                if (openSpace > 0) {
                    openPiece = 1;
                    openSpace = 0;
                } else
                    openPiece++;
            } else {
                places[numPlaces - 1].set(numPieces, tryHash, false);
                tryHash = tryHash * (numPlaces - numPieces - 1);
                openSpace++;
            }
        }
        return toString();
    }
}
