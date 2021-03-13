package Games;


import Helpers.Primitive;

import java.io.Serializable;
import java.util.*;

public class Connect4Optimized implements Serializable {
    final int WIDTH = 7;
    final int HEIGHT = 6;
    final int XINAROW = 4;
    final long INITIALPOSITION;
    long position;

    /** Each sequence of 7 bits stores a column as follows:
     * Everything up to and including the first 1 are empty. After that, 0 is yellow, 1 is red
     * For example, the string 0b0001011 means ----YRR. Columns are stored left to right or right to left,
     * depending on which yields a lower hash. Bit 63 is used to store the current player.*/
    public Connect4Optimized() {
        long l = 0;
        for(int i = 0; i < WIDTH; i++) l |= 1L<<((HEIGHT+1)*i);
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

        long newpos = (position ^ 0x8000000000000000L)+(1L<<(move+(position >>> 63)));
        long oppositepos = newpos & 0x8000000000000000L;
        for(int i = 0; i < WIDTH; i++)
        {
            long val = (newpos>>>(i*(HEIGHT+1)))&((1L<<(HEIGHT+1))-1);
            oppositepos |= val<<((HEIGHT+1)*(WIDTH-i-1));
        }
        return Math.min(oppositepos, newpos);
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
        //TODO Check if loss
        if((position & INITIALPOSITION << HEIGHT) == INITIALPOSITION << HEIGHT) return Primitive.TIE;
        return Primitive.NOT_PRIMITIVE;
    }


    public int getSize() {
        return WIDTH*HEIGHT;
    }

    public static void main(String[] args) {
        Connect4Optimized c = new Connect4Optimized();
        long pos = c.getStartingPositions();
        System.out.printf("Starting position: 0x%016X %n", c.getStartingPositions());
        byte[] moves = c.generateMoves(pos);
        while(moves[6] != -1) {
            pos = c.doMove(pos, moves[6]);
            System.out.printf("New position: 0x%016X %n", pos);
            moves = c.generateMoves(pos);
            System.out.println(moves[6]);
        }
    }

}


