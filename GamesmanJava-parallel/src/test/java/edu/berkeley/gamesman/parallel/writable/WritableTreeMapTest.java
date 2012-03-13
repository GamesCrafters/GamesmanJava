package edu.berkeley.gamesman.parallel.writable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import junit.framework.Assert;

import org.apache.hadoop.io.IntWritable;
import org.junit.Test;

import edu.berkeley.gamesman.util.qll.Factory;
import edu.berkeley.gamesman.util.qll.Pool;
import edu.berkeley.gamesman.util.qll.QLLFactory;

public class WritableTreeMapTest {

	private static final int[][] pairs = new int[204][2];
	static {
		pairs[0][0] = 0;
		pairs[0][1] = 38;
		pairs[1][0] = 20;
		pairs[1][1] = 105;
		pairs[2][0] = 21;
		pairs[2][1] = 1;
		pairs[3][0] = 1000;
		pairs[3][1] = 2;
		for (int i = 0; i < 200; i++) {
			pairs[i + 4][0] = (1 << 17) + i;
			pairs[i + 4][1] = i;
		}
	}

	@Test
	public void testMap() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		QLLFactory<IntWritable> qFact = new QLLFactory<IntWritable>();
		Pool<IntWritable> pool = new Pool<IntWritable>(
				new Factory<IntWritable>() {

					@Override
					public IntWritable newObject() {
						return new IntWritable();
					}

					@Override
					public void reset(IntWritable t) {
					}
				});
		WritableTreeMap<IntWritable> wtm = new WritableTreeMap<IntWritable>(
				qFact, pool, qFact, pool);
		int[][] pairs = new int[204][2];
		pairs[0][0] = 0;
		pairs[0][1] = 38;
		pairs[1][0] = 20;
		pairs[1][1] = 105;
		pairs[2][0] = 21;
		pairs[2][1] = 1;
		pairs[3][0] = 1000;
		pairs[3][1] = 2;
		for (int i = 0; i < 200; i++) {
			pairs[i + 4][0] = (1 << 17) + i;
			pairs[i + 4][1] = i;
		}
		for (int i = 0; i < 204; i++) {
			wtm.add(pairs[i][0]).set(pairs[i][1]);
		}
		wtm.write(dos);
		dos.close();
		byte[] b = baos.toByteArray();
		WritableTreeMap<IntWritable> wtm2 = new WritableTreeMap<IntWritable>(
				qFact, pool, qFact, pool);
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(b));
		wtm2.readFields(dis);
		for (int i = 0; i < 204; i++) {
			Assert.assertEquals(pairs[i][0], wtm2.peekNext());
			Assert.assertEquals(pairs[i][1], wtm2.getNext(pairs[i][0]).get());
		}
	}

	@Test
	public void testJump() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		QLLFactory<IntWritable> qFact = new QLLFactory<IntWritable>();
		Pool<IntWritable> pool = new Pool<IntWritable>(
				new Factory<IntWritable>() {

					@Override
					public IntWritable newObject() {
						return new IntWritable();
					}

					@Override
					public void reset(IntWritable t) {
					}
				});
		JumpList wtm = new JumpList(qFact, pool);
		for (int i = 0; i < 204; i++) {
			wtm.add(pairs[i][0]);
		}
		wtm.write(dos);
		dos.close();
		Assert.assertTrue(wtm.noWaiting());
		byte[] b = baos.toByteArray();
		JumpList wtm2 = new JumpList(qFact, pool);
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(b));
		wtm2.readFields(dis);
		for (int i = 0; i < 204; i++) {
			Assert.assertEquals(pairs[i][0], wtm2.next());
		}
	}
}
