package edu.berkeley.gamesman.game.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author DNSpies
 */
public final class PieceRearranger implements Cloneable {
    /**
     * Iterates over all the pieces whose colors flipped
     * 
     * @author dnspies
     */
    public static final class ChangedPieces {
        private final int firstEnd, secondEnd, thirdEnd;

        private int next = 0;

        /**
         * @param firstSwitchedX The number of x's that moved to the right
         * @param firstSwitchedO The number of o's that moved to the left
         */
        public ChangedPieces(int firstSwitchedX, int firstSwitchedO) {
            if (firstSwitchedX < firstSwitchedO) {
                firstEnd = firstSwitchedX;
                secondEnd = firstSwitchedO;
            } else {
                firstEnd = firstSwitchedO;
                secondEnd = firstSwitchedX;
            }
            thirdEnd = firstSwitchedX + firstSwitchedO + 2;
            if (firstEnd == 0)
                next = secondEnd;
        }

        /**
         * @return Was another piece flipped?
         */
        public boolean hasNext() {
            return next < thirdEnd;
        }

        /**
         * @return The index of the next flipped piece
         */
        public int next() {
            int result = next;
            next++;
            if (next >= firstEnd && next < secondEnd)
                next = secondEnd;
            return result;
        }
    }

    private static final class HashPiece {
        private final int index;

        private char player;

        private long hash;

        private long nextO;

        private long nextX;

        private HashGroup group;

        private HashPiece(int pieces, int os, long hash, char player,
                HashGroup group) {
            this.player = player;
            this.index = pieces;
            this.hash = hash;
            if (hash == 0) {
                nextO = 0;
                nextX = 1;
            } else {
                nextO = hash * (pieces + 1) / (os + 1);
                nextX = hash * (pieces + 1) / (pieces + 1 - os);
            }
            this.group = group;
        }

        private void set(int os, long hash, char player) {
            long xChange = 0;
            long oChange = 0;
            if (this.player == 'O') {
                xChange -= nextX - this.hash;
                oChange -= nextO - this.hash;
            }
            this.player = player;
            this.hash = hash;
            if (hash == 0) {
                nextO = 0;
                nextX = 1;
            } else {
                nextO = hash * (index + 1) / (os + 1);
                nextX = hash * (index + 1) / (index + 1 - os);
            }
            if (player == 'O') {
                xChange += nextX - hash;
                oChange += nextO - hash;
            }
            group.setPiece(oChange, xChange);
        }

        public String toString() {
            return String.valueOf(player);
        }
    }

    private static final class HashGroup {
        private HashPiece lastPiece;
        private long addO = 0;
        private long addX = 0;

        private HashGroup(HashPiece lastPiece) {
            this.lastPiece = lastPiece;
        }

        private void addPiece(HashPiece p) {
            if (p.player == 'O')
                setPiece(p.nextO - p.hash, p.nextX - p.hash);
            lastPiece = p;
        }

        private void setPiece(long addOChange, long addXChange) {
            addO += (addOChange);
            addX += (addXChange);
        }
    }

    /**
     * The number of possible arrangements of the given number of X's and O's
     */
    public final long colorArrangements;

    private final LinkedList<HashGroup> groups;

    private final int numOs;

    private long hash = 0;

    private int openX = 0, openO = 0;

    private boolean hasNext;

    private final ArrayList<HashPiece> pieces;

    private final HashPiece lowPiece = new HashPiece(-1, 0, 0, 'O', null);

    /**
     * @param s A character representation of the board (in 'X' 'O' and ' ')
     */
    public PieceRearranger(final char[] s) {
        groups = new LinkedList<HashGroup>();
        HashPiece lastPiece = lowPiece;
        HashGroup currentGroup = new HashGroup(lastPiece);
        pieces = new ArrayList<HashPiece>(s.length);
        int numOs = 0;
        boolean onFX = true, onFO = true;
        for (int i = 0; i < s.length; i++) {
            if (s[i] == ' ') {
                groups.add(currentGroup);
                currentGroup = new HashGroup(lastPiece);
            } else {
                if (s[i] == 'O') {
                    if (onFO) {
                        onFX = false;
                        openO++;
                    }
                    numOs++;
                    lastPiece = new HashPiece(pieces.size(), numOs,
                            lastPiece.nextO, 'O', currentGroup);
                    hash += lastPiece.hash;
                    currentGroup.addPiece(lastPiece);
                } else if (s[i] == 'X') {
                    if (onFX)
                        openX++;
                    else
                        onFO = false;
                    lastPiece = new HashPiece(pieces.size(), numOs,
                            lastPiece.nextX, 'X', currentGroup);
                    currentGroup.addPiece(lastPiece);
                } else
                    new Exception("Bad String: " + String.valueOf(s))
                            .printStackTrace();
                pieces.add(lastPiece);
            }
        }
        hasNext = !onFO;
        groups.add(currentGroup);
        colorArrangements = lastPiece.nextX;
        this.numOs = numOs;
    }

    /**
     * @param s A character outline telling where the pieces and spaces are
     * @param os The number of O's on the board
     * @param xs The number of X's on the board
     */
    public PieceRearranger(final char[] s, int os, int xs) {
        groups = new LinkedList<HashGroup>();
        HashPiece lastPiece = lowPiece;
        HashGroup currentGroup = new HashGroup(lastPiece);
        pieces = new ArrayList<HashPiece>(s.length);
        int numOs = 0;
        for (int i = 0; i < s.length; i++) {
            if (s[i] == ' ') {
                groups.add(currentGroup);
                currentGroup = new HashGroup(lastPiece);
            } else {
                if (numOs < os) {
                    numOs++;
                    lastPiece = new HashPiece(pieces.size(), numOs,
                            lastPiece.nextO, 'O', currentGroup);
                    currentGroup.addPiece(lastPiece);
                } else {
                    lastPiece = new HashPiece(pieces.size(), numOs,
                            lastPiece.nextX, 'X', currentGroup);
                    currentGroup.addPiece(lastPiece);
                }
                pieces.add(lastPiece);
            }
        }
        groups.add(currentGroup);
        colorArrangements = lastPiece.nextX;
        this.numOs = numOs;
        openO = os;
        openX = 0;
        hasNext = xs > 0 && os > 0;
    }

    /**
     * Puts all the Os before all the Xs
     */
    public void reset() {
        int i;
        for (i = 0; i < numOs; i++)
            pieces.get(i).set(i, 0, 'O');
        HashPiece lastPiece;
        if (numOs > 0)
            lastPiece = pieces.get(numOs - 1);
        else
            lastPiece = lowPiece;
        for (; i < pieces.size(); i++)
            pieces.get(i).set(numOs, lastPiece.nextX, 'X');
        openO = numOs;
        openX = 0;
        hasNext = pieces.size() > numOs && numOs > 0;
        hash = 0;
    }

    /**
     * Sets the groups to the provided sizes
     * 
     * @param groupSizes The size of each group
     */
    public void setGroupSizes(List<Integer> groupSizes) {
        groups.clear();
        int k = 0;
        int totSize = 0;
        HashPiece lastPiece = lowPiece;
        HashGroup g;
        for (int count : groupSizes) {
            g = new HashGroup(lastPiece);
            for (totSize += count; k < totSize; k++) {
                lastPiece = pieces.get(k);
                lastPiece.group = g;
                g.addPiece(lastPiece);
            }
            groups.add(g);
        }
        g = new HashGroup(lastPiece);
        for (; k < pieces.size(); k++) {
            lastPiece = pieces.get(k);
            lastPiece.group = g;
            g.addPiece(lastPiece);
        }
        groups.add(g);
    }

    /**
     * @param s A character representation of the board (in 'X' 'O' and ' ')
     */
    public PieceRearranger(String s) {
        this(s.toCharArray());
    }

    /**
     * @param s A character outline telling where the pieces and spaces are (' '
     *            is space, anything else is piece)
     * @param os The number of O's on the board
     * @param xs The number of X's on the board
     */
    public PieceRearranger(String s, int os, int xs) {
        this(s.toCharArray(), os, xs);
    }

    /**
     * @param player 'X' or 'O', the piece to be added to the board
     * @return A collection of all the hashes after each possible move is made.
     */
    public long[] getChildren(final char player) {
        long[] result = new long[groups.size() - 1];
        long move = hash;
        Iterator<HashGroup> it = groups.descendingIterator();
        if (player == 'O') {
            HashGroup g;
            move += it.next().addO;
            for (int i = result.length - 1; i >= 0; i--) {
                g = it.next();
                result[i] = move + g.lastPiece.nextO;
                move += g.addO;
            }
        } else if (player == 'X') {
            move += it.next().addX;
            for (int i = result.length - 1; i >= 0; i--) {
                result[i] = move;
                move += it.next().addX;
            }
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
     * @return An iterator over the pieces that changed
     */
    public ChangedPieces next() {
        int newOpenO = openO - 1;
        int totalOpen = openX + newOpenO;
        ChangedPieces cp = new ChangedPieces(openX, newOpenO);
        int i;
        if (openX > 0 && newOpenO > 0) {
            for (i = 0; i < newOpenO; i++)
                pieces.get(i).set(i + 1, 0, 'O');
            for (i = newOpenO; i < totalOpen; i++)
                pieces.get(i).set(newOpenO, pieces.get(i - 1).nextX, 'X');
        }
        long firstXHash = (totalOpen == 0 ? 1 : pieces.get(totalOpen - 1).nextX);
        pieces.get(totalOpen).set(newOpenO, firstXHash, 'X');
        pieces.get(totalOpen + 1).set(newOpenO + 1,
                pieces.get(totalOpen).nextO, 'O');
        if (newOpenO > 0) {
            openX = 0;
            openO = newOpenO;
        } else {
            openX++;
            i = totalOpen + 1;
            for (i = totalOpen + 2; i < pieces.size(); i++) {
                if (pieces.get(i).player == 'O')
                    openO++;
                else
                    break;
            }
            if (i == pieces.size())
                hasNext = false;
        }
        hash++;
        return cp;
    }

    /**
     * @param piece The index of the piece to return
     * @return The character of the piece.
     */
    public char get(final int piece) {
        return pieces.get(piece).player;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder(pieces.size() + groups.size());
        Iterator<HashGroup> gIt = groups.iterator();
        HashGroup g = gIt.next();
        while (g.lastPiece == lowPiece) {
            str.append(' ');
            g = gIt.next();
        }
        for (HashPiece piece : pieces) {
            str.append(piece.player);
            while (g.lastPiece == piece) {
                str.append(' ');
                g = gIt.next();
            }
        }
        return str.substring(0, str.length() - 1);
    }

    @Override
    public PieceRearranger clone() {
        try {
            return new PieceRearranger(toString().toCharArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @return The current hash value
     */
    public long getHash() {
        return hash;
    }

    /**
     * Sets the board to the positoin represented by the given hash
     * 
     * @param hash
     */
    public void setFromHash(long hash) {
        this.hash = hash;
        openO = 0;
        openX = 0;
        long tryHash;
        if (pieces.size() > 0)
            tryHash = colorArrangements * (pieces.size() - numOs)
                    / pieces.size();
        else
            tryHash = 0;
        int oCount = numOs;
        for (int i = pieces.size() - 1; i >= 0; i--) {
            if (hash >= tryHash) {
                hash -= tryHash;
                pieces.get(i).set(oCount, tryHash, 'O');
                if (i > 0)
                    tryHash = tryHash * oCount / i;
                oCount--;
                if (openX > 0) {
                    openO = 1;
                    openX = 0;
                } else
                    openO++;
            } else {
                pieces.get(i).set(oCount, tryHash, 'X');
                if (i > 0)
                    tryHash = tryHash * (i - oCount) / i;
                openX++;
            }
        }
    }
}
