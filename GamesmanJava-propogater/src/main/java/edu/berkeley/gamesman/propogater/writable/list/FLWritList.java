package edu.berkeley.gamesman.propogater.writable.list;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.hadoop.io.Writable;

import edu.berkeley.gamesman.propogater.writable.ByteArrayDOutputStream;
import edu.berkeley.gamesman.propogater.writable.FixedLengthWritable;

public class FLWritList<T extends FixedLengthWritable> implements Writable,
		WritList<T> {
	private final byte[] zArr;
	private byte[] myBytes;
	private int length = 0;
	private final int objLengthBits;
	private final int objLength;
	private final ByteArrayDOutputStream baos = new ByteArrayDOutputStream();
	private final DataOutputStream dos = new DataOutputStream(baos);
	private final ByteArrayDOutputStream backWriter = new ByteArrayDOutputStream();
	private final DataOutputStream backDos = new DataOutputStream(backWriter);
	private ByteArrayInputStream bais;
	private DataInputStream dis;
	private boolean adding = false;
	private final T obj;

	public FLWritList(T obj) {
		this.obj = obj;
		objLengthBits = computeBits(obj.size());
		objLength = 1 << objLengthBits;
		zArr = new byte[objLength];
		myBytes = new byte[objLength];
		bais = new ByteArrayInputStream(myBytes);
		dis = new DataInputStream(bais);
	}

	private static int computeBits(int objLength) {
		int olBits = 0;
		objLength--;
		while (objLength > 0) {
			olBits++;
			objLength >>= 1;
		}
		return olBits;
	}

	@Override
	public void write(DataOutput out) throws IOException {
		if (adding) {
			out.writeInt(baos.size());
			baos.writeTo(out);
		} else {
			int byteLength = length << objLengthBits;
			out.writeInt(byteLength);
			out.write(myBytes, 0, byteLength);
		}
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		reset(false);
		int byteLength = in.readInt();
		length = byteLength >> objLengthBits;
		ensureByteSize(byteLength);
		in.readFully(myBytes, 0, byteLength);
	}

	private void ensureByteSize(int byteLength) {
		if (myBytes.length < byteLength) {
			myBytes = new byte[Math.max(byteLength, myBytes.length * 2)];
			bais = new ByteArrayInputStream(myBytes);
			dis = new DataInputStream(bais);
		}
	}

	@Override
	public T get(int i) {
		if (adding)
			throw new UnsupportedOperationException("Must not be adding");
		else if (i >= length)
			throw new ArrayIndexOutOfBoundsException("Length is only " + length);
		int place = i << objLengthBits;
		bais.reset();
		long skipped = bais.skip(place);
		assert skipped == place;
		try {
			obj.readFields(dis);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return obj;
	}

	@Override
	public int length() {
		return length;
	}

	public void add(T t) {
		if (!adding)
			throw new UnsupportedOperationException("Must be adding");
		try {
			int nextPlace = baos.size() + objLength;
			t.write(dos);
			int place = baos.size();
			if (place > nextPlace)
				throw new RuntimeException("Too many bytes written out: " + t
						+ " wrote " + (place - nextPlace + objLength)
						+ " bytes");
			baos.write(zArr, 0, nextPlace - place);
			length++;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void reset(boolean adding) {
		this.adding = adding;
		if (adding)
			baos.reset();
		else
			length = 0;
	}

	public void writeBack(int pos, T obj) {
		if (adding)
			throw new UnsupportedOperationException(
					"Cannot do this while adding");
		backWriter.reset();
		try {
			obj.write(backDos);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		backWriter.writeTo(myBytes, pos << objLengthBits);
	}

	public boolean isEmpty() {
		return length == 0;
	}
}
