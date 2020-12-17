package Helpers;

public enum Piece {
    EMPTY,
    RED,
    BLUE;

    public Piece opposite() {
        switch(this) {
            case RED: return BLUE;
            case BLUE: return RED;
            default: throw new IllegalStateException("This should never happen: " + this + " has no opposite.");
        }
    }
}
