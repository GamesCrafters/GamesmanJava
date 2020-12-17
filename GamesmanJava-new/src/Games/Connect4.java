package Games;

import Helpers.Piece;
import Helpers.Primitive;
import Helpers.Tuple;

import java.io.Serializable;
import java.util.*;

public class Connect4 implements Serializable {
    int width;
    int height;
    int win;
    Piece[] gameStartingPosition;

    /** Pieces stored in column major order, starting from bottom right*/
    public Connect4(int w, int h, int wi) {
        width = w;
        height = h;
        win = wi;
        gameStartingPosition = new Piece[w*h];
        Arrays.fill(gameStartingPosition, Piece.EMPTY);
    }

    public Piece[] getStartingPositions() {
        return gameStartingPosition;
    }


    public Piece[] doMove(Piece[] position, int move, Piece p) {
        Piece[] newPosition = new Piece[getSize()];
        System.arraycopy(position, 0, newPosition, 0, position.length);
        newPosition[move] = p;
        return newPosition;
    }


    public List<Integer> generateMoves(Piece[] position) {
        List<Integer> ret = new ArrayList<>();
        for (int i = 0; i < getSize(); i++) {
            if (position[i] == Piece.EMPTY) {
                ret.add(i);
                i = (i + height) / height * height; //Move to next multiple of height
                i -= 1;
            }
        }
        return ret;
    }

    public Tuple<Primitive, Integer> isPrimitive(Piece[] position, Piece placed) {
        boolean full = true;
        for (int column = 0; column < width; column++) {
            int row = height - 1;
            Piece atP = position[row + column * height];
            if (atP == Piece.EMPTY) {
                full = false;
            }
            while(atP == Piece.EMPTY && row > 0) {
                row --;
                atP = position[row + column * height];
            }
            if (atP != placed) {
                continue;
            }
            //Now we now we are at a piece of placed type on top of column
            // Vertical wins
            if (row - win + 1 >= 0) {
                for (int r = row - 1; r >= row - win + 1; r--) {
                    if (position[r + column*height] != placed) {
                        break;
                    }
                    if (r == row - win + 1) {
                        return new Tuple<>(Primitive.LOSS, 0);
                    }
                }
            }

            //Horizontal wins
            if (win <= width) {
                int in_a_row = 1;
                for (int c = column - 1; c >=0; c--) {
                    if (position[row + c*height] != placed) {
                        break;
                    } else {
                        in_a_row++;
                    }
                }
                for (int c = column + 1; c < width; c++) {
                    if (position[row + c*height] != placed) {
                        break;
                    } else {
                        in_a_row++;
                    }
                }
                if (in_a_row >= win) {
                    return new Tuple<>(Primitive.LOSS, 0);
                }
            }

            // Diag Left High
            if (win <= width && win <= height) {
                int in_a_diag = 1;
                int found = row + column*height;
                for (int f = found + 1 + height; f < width*height && f % height != 0; f += 1 + height) {
                    if (position[f] != placed) {
                        break;
                    } else {
                        in_a_diag++;
                    }
                }
                for (int f = found - 1 - height; f >= 0 && (f + 1) % height != 0; f -= 1 + height) {
                    if (position[f] != placed) {
                        break;
                    } else {
                        in_a_diag++;
                    }
                }
                if (in_a_diag >= win) {
                    return new Tuple<>(Primitive.LOSS, 0);
                }
            }

            //Diag Right High
            if (win <= width && win <= height) {
                int in_a_diag = 1;
                int found = row + column*height;
                for (int f = found + 1 - height; f >= 0 && f % height != 0; f += 1 - height) {
                    if (position[f] != placed) {
                        break;
                    } else {
                        in_a_diag++;
                    }
                }
                for (int f = found - 1 + height; f < width*height && (f + 1) % height != 0; f -= 1 - height) {
                    if (position[f] != placed) {
                        break;
                    } else {
                        in_a_diag++;
                    }
                }
                if (in_a_diag >= win) {
                    return new Tuple<>(Primitive.LOSS, 0);
                }
            }
        }
        if (full) {
            return new Tuple<>(Primitive.TIE, 0);
        } else {
            return new Tuple<>(Primitive.NOT_PRIMITIVE, 0);
        }
    }

    // The same as isPrimitive(position, placed) except we only check the one location we need to
    public Tuple<Primitive, Integer> isPrimitive(Piece[] position, Piece placed, int location) {
        if (location == -1) {
            return isPrimitive(position, placed);
        }
        boolean full = true;
        for (int column = 0; column < width; column++) {
            int row = height - 1;
            Piece atP = position[row + column * height];
            if (atP == Piece.EMPTY) {
                full = false;
                break;
            }
        }
        int row = location % height;
        int column = location / height;
        // Vertical wins
        if (row - win + 1 >= 0) {
            for (int r = row - 1; r >= row - win + 1; r--) {
                if (position[r + column*height] != placed) {
                    break;
                }
                if (r == row - win + 1) {
                    return new Tuple<>(Primitive.LOSS, 0);
                }
            }
        }

        //Horizontal wins
        if (win <= width) {
            int in_a_row = 1;
            for (int c = column - 1; c >=0; c--) {
                if (position[row + c*height] != placed) {
                    break;
                } else {
                    in_a_row++;
                }
            }
            for (int c = column + 1; c < width; c++) {
                if (position[row + c*height] != placed) {
                    break;
                } else {
                    in_a_row++;
                }
            }
            if (in_a_row >= win) {
                return new Tuple<>(Primitive.LOSS, 0);
            }
        }

        // Diag Left High
        if (win <= width && win <= height) {
            int in_a_diag = 1;
            int found = row + column*height;
            for (int f = found + 1 + height; f < width*height && f % height != 0; f += 1 + height) {
                if (position[f] != placed) {
                    break;
                } else {
                    in_a_diag++;
                }
            }
            for (int f = found - 1 - height; f >= 0 && (f + 1) % height != 0; f -= 1 + height) {
                if (position[f] != placed) {
                    break;
                } else {
                    in_a_diag++;
                }
            }
            if (in_a_diag >= win) {
                return new Tuple<>(Primitive.LOSS, 0);
            }
        }

        //Diag Right High
        if (win <= width && win <= height) {
            int in_a_diag = 1;
            int found = row + column*height;
            for (int f = found + 1 - height; f >= 0 && f % height != 0; f += 1 - height) {
                if (position[f] != placed) {
                    break;
                } else {
                    in_a_diag++;
                }
            }
            for (int f = found - 1 + height; f < width*height && (f + 1) % height != 0; f -= 1 - height) {
                if (position[f] != placed) {
                    break;
                } else {
                    in_a_diag++;
                }
            }
            if (in_a_diag >= win) {
                return new Tuple<>(Primitive.LOSS, 0);
            }

        }
        if (full) {
            return new Tuple<>(Primitive.TIE, 0);
        } else {
            return new Tuple<>(Primitive.NOT_PRIMITIVE, 0);
        }
    }


    public int symMove(int move) {
        return (move % height) + (width - (move / height) - 1) * height;
    }

    public int getSize() {
        return width*height;
    }

    public void printBoard(Piece[] board) {
        StringBuilder stb = new StringBuilder();
        for (int r = height - 1; r >= 0; r--) {
            for (int c = width - 1; c >= 0; c--) {
                switch(board[r + c * height]) {
                    case RED:
                        stb.append("|O");
                        break;
                    case BLUE:
                        stb.append("|X");
                        break;
                    case EMPTY:
                        stb.append("| ");
                }
            }
            stb.append("|\n");
        }
        for (int c = width - 1; c >= 0; c--) {
            stb.append(' ');
            stb.append(c + 1);
        }

        System.out.println(stb.toString());
    }



}


