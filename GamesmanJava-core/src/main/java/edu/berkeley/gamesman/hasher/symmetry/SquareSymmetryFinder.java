package edu.berkeley.gamesman.hasher.symmetry; 

import java.util.ArrayList;

public class SquareSymmetryFinder extends SymmetryFinder {

  private ArrayList<Mapping> mappings;
  private int[] fixedPoints;
  private int size;

  public SquareSymmetryFinder(int size) {
    super(size);//me
    mappings = new ArrayList<Mapping>();
    this.size = size;
    createIndices(0, 0);
  }

  public int[][] getBoardIndices() {
    return indices;
  }

  public int[] getFixedPoints() {
    if (fixedPoints != null)
      return fixedPoints;

    fixedPoints = new int[size*size];

    int max = 0;
    for(int j = fixedPoints.length - 1; j >= 0; j--) {
      int i = fixedPoints.length - j - 1;
      
      max = Math.max(findLargestTransformation(mappings.get(i)), max);

      fixedPoints[j] = max;
    }

    return fixedPoints;
  }

  private void createIndices(int soffset, int ioffset) {
    if (size - 2 * soffset == 1)
      addMapping(soffset, soffset, ioffset);
    else if (size - 2 * soffset > 1) {
      //fill in corners
      addMapping(soffset, soffset, ioffset);
      ioffset++;
      addMapping(soffset, size - soffset - 1, ioffset);
      ioffset++;
      addMapping(size - soffset - 1, size - soffset - 1, ioffset);
      ioffset++;
      addMapping(size - soffset - 1, soffset, ioffset);
      ioffset++;

      //fill in edges
      if (size - 2 * soffset - 2 > 1) {
        for(int i = 1; i < (size - 2 * soffset)/2; i++) {
          //fill in each set of edge cells
          addMapping(soffset, soffset + i, ioffset);
          ioffset++;
          addMapping(soffset, size - soffset - i - 1, ioffset);
          ioffset++;       
          addMapping(soffset + i, size - soffset - 1, ioffset);
          ioffset++;       
          addMapping(size - soffset - i - 1, size - soffset - 1, ioffset);
          ioffset++; 
          addMapping(size - soffset - 1, size - soffset - i - 1, ioffset);
          ioffset++; 
          addMapping(size - soffset - 1, soffset + i, ioffset);
          ioffset++; 
          addMapping(size - soffset - i - 1, soffset, ioffset);
          ioffset++; 
          addMapping(soffset + i, soffset, ioffset);
          ioffset++; 
        }
      }

      //fill in cross
      if (size - 2 * soffset > 2 && size % 2 == 1) {
        addMapping(soffset, size/2, ioffset);
        ioffset++;
        addMapping(size/2, size - soffset - 1, ioffset);
        ioffset++;
        addMapping(size - soffset - 1, size/2, ioffset);
        ioffset++;
        addMapping(size/2, soffset, ioffset);
        ioffset++;
      }

      //fill in center
      createIndices(soffset + 1, ioffset);
    }
  }

  private void addMapping(int row, int column, int index) {
    mappings.add(new Mapping(index, row, column));
    indices[row][column] = index;
  }
}
