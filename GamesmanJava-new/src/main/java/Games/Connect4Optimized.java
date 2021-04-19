package Games;


import Helpers.Primitive;

import java.io.Serializable;
import java.util.*;

public class Connect4Optimized implements Serializable {
    final int WIDTH = 5;
    final int HEIGHT = 4;
    final int XINAROW = 3;
    final long HORIZONTALWIN;
    final long VERTICALWIN;
    final long UPDIAGWIN;
    final long DOWNDIAGWIN;
    final long INITIALPOSITION;
    long position;

    /** Each sequence of 7 bits stores a column as follows:
     * Everything up to and including the first 1 are empty. After that, 0 is yellow, 1 is red
     * For example, the string 0b0001011 means ----YRR. Columns are stored left to right or right to left,
     * depending on which yields a lower hash. Bit 63 is used to store the current player.*/
    public Connect4Optimized() {
        long l = 0;
        for(int i = 0; i < WIDTH; i++) l |= 1L<<((HEIGHT+1)*i);
        long down=0, left=0, updiag=0, downdiag = 0;
        for(int i = 0; i < XINAROW;i++)
        {
            down = (down << 1)|1;
            left = (left << (HEIGHT+1))|1;
            downdiag = (downdiag << HEIGHT)|1;
            updiag = (updiag << (HEIGHT+2))|1;
        }
        DOWNDIAGWIN = downdiag;
        HORIZONTALWIN = left;
        VERTICALWIN = down;
        UPDIAGWIN = updiag;
        INITIALPOSITION = l;
        position = INITIALPOSITION;
    }

    public long getStartingPositions() {
        return INITIALPOSITION;
    }

    /** Returns the result of moving at the given position
     * Bit 63 is flipped to change current player
     * Move is assumed to be the bit position of the empty cell.
     * As such, we add 1<<move for a yellow, and 1<<(move+1) for red
     * 0b0001011 + 1<<move = 0b0010011 = ---YYRR
     * 0b0001011 + 1<<(move+1) = 0b0011011 = ---RYRR*/
    public long doMove(long position, byte move) {

        return (position ^ 0x8000000000000000L)+(1L<<(move+(position >>> 63)));

    }

    /** Returns the list of viable moves*/
    public byte[] generateMoves(long position) {
        byte[] ret = new byte[WIDTH];
        int j =0;
        for(int i = 0; i < WIDTH; i++)
        {
            byte start = (byte)((HEIGHT+1)*(i+1) - 1);
            while((position & (1L<<start))==0) start--;
            if(start != (byte)(HEIGHT+1)*(i+1)-1)
            {
                ret[j] = start;
                j++;
            }
        }
        if(j<WIDTH) ret[j] = -1;
        return ret;
    }

    /** Returns if the given position is primitive, assuming that the most recent move is as stated*/
    public Primitive isPrimitive(long position, byte mostrecentmove) {
        long origpos = position;
        if((position & 0x8000000000000000L) == 0)
        { // Check wins of 1s
            for(int i = 0; i < WIDTH; i++)
            {
                byte start = (byte)((HEIGHT+1)*(i+1) - 1);
                while((position & (1L<<start))==0) start--;
                position ^= (1L<<start);
            }
        }
        else
        { //Check wins of 0s
            for(int i = 0; i < WIDTH; i++)
            {
                byte start = (byte)((HEIGHT+1)*(i+1) - 1);
                byte start2 = (byte)(start+1);
                while((position & (1L<<start))==0) start--;
                position |= (1L<<(start2))-(1L << start);
            }
            position = ~position;
        }
        position &= (1L<<(WIDTH*(HEIGHT+1)))-1;
        //System.out.printf("%016X %n", position);
        //At this point, the position should contain 1s only on the places that match the most recent move.
        int x = mostrecentmove/(HEIGHT+1), y=mostrecentmove%(HEIGHT+1);
        //Vertical check
        if((y >= XINAROW-1)&& isawin(position, VERTICALWIN<<(mostrecentmove-(XINAROW-1)))) return Primitive.LOSS;
        //Horizontal and diagonal checks
        for(int i = 0; i < XINAROW; i++) {
            if (x >= i && (x + ((XINAROW - 1) - i)) < WIDTH) {
                //Horizontal check
                if (isawin(position, HORIZONTALWIN << (mostrecentmove - i * (HEIGHT + 1)))) return Primitive.LOSS;
                if (y >= i && (y + ((XINAROW - 1) - i)) < HEIGHT) {
                    //Up Diagonal check
                    if (isawin(position, UPDIAGWIN << (mostrecentmove - i * (HEIGHT + 2)))) return Primitive.LOSS;
                }
                if (y + i < HEIGHT && (y - ((XINAROW - 1) - i)) >= 0) {
                    //Down Diagonal check
                    if (isawin(position, DOWNDIAGWIN << (mostrecentmove - i * (HEIGHT)))) return Primitive.LOSS;
                }
            }
        }
        if((origpos & (INITIALPOSITION << HEIGHT)) == INITIALPOSITION << HEIGHT) {return Primitive.TIE;}
        return Primitive.NOT_PRIMITIVE;
    }

    private boolean isawin(long position, long pieces)
    {
        //System.out.printf("%016X %b %n", pieces, (position&pieces) == pieces);
        return (position&pieces) == pieces;
    }

    public int getSize() {
        return WIDTH*HEIGHT;
    }

    public long getHash(long position) {
        long newpos= position & 0x7FFFFFFFFFFFFFFFL;
        long oppositepos = 0;
        for(int i = 0; i < WIDTH; i++)
        {
            long val = (newpos>>>(i*(HEIGHT+1)))&((1L<<(HEIGHT+1))-1);
            oppositepos |= val<<((HEIGHT+1)*(WIDTH-i-1));
        }
        return Math.min(oppositepos, newpos);
    }

    public long hashToPosition(long hash) {
        byte emptyspots = 0;
        for(int j = 0; j < WIDTH; j++) {
            for (int i = HEIGHT; i >= 0; i--) {
                if((hash&(1L<<(j*(HEIGHT+1)+i))) == 0) emptyspots++;
                else break;
            }
        }
        return hash | ((long) ((getSize()-emptyspots)%2)) << 63;
    }

    private static void recurse(HashSet<Long> primitives, long position, Connect4Optimized c, String move) {
        if(move.length() < 5) System.out.println(move);
        byte[] moves = c.generateMoves(position);
        c.generateMoves(position);
        for(int i = 0; i < c.WIDTH;i++)
        {
            if(moves[i] == -1) return;
            long newpos = c.doMove(position, moves[i]);
            c.doMove(position, moves[i]);
            c.isPrimitive(newpos, moves[i]);
            if(c.isPrimitive(newpos, moves[i]) != Primitive.NOT_PRIMITIVE)
            {
                primitives.add(c.getHash(newpos));
                c.getHash(newpos);
            }
            else
            {
                recurse(primitives,newpos, c, move+Integer.toString(i));
            }
        }
    }
    public static void main(String[] args) {
        Connect4Optimized c = new Connect4Optimized();
        long pos = c.getStartingPositions();
        HashSet<Long> hashset = new HashSet<>();
        recurse(hashset, pos, c, "");
        System.out.printf("Number of primitives: %d", hashset.size());

    }

}


