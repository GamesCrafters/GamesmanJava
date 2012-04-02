package edu.berkeley.gamesman.hasher.symmetry; 

public class RectangleTest {

  public static void main(String[] args) {
    System.out.println("Testing Index Generation");
    
    RectangleSymmetryFinder s = new RectangleSymmetryFinder(3, 2);
    int[][] indices = s.getBoardIndices();
    int[][] answer = getCorrectBoardIndices(3, 2);
    for(int i = 0; i < 3; i++) {
      for(int j = 0; j < 2; j++) {
        if(answer[i][j] != indices[i][j])
          System.out.print(answer[i][j] - indices[i][j] + "\t");
        else
          System.out.print(".\t");
      }
      System.out.println("");
    }
    System.out.println("\n");

    s = new RectangleSymmetryFinder(4, 5);
    indices = s.getBoardIndices();
    answer = getCorrectBoardIndices(4, 5);
    for(int i = 0; i < 4; i++) {
      for(int j = 0; j < 5; j++) {
        if(answer[i][j] != indices[i][j])
          System.out.print(answer[i][j] - indices[i][j] + "\t");
        else
          System.out.print(".\t");
      }
      System.out.println("");
    }
    System.out.println("\n");

    System.out.println("Testing Mapping");

    s = new RectangleSymmetryFinder(3, 2);
    int[] points = s.getFixedPoints();
    int[] answers = getCorrectFixedPoints(3, 2);
    for(int i = 0; i < 3*2; i++) {
      if(points[i] != answers[i])
        System.out.print(answers[i] - points[i] + " ");
      else
        System.out.print(".");
    }
    System.out.println("\n");

    s = new RectangleSymmetryFinder(4, 5);
    points = s.getFixedPoints();
    answers = getCorrectFixedPoints(4, 5);
    for(int i = 0; i < 4*5; i++) {
      if(points[i] != answers[i])
        System.out.print(answers[i] - points[i] + " ");
      else
        System.out.print(".");
    }
    System.out.println("\n");
  }

  private static int[] getCorrectFixedPoints(int r, int c) {
    int[] f32 = {5,5,5,5,4,3};

    int[] f45 = {19,18,17,16,15,15,15,15,15,14,13,12,7,7,7,7,7,6,5,4};
  
    int[] shit = new int[100];

    if (r == 3 && c == 2)
      return f32;
    else if (r == 4 && c == 5)
      return f45;
    else
      return shit;
  }

  private static int[][] getCorrectBoardIndices(int r, int c) {
    int[][] b32 = {{5,2},
                   {4,1},
                   {3,0}};

    int[][] b45 = {{7,15,19,11,3},
                   {6,14,18,10,2},
                   {5,13,17,9,1},
                   {4,12,16,8,0}};
  
    int[][] shit = new int[100][100];

    if (r == 3 && c == 2)
      return b32;
    else if (r == 4 && c == 5)
      return b45;
    else
      return shit;
  }
}
