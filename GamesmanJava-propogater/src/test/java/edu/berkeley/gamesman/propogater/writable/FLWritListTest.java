package edu.berkeley.gamesman.propogater.writable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import edu.berkeley.gamesman.propogater.writable.list.FLWritList;

public class FLWritListTest {
	private static class BWritable implements FixedLengthWritable {
		private final int bSize;
		private final byte[] bytes;

		public BWritable(int bSize) {
			this.bSize = bSize;
			bytes = new byte[bSize];
		}

		@Override
		public void write(DataOutput out) throws IOException {
			out.write(bytes);
		}

		@Override
		public void readFields(DataInput in) throws IOException {
			in.readFully(bytes);
		}

		@Override
		public int size() {
			return bSize;
		}

	}

	@Test
	public void testAddAndSerialize() throws IOException {
		byte[] serialized = createSerialized();
		FLWritList<BWritable> l2 = new FLWritList<BWritable>(new BWritable(3));
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(
				serialized));
		l2.readFields(dis);
		Assert.assertEquals(4, l2.length());
		for (int i = 0; i < 4; i++) {
			Assert.assertEquals(i, l2.get(i).bytes[0]);
		}
	}

	private byte[] createSerialized() throws IOException {
		FLWritList<BWritable> l1 = new FLWritList<BWritable>(new BWritable(3));
		l1.reset(true);
		BWritable bw = new BWritable(3);
		bw.bytes[0] = 0;
		l1.add(bw);
		bw.bytes[0] = 1;
		l1.add(bw);
		bw.bytes[0] = 2;
		l1.add(bw);
		bw.bytes[0] = 3;
		l1.add(bw);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		l1.write(dos);
		byte[] serialized = baos.toByteArray();
		return serialized;
	}

	@Test
	public void testWriteBack() throws IOException {
		byte[] serialized = createSerialized();
		FLWritList<BWritable> l2 = new FLWritList<BWritable>(new BWritable(3));
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(
				serialized));
		l2.readFields(dis);
		BWritable g = l2.get(2);
		g.bytes[1] = 1;
		l2.writeBack(2, g);
		ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
		l2.write(new DataOutputStream(baos2));
		dis = new DataInputStream(new ByteArrayInputStream(baos2.toByteArray()));
		FLWritList<BWritable> l3 = new FLWritList<BWritable>(new BWritable(3));
		l3.readFields(dis);
		Assert.assertEquals(1, l3.get(2).bytes[1]);
	}
}