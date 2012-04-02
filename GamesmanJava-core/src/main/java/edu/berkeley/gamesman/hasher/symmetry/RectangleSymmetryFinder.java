package edu.berkeley.gamesman.hasher.symmetry; 

import java.util.ArrayList;

public class RectangleSymmetryFinder extends SymmetryFinder {

  private ArrayList<Mapping> mappings;
  private int[] fixedPoints;

  public RectangleSymmetryFinder(int height, int width) {
    super(height, width);
    rotation = false;
    vertical = false;
    mappings = new ArrayList<Mapping>();

    createIndices();
  }

  public int[][] getBoardIndices() {
    return indices;
  }

  public int[] getFixedPoints() {
    if (fixedPoints != null)
      return fixedPoints;

    fixedPoints = new int[width*height];

    int max = 0;
    for(int j = fixedPoints.length - 1; j >= 0; j--) {
      int i = fixedPoints.length - j - 1;
      
      max = Math.max(findLargestTransformation(mappings.get(i)), max);

      fixedPoints[j] = max;
    }

    return fixedPoints;
  }

  private void createIndices() {
    int index = 0;
    int inc = width - 1;
    int j = width - 1;
    int cols = 0;
    while(cols < width && j >= 0 && j < width) {
      for(int i = height - 1; i >= 0; i--) {
        addMapping(i, j, index);
        index++;
      }
      j += (int)(Math.pow(-1, cols + 1) * inc);
      inc--;
      cols++;
    }
  }

  private void addMapping(int row, int column, int index) {
    mappings.add(new Mapping(index, row, column));
    indices[row][column] = index;
  }
}
