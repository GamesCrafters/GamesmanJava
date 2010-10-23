
package edu.berkeley.gamesman.testing;

import static org.junit.Assert.assertTrue;

import java.util.Vector;

import org.junit.Test;

import edu.berkeley.gamesman.game.YGame;

public class TestY
{
    @Test
    public void TestConstructor()
    {
        YGame yGame = new YGame( 4, 8 );

        assertTrue( yGame.getBoardSize() == 93 );
    }

    @Test
    public void TestNeighborCounts()
    {
        YGame yGame = new YGame( 2, 4 );

        yGame.fillBoardWithPlayer( 'X' );

        Vector< YGame.Node > neighbors = yGame.getNeighbors( false, 2, 0, 'X' );

        assertTrue( neighbors.size() == 3 );
    }
}
