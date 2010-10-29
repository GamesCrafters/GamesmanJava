

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Vector;

import org.junit.Test;

import edu.berkeley.gamesman.game.YGame;

public class TestY
{
    /**
     * @param innerTriangleSegmentsIn
     * @param outerRingSegmentsIn
     */
    @Test
    public void TestConstructor()
    {
        YGame yGame = new YGame(3, 6);
        assertTrue(yGame.getBoardSize() == 54); // not counting inner most
        // triangle
    }

    // General test case to print all index numbers, etc...
    @Test
    public void Test_Print_All()
    {

        int innerTriangleSegments = 2;
        int outerRingSegments = 4;

        int index = 0;
        int currentTriangleSegments = 3 * innerTriangleSegments;
        int triangle = 1;
        int triangleRings = (outerRingSegments - innerTriangleSegments) + 1;

        YGame yGame = new YGame(innerTriangleSegments, outerRingSegments);
        yGame.fillBoardWithPlayer('X');

        for (int j = 0; j < triangleRings; j++, currentTriangleSegments += 3, triangle++)
        {
            for (int k = 0; k < currentTriangleSegments; k++, index++)
            {
                Vector<YGame.Node> neighbors = yGame.getNeighbors(triangle,
                        index, 'X');
                System.out.print("\nTriangle " + triangle + " at index "
                        + index + ": ");
                int saw[] = new int[neighbors.size()];
                for (int i = 0; i < neighbors.size(); i++)
                    saw[i] = neighbors.get(i).getIndex();

                Arrays.sort(saw);
                for (int i = 0; i < neighbors.size(); i++)
                    System.out.print(saw[i] + " ");

            }
            System.out.println();
            index = 0;
        }
        assertEquals(0, 0);
    }

    @Test
    public void Test_2by4_Tri1_Ind0()
    {
        YGame yGame = new YGame(2, 4);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(1, 0, 'X');

        int exp[] =
        { 0, 1, 1, 5, 8 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_2by4_Tri1_Ind1()
    {
        YGame yGame = new YGame(2, 4);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(1, 1, 'X');

        int exp[] =
        { 0, 1, 2, 2, 3, 5 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_2by4_Tri1_Ind2()
    {
        YGame yGame = new YGame(2, 4);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(1, 2, 'X');

        int exp[] =
        { 1, 2, 3, 3, 4 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_2by4_Tri1_Ind3()
    {
        YGame yGame = new YGame(2, 4);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(1, 3, 'X');

        int exp[] =
        { 1, 2, 4, 4, 5, 5 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_2by4_Tri1_Ind4()
    {
        YGame yGame = new YGame(2, 4);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(1, 4, 'X');

        int exp[] =
        { 3, 5, 5, 6, 7 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_2by4_Tri1_Ind5()
    {
        YGame yGame = new YGame(2, 4);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(1, 5, 'X');

        int exp[] =
        { 0, 1, 3, 4, 7, 8 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_2by4_Tri2_Ind0()
    {
        YGame yGame = new YGame(2, 4);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(2, 0, 'X');

        int exp[] =
        { 0, 0, 1, 1, 8, 11 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_2by4_Tri2_Ind1()
    {
        YGame yGame = new YGame(2, 4);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(2, 1, 'X');

        int exp[] =
        { 0, 0, 1, 1, 2, 2 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_2by4_Tri2_Ind2()
    {
        YGame yGame = new YGame(2, 4);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(2, 2, 'X');

        int exp[] =
        { 1, 1, 2, 2, 3, 3 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_2by4_Tri2_Ind3()
    {
        YGame yGame = new YGame(2, 4);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(2, 3, 'X');

        int exp[] =
        { 2, 2, 3, 4, 4, 5 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_2by4_Tri2_Ind4()
    {
        YGame yGame = new YGame(2, 4);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(2, 4, 'X');

        int exp[] =
        { 2, 3, 3, 5, 5, 6 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_2by4_Tri2_Ind5()
    {
        YGame yGame = new YGame(2, 4);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(2, 5, 'X');

        int exp[] =
        { 3, 4, 4, 6, 6, 7 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_2by4_Tri2_Ind6()
    {
        YGame yGame = new YGame(2, 4);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(2, 6, 'X');

        int exp[] =
        { 4, 5, 7, 7, 8, 9 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_2by4_Tri2_Ind7()
    {
        YGame yGame = new YGame(2, 4);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(2, 7, 'X');

        int exp[] =
        { 4, 5, 6, 8, 9, 10 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_2by4_Tri2_Ind8()
    {
        YGame yGame = new YGame(2, 4);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(2, 8, 'X');

        int exp[] =
        { 0, 0, 5, 7, 10, 11 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_2by4_Tri3_Ind0()
    {
        YGame yGame = new YGame(2, 4);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(3, 0, 'X');

        int exp[] =
        { 0, 1, 11 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_2by4_Tri3_Ind1()
    {
        YGame yGame = new YGame(2, 4);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(3, 1, 'X');

        int exp[] =
        { 0, 0, 1, 2 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_2by4_Tri3_Ind2()
    {
        YGame yGame = new YGame(2, 4);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(3, 2, 'X');

        int exp[] =
        { 1, 1, 2, 3 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_2by4_Tri3_Ind3()
    {
        YGame yGame = new YGame(2, 4);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(3, 3, 'X');

        int exp[] =
        { 2, 2, 3, 4 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_2by4_Tri3_Ind4()
    {
        YGame yGame = new YGame(2, 4);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(3, 4, 'X');

        int exp[] =
        { 3, 3, 5 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_2by4_Tri3_Ind5()
    {
        YGame yGame = new YGame(2, 4);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(3, 5, 'X');

        int exp[] =
        { 3, 4, 4, 6 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_2by4_Tri3_Ind6()
    {
        YGame yGame = new YGame(2, 4);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(3, 6, 'X');

        int exp[] =
        { 4, 5, 5, 7 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_2by4_Tri3_Ind7()
    {
        YGame yGame = new YGame(2, 4);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(3, 7, 'X');

        int exp[] =
        { 5, 6, 6, 8 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_2by4_Tri3_Ind8()
    {
        YGame yGame = new YGame(2, 4);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(3, 8, 'X');

        int exp[] =
        { 6, 7, 9 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_2by4_Tri3_Ind9()
    {
        YGame yGame = new YGame(2, 4);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(3, 9, 'X');

        int exp[] =
        { 6, 7, 8, 10 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_2by4_Tri3_Ind10()
    {
        YGame yGame = new YGame(2, 4);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(3, 10, 'X');

        int exp[] =
        { 7, 8, 9, 11 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_2by4_Tri3_Ind11()
    {
        YGame yGame = new YGame(2, 4);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(3, 11, 'X');

        int exp[] =
        { 0, 0, 8, 10 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri1_Ind0()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(1, 0, 'X');

        int exp[] =
        { 0, 1, 1, 8, 11 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri1_Ind1()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(1, 1, 'X');

        int exp[] =
        { 1, 0, 0, 2, 2, 8 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri1_Ind2()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(1, 2, 'X');

        int exp[] =
        { 0, 1, 2, 3, 3, 4 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri1_Ind3()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(1, 3, 'X');

        int exp[] =
        { 2, 3, 4, 4, 5 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri1_Ind4()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(1, 4, 'X');

        int exp[] =
        { 0, 2, 3, 5, 5, 6 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri1_Ind5()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(1, 5, 'X');

        int exp[] =
        { 0, 4, 6, 6, 7, 7 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri1_Ind6()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(1, 6, 'X');

        int exp[] =
        { 5, 7, 7, 8, 9 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri1_Ind7()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(1, 7, 'X');

        int exp[] =
        { 0, 5, 6, 8, 9, 10 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri1_Ind8()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(1, 8, 'X');

        int exp[] =
        { 0, 0, 1, 7, 10, 11 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri2_Ind0()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(2, 0, 'X');

        int exp[] =
        { 1, 1, 0, 0, 11, 14 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri2_Ind1()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(2, 1, 'X');

        int exp[] =
        { 0, 0, 1, 1, 2, 2 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri2_Ind2()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(2, 2, 'X');

        int exp[] =
        { 1, 1, 2, 2, 3, 3 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri2_Ind3()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(2, 3, 'X');

        int exp[] =
        { 2, 2, 3, 3, 4, 4 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri2_Ind4()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(2, 4, 'X');

        int exp[] =
        { 3, 3, 4, 5, 5, 6 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri2_Ind5()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(2, 5, 'X');

        int exp[] =
        { 3, 4, 4, 6, 6, 7 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri2_Ind6()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(2, 6, 'X');

        int exp[] =
        { 4, 5, 5, 7, 7, 8 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri2_Ind7()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(2, 7, 'X');

        int exp[] =
        { 5, 6, 6, 8, 8, 9 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri2_Ind8()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(2, 8, 'X');

        int exp[] =
        { 6, 7, 9, 9, 10, 11 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri2_Ind9()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(2, 9, 'X');

        int exp[] =
        { 6, 7, 8, 10, 11, 12 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri2_Ind10()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(2, 10, 'X');
        int triangles[] =
        { 2, 1, 1, 2, 3, 3 };
        int indexes[] =
        { 11, 8, 7, 9, 12, 13 };
        int i;

        for (i = 0; i < 6; i++)
        {
            assertEquals(triangles[i], neighbors.get(i).getTriangle());
            assertEquals(indexes[i], neighbors.get(i).getIndex());
        }
    }

    @Test
    public void Test_3by6_Tri2_Ind11()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(2, 11, 'X');

        int exp[] =
        { 0, 0, 8, 10, 13, 14 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri3_Ind0()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(3, 0, 'X');

        int exp[] =
        { 0, 0, 1, 1, 14, 17 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri3_Ind1()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(3, 1, 'X');

        int exp[] =
        { 0, 0, 1, 1, 2, 2 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri3_Ind2()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(3, 2, 'X');

        int exp[] =
        { 1, 1, 2, 2, 3, 3 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri3_Ind3()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(3, 3, 'X');

        int exp[] =
        { 2, 2, 3, 3, 4, 4 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri3_Ind4()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(3, 4, 'X');

        int exp[] =
        { 3, 3, 4, 4, 5, 5 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri3_Ind5()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(3, 5, 'X');

        int exp[] =
        { 4, 4, 5, 6, 6, 7 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri3_Ind6()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(3, 6, 'X');

        int exp[] =
        { 4, 5, 5, 7, 7, 8 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri3_Ind7()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(3, 7, 'X');

        int exp[] =
        { 5, 6, 6, 8, 8, 9 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri3_Ind8()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(3, 8, 'X');

        int exp[] =
        { 6, 7, 7, 9, 9, 10 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri3_Ind9()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(3, 9, 'X');

        int exp[] =
        { 7, 8, 8, 10, 10, 11 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri3_Ind10()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(3, 10, 'X');

        int exp[] =
        { 8, 9, 11, 11, 12, 13 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri3_Ind11()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(3, 11, 'X');

        int exp[] =
        { 9, 8, 10, 12, 13 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri3_Ind12()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(3, 12, 'X');

        int exp[] =
        { 9, 10, 11, 13, 15, 14 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri3_Ind13()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(3, 13, 'X');

        int exp[] =
        { 10, 11, 14, 12, 15, 16 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri3_Ind14()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(3, 14, 'X');

        int exp[] =
        { 17, 16, 13, 11, 0, 0 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri4_Ind0()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(4, 0, 'X');

        int exp[] =
        { 0, 1, 17 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri4_Ind1()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(4, 1, 'X');

        int exp[] =
        { 0, 0, 1, 2 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri4_Ind2()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(4, 2, 'X');

        int exp[] =
        { 1, 1, 2, 3 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri4_Ind3()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(4, 3, 'X');

        int exp[] =
        { 2, 2, 3, 4 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri4_Ind4()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(4, 4, 'X');

        int exp[] =
        { 3, 3, 4, 5 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri4_Ind5()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(4, 5, 'X');

        int exp[] =
        { 4, 4, 5, 6 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri4_Ind6()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(4, 6, 'X');

        int exp[] =
        { 5, 5, 7 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri4_Ind7()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(4, 7, 'X');

        int exp[] =
        { 5, 6, 6, 8 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri4_Ind8()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(4, 8, 'X');

        int exp[] =
        { 6, 7, 7, 9 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri4_Ind9()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(4, 9, 'X');

        int exp[] =
        { 7, 8, 8, 10 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri4_Ind10()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(4, 10, 'X');

        int exp[] =
        { 8, 9, 9, 11 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri4_Ind11()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(4, 11, 'X');

        int exp[] =
        { 9, 10, 10, 12 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri4_Ind12()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(4, 12, 'X');

        int exp[] =
        { 10, 11, 13 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri4_Ind13()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(4, 13, 'X');

        int exp[] =
        { 12, 10, 11, 14 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri4_Ind14()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(4, 14, 'X');

        int exp[] =
        { 11, 12, 13, 15 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri4_Ind15()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(4, 15, 'X');

        int exp[] =
        { 12, 13, 14, 16 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri4_Ind16()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(4, 16, 'X');

        int exp[] =
        { 13, 14, 15, 17 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

    @Test
    public void Test_3by6_Tri4_Ind17()
    {
        YGame yGame = new YGame(3, 6);
        yGame.fillBoardWithPlayer('X');
        Vector<YGame.Node> neighbors = yGame.getNeighbors(4, 17, 'X');

        int exp[] =
        { 0, 0, 14, 16 };
        int saw[] = new int[neighbors.size()];

        for (int i = 0; i < neighbors.size(); i++)
            saw[i] = neighbors.get(i).getIndex();

        Arrays.sort(exp);
        Arrays.sort(saw);
        assertArrayEquals(exp, saw);
    }

}
