package SQL;

import Zobrist.Primitive;

import java.util.List;

public abstract class Game {

    public Piece[] startingPosition;

    public enum Piece {EMPTY, RED, BLUE}

    public abstract Piece[] doMove(Piece[] position, int move, Piece p);

    public abstract List<Integer> generateMoves(Piece[] position);

    public abstract Primitive isPrimitive(Piece[] position);

    public Piece[] getStartingPosition() {
        return startingPosition;
    }

    public abstract int hash(Piece[] position);

}
