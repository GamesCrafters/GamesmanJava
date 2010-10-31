import static org.junit.Assert.assertEquals;

import java.util.Vector;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.game.YGame;
import edu.berkeley.gamesman.game.YGame.Node;

public class TestY
{
    /*
     * @Test public void TestConstructor() { YGame yGame = new YGame(3, 6); assertTrue(yGame.getBoardSize() == 54); // not counting
     * inner most // triangle }
     * 
     * 
     * // General test case to print all index numbers, etc...
     * 
     * @Test public void Test_Print_All() {
     * 
     * int innerTriangleSegments = 3; int outerRingSegments = 6;
     * 
     * int index = 0; int currentTriangleSegments = 3 * innerTriangleSegments; int triangle = 1; int triangleRings =
     * (outerRingSegments - innerTriangleSegments) + 1;
     * 
     * YGame yGame = new YGame(innerTriangleSegments, outerRingSegments); yGame.fillBoardWithPlayer('X');
     * 
     * for (int j = 0; j < triangleRings; j++, currentTriangleSegments += 3, triangle++) { for (int k = 0; k <
     * currentTriangleSegments; k++, index++) {
     * 
     * Vector<YGame.Node> neighbors = yGame.getNeighbors(triangle, index, 'X'); System.out.print("\nTriangle " + triangle +
     * " at index " + index + ": "); int saw[] = new int[neighbors.size()]; for (int i = 0; i < neighbors.size(); i++) saw[i] =
     * neighbors.get(i).getIndex();
     * 
     * Arrays.sort(saw); for (int i = 0; i < neighbors.size(); i++) System.out.print(saw[i] + " ");
     * 
     * } System.out.println(); index = 0; } assertEquals(0,0); }
     */

    /*
     * Array exp[][] is of the form { {TRIANGLE, INDEX} ...} IGOR: we always start from same-layer neighbor which is index+1%tr
     */

    private Configuration conf;
    private YGame ygame24;

    @Before
    public void setUp() throws ClassNotFoundException
    {
        conf = new Configuration("jobs/YGame24.job");
        ygame24 = (YGame) conf.getGame();
        ygame24.fillBoardWithPlayer('X');
    }

    @Test
    public void Test_2by4_Tri0_Ind0()
    {
        Vector<Node> neighbors = ygame24.getNeighbors(0, 0, 'X');
        int exp[][] =
        {
        { 0, 1 },
        { 0, 5 },
        { 1, 8 },
        { 1, 0 },
        { 1, 1 } };

        for (int i = 0; i < exp.length; i++)
        {
            assertEquals(exp[i][0], neighbors.get(i).getIndex());
            assertEquals(exp[i][1], neighbors.get(i).getTriangle());
        }
    }

    @Test
    public void Test_2by4_Tri0_Ind1()
    {
        Vector<Node> neighbors = ygame24.getNeighbors(0, 1, 'X');
        int exp[][] =
        {
        { 0, 2 },
        { 0, 3 },
        { 0, 5 },
        { 0, 0 },
        { 1, 1 },
        { 1, 2 } };

        for (int i = 0; i < exp.length; i++)
        {
            assertEquals(exp[i][0], neighbors.get(i).getIndex());
            assertEquals(exp[i][1], neighbors.get(i).getTriangle());
        }
    }

    @Test
    public void Test_2by4_Tri0_Ind2()
    {
        Vector<Node> neighbors = ygame24.getNeighbors(0, 2, 'X');
        int exp[][] =
        {
        { 0, 3 },
        { 0, 1 },
        { 1, 2 },
        { 1, 3 },
        { 1, 4 } };

        for (int i = 0; i < exp.length; i++)
        {
            assertEquals(exp[i][0], neighbors.get(i).getIndex());
            assertEquals(exp[i][1], neighbors.get(i).getTriangle());
        }
    }

    @Test
    public void Test_2by4_Tri0_Ind3()
    {
        Vector<Node> neighbors = ygame24.getNeighbors(0, 3, 'X');
        int exp[][] =
        {
        { 0, 4 },
        { 0, 5 },
        { 0, 1 },
        { 0, 2 },
        { 1, 4 },
        { 1, 5 } };

        for (int i = 0; i < exp.length; i++)
        {
            assertEquals(exp[i][0], neighbors.get(i).getIndex());
            assertEquals(exp[i][1], neighbors.get(i).getTriangle());
        }
    }

    @Test
    public void Test_2by4_Tri0_Ind4()
    {
        Vector<Node> neighbors = ygame24.getNeighbors(0, 4, 'X');
        int exp[][] =
        {
        { 0, 5 },
        { 0, 3 },
        { 1, 5 },
        { 1, 6 },
        { 1, 7 } };

        for (int i = 0; i < exp.length; i++)
        {
            assertEquals(exp[i][0], neighbors.get(i).getIndex());
            assertEquals(exp[i][1], neighbors.get(i).getTriangle());
        }
    }

    @Test
    public void Test_2by4_Tri0_Ind5()
    {
        Vector<Node> neighbors = ygame24.getNeighbors(0, 5, 'X');
        int exp[][] =
        {
        { 0, 0 },
        { 0, 1 },
        { 0, 3 },
        { 0, 4 },
        { 1, 7 },
        { 1, 8 } };

        for (int i = 0; i < exp.length; i++)
        {
            assertEquals(exp[i][0], neighbors.get(i).getIndex());
            assertEquals(exp[i][1], neighbors.get(i).getTriangle());
        }
    }

    @Test
    public void Test_2by4_Tri1_Ind0()
    {
        Vector<Node> neighbors = ygame24.getNeighbors(1, 0, 'X');
        int exp[][] =
        {
        { 1, 1 },
        { 0, 1 },
        { 1, 8 },
        { 2, 11 },
        { 2, 0 },
        { 2, 1 } };

        for (int i = 0; i < exp.length; i++)
        {
            assertEquals(exp[i][0], neighbors.get(i).getIndex());
            assertEquals(exp[i][1], neighbors.get(i).getTriangle());
        }
    }

    @Test
    public void Test_2by4_Tri1_Ind1()
    {
        Vector<Node> neighbors = ygame24.getNeighbors(1, 1, 'X');
        int exp[][] =
        {
        { 1, 2 },
        { 0, 1 },
        { 0, 0 },
        { 1, 0 },
        { 2, 1 },
        { 2, 2 } };

        for (int i = 0; i < exp.length; i++)
        {
            assertEquals(exp[i][0], neighbors.get(i).getIndex());
            assertEquals(exp[i][1], neighbors.get(i).getTriangle());
        }
    }

    @Test
    public void Test_2by4_Tri1_Ind2()
    {
        Vector<Node> neighbors = ygame24.getNeighbors(1, 2, 'X');
        int exp[][] =
        {
        { 1, 3 },
        { 0, 2 },
        { 0, 1 },
        { 1, 1 },
        { 2, 2 },
        { 2, 3 } };

        for (int i = 0; i < exp.length; i++)
        {
            assertEquals(exp[i][0], neighbors.get(i).getIndex());
            assertEquals(exp[i][1], neighbors.get(i).getTriangle());
        }
    }

    @Test
    public void Test_2by4_Tri1_Ind3()
    {
        Vector<Node> neighbors = ygame24.getNeighbors(1, 3, 'X');
        int exp[][] =
        {
        { 1, 4 },
        { 0, 2 },
        { 1, 2 },
        { 2, 3 },
        { 2, 4 },
        { 2, 5 } };

        for (int i = 0; i < exp.length; i++)
        {
            assertEquals(exp[i][0], neighbors.get(i).getIndex());
            assertEquals(exp[i][1], neighbors.get(i).getTriangle());
        }
    }

    @Test
    public void Test_2by4_Tri1_Ind4()
    {
        Vector<Node> neighbors = ygame24.getNeighbors(1, 4, 'X');
        int exp[][] =
        {
        { 1, 5 },
        { 0, 3 },
        { 0, 2 },
        { 1, 3 },
        { 2, 5 },
        { 2, 6 } };

        for (int i = 0; i < exp.length; i++)
        {
            assertEquals(exp[i][0], neighbors.get(i).getIndex());
            assertEquals(exp[i][1], neighbors.get(i).getTriangle());
        }
    }

    @Test
    public void Test_2by4_Tri1_Ind5()
    {
        Vector<Node> neighbors = ygame24.getNeighbors(1, 5, 'X');
        int exp[][] =
        {
        { 1, 6 },
        { 0, 4 },
        { 0, 3 },
        { 1, 4 },
        { 2, 6 },
        { 2, 7 } };

        for (int i = 0; i < exp.length; i++)
        {
            assertEquals(exp[i][0], neighbors.get(i).getIndex());
            assertEquals(exp[i][1], neighbors.get(i).getTriangle());
        }
    }

    @Test
    public void Test_2by4_Tri1_Ind6()
    {
        Vector<Node> neighbors = ygame24.getNeighbors(1, 6, 'X');
        int exp[][] =
        {
        { 1, 7 },
        { 0, 4 },
        { 1, 5 },
        { 2, 7 },
        { 2, 8 },
        { 2, 9 } };

        for (int i = 0; i < exp.length; i++)
        {
            assertEquals(exp[i][0], neighbors.get(i).getIndex());
            assertEquals(exp[i][1], neighbors.get(i).getTriangle());
        }
    }

    @Test
    public void Test_2by4_Tri1_Ind7()
    {
        Vector<Node> neighbors = ygame24.getNeighbors(1, 7, 'X');
        int exp[][] =
        {
        { 1, 8 },
        { 0, 5 },
        { 0, 4 },
        { 1, 6 },
        { 2, 9 },
        { 2, 10 } };

        for (int i = 0; i < exp.length; i++)
        {
            assertEquals(exp[i][0], neighbors.get(i).getIndex());
            assertEquals(exp[i][1], neighbors.get(i).getTriangle());
        }
    }

    @Test
    public void Test_2by4_Tri1_Ind8()
    {
        Vector<Node> neighbors = ygame24.getNeighbors(1, 8, 'X');
        int exp[][] =
        {
        { 1, 0 },
        { 0, 0 },
        { 0, 5 },
        { 1, 7 },
        { 2, 10 },
        { 2, 11 } };

        for (int i = 0; i < exp.length; i++)
        {
            assertEquals(exp[i][0], neighbors.get(i).getIndex());
            assertEquals(exp[i][1], neighbors.get(i).getTriangle());
        }
    }

    @Test
    public void Test_2by4_Tri2_Ind0()
    {
        Vector<Node> neighbors = ygame24.getNeighbors(2, 0, 'X');
        int exp[][] =
        {
        { 2, 1 },
        { 1, 0 },
        { 2, 11 } };

        for (int i = 0; i < exp.length; i++)
        {
            assertEquals(exp[i][0], neighbors.get(i).getIndex());
            assertEquals(exp[i][1], neighbors.get(i).getTriangle());
        }
    }

    @Test
    public void Test_2by4_Tri2_Ind1()
    {
        Vector<Node> neighbors = ygame24.getNeighbors(2, 1, 'X');
        int exp[][] =
        {
        { 2, 2 },
        { 1, 1 },
        { 1, 0 },
        { 2, 0 } };

        for (int i = 0; i < exp.length; i++)
        {
            assertEquals(exp[i][0], neighbors.get(i).getIndex());
            assertEquals(exp[i][1], neighbors.get(i).getTriangle());
        }
    }

    @Test
    public void Test_2by4_Tri2_Ind2()
    {
        Vector<Node> neighbors = ygame24.getNeighbors(2, 2, 'X');
        int exp[][] =
        {
        { 2, 3 },
        { 1, 2 },
        { 1, 1 },
        { 2, 1 } };

        for (int i = 0; i < exp.length; i++)
        {
            assertEquals(exp[i][0], neighbors.get(i).getIndex());
            assertEquals(exp[i][1], neighbors.get(i).getTriangle());
        }
    }

    @Test
    public void Test_2by4_Tri2_Ind3()
    {
        Vector<Node> neighbors = ygame24.getNeighbors(2, 3, 'X');
        int exp[][] =
        {
        { 2, 4 },
        { 1, 3 },
        { 1, 2 },
        { 2, 2 } };

        for (int i = 0; i < exp.length; i++)
        {
            assertEquals(exp[i][0], neighbors.get(i).getIndex());
            assertEquals(exp[i][1], neighbors.get(i).getTriangle());
        }
    }

    @Test
    public void Test_2by4_Tri2_Ind4()
    {
        Vector<Node> neighbors = ygame24.getNeighbors(2, 4, 'X');
        int exp[][] =
        {
        { 2, 5 },
        { 1, 3 },
        { 2, 3 } };

        for (int i = 0; i < exp.length; i++)
        {
            assertEquals(exp[i][0], neighbors.get(i).getIndex());
            assertEquals(exp[i][1], neighbors.get(i).getTriangle());
        }
    }

    @Test
    public void Test_2by4_Tri2_Ind5()
    {
        Vector<Node> neighbors = ygame24.getNeighbors(2, 5, 'X');
        int exp[][] =
        {
        { 2, 6 },
        { 1, 4 },
        { 1, 3 },
        { 2, 4 } };

        for (int i = 0; i < exp.length; i++)
        {
            assertEquals(exp[i][0], neighbors.get(i).getIndex());
            assertEquals(exp[i][1], neighbors.get(i).getTriangle());
        }
    }

    @Test
    public void Test_2by4_Tri2_Ind6()
    {
        Vector<Node> neighbors = ygame24.getNeighbors(2, 6, 'X');
        int exp[][] =
        {
        { 2, 7 },
        { 1, 5 },
        { 1, 4 },
        { 2, 5 } };

        for (int i = 0; i < exp.length; i++)
        {
            assertEquals(exp[i][0], neighbors.get(i).getIndex());
            assertEquals(exp[i][1], neighbors.get(i).getTriangle());
        }
    }

    @Test
    public void Test_2by4_Tri2_Ind7()
    {
        Vector<Node> neighbors = ygame24.getNeighbors(2, 7, 'X');
        int exp[][] =
        {
        { 2, 8 },
        { 1, 6 },
        { 1, 5 },
        { 2, 6 } };

        for (int i = 0; i < exp.length; i++)
        {
            assertEquals(exp[i][0], neighbors.get(i).getIndex());
            assertEquals(exp[i][1], neighbors.get(i).getTriangle());
        }
    }

    @Test
    public void Test_2by4_Tri2_Ind8()
    {
        Vector<Node> neighbors = ygame24.getNeighbors(2, 8, 'X');
        int exp[][] =
        {
        { 2, 9 },
        { 1, 6 },
        { 2, 7 } };

        for (int i = 0; i < exp.length; i++)
        {
            assertEquals(exp[i][0], neighbors.get(i).getIndex());
            assertEquals(exp[i][1], neighbors.get(i).getTriangle());
        }
    }

    @Test
    public void Test_2by4_Tri2_Ind9()
    {
        Vector<Node> neighbors = ygame24.getNeighbors(2, 9, 'X');
        int exp[][] =
        {
        { 2, 10 },
        { 1, 7 },
        { 1, 6 },
        { 2, 8 } };

        for (int i = 0; i < exp.length; i++)
        {
            assertEquals(exp[i][0], neighbors.get(i).getIndex());
            assertEquals(exp[i][1], neighbors.get(i).getTriangle());
        }
    }

    @Test
    public void Test_2by4_Tri2_Ind10()
    {
        Vector<Node> neighbors = ygame24.getNeighbors(2, 10, 'X');
        int exp[][] =
        {
        { 2, 11 },
        { 1, 8 },
        { 1, 7 },
        { 2, 9 } };

        for (int i = 0; i < exp.length; i++)
        {
            assertEquals(exp[i][0], neighbors.get(i).getIndex());
            assertEquals(exp[i][1], neighbors.get(i).getTriangle());
        }
    }

    @Test
    public void Test_2by4_Tri2_Ind11()
    {
        Vector<Node> neighbors = ygame24.getNeighbors(2, 11, 'X');
        int exp[][] =
        {
        { 2, 0 },
        { 1, 0 },
        { 1, 8 },
        { 2, 10 } };

        for (int i = 0; i < exp.length; i++)
        {
            assertEquals(exp[i][0], neighbors.get(i).getIndex());
            assertEquals(exp[i][1], neighbors.get(i).getTriangle());
        }
    }

    @AfterClass
    public static void oneTimeTearDown()
    {
    }
}
