package edu.berkeley.gamesman.hadoop.game.connect4;

import java.util.Collection;

import junit.framework.Assert;

import org.apache.hadoop.conf.Configuration;
import org.junit.Test;

import edu.berkeley.gamesman.hadoop.ranges.Range;

public class C4RangeTester {
	@Test
	public void testRangeSize() {
		Configuration conf = new Configuration();
		Connect4 c4 = new Connect4();

		// Test default = 10
		c4.setConf(conf);
		Collection<Range<C4State>> roots = c4.getRoots();
		assert roots.size() == 1;
		Range<C4State> root = roots.iterator().next();
		Assert.assertEquals(root.length(), 11);

		// Test for 15
		conf.setInt("gamesman.game.variance.length", 15);
		c4.setConf(conf);
		roots = c4.getRoots();
		assert roots.size() == 1;
		root = roots.iterator().next();
		Assert.assertEquals(root.length(), 6);
	}
}
