package edu.berkeley.gamesman.hasher.symmetry; 
/**
 * Abstract class for RectanularSymmetryFinder and SquareSymmetryFinder.
 * Creates public interface for using thesymmetry tools, but leaves
 * board specific methods to be implemented.
 *
 * findLargestTransformation is given a Mapping (index, row, and column)
 * and returns the largest index that it could transform into i.e.
 * a Mapping of the bottom right corner piece would return 3, the largest
 * index of a corner (bottom left).
 */
public abstract class SymmetryFinder {

  protected int height;
  protected int width;
  protected int[][] indices;
  protected boolean rotation = true;
  protected boolean horizontal = true;
  protected boolean vertical = true;

  public SymmetryFinder(int size) {
    this.width = size;
    this.height = size;
    indices = new int[size][size];
  }

  public SymmetryFinder(int height, int width) {
    this.height = height;
    this.width = width;
    indices = new int[height][width];
  }

  protected int findLargestTransformation(Mapping mapping) {
    int index = mapping.index;
    int row = mapping.row;
    int col = mapping.column;
    //identity transformation
    int max = index;

    if(rotation) {
      //rotate right 90
      max = Math.max(max, indices[col][width - row - 1]);
      //rotate right 180
      max = Math.max(max, indices[height - row - 1][width - col - 1]);
      //rotate right 270
      max = Math.max(max, indices[height - col - 1][row]);
    }

    if(horizontal) {
      //horizontal reflection
      max = Math.max(max, indices[row][width - col - 1]);
    }

    if(vertical) {
      //vertical reflection
      max = Math.max(max, indices[height - row - 1][col]);   
    }

    if(rotation && (horizontal || vertical)) {
      //rotate right 90, horizontal reflection, or
      //rotate right 270, vertical reflection
      max = Math.max(max, indices[col][row]); 
      //rotate right 90, vertical reflection or
      //rotate right 270, horizontal reflection
      max = Math.max(max, indices[height - col - 1][width - row - 1]);
    }

    return max;
  }

  abstract int[][] getBoardIndices();

  abstract int[] getFixedPoints();

  protected class Mapping {

    protected int index;
    protected int row;
    protected int column;
  
    public Mapping(int index, int row, int column) {
      this.index = index;
      this.row = row;
      this.column = column;
    }
  }
}
