package edu.berkeley.gamesman.parallel.writable;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.hadoop.io.Writable;

import edu.berkeley.gamesman.propogater.writable.ByteArrayDOutputStream;

public class JumpList implements Writable {
	private byte[] myBytes;
	private int byteLength = 0;
	private final ByteArrayDOutputStream baos = new ByteArrayDOutputStream();
	private final DataOutputStream dos = new DataOutputStream(baos);
	private ByteArrayInputStream bais;
	private DataInputStream dis;
	private boolean adding = false;

	// Adding variables
	private int writtenKey = 0;
	private int startKey = -2;
	private int lastKey = -2;
	private int waitingSize = 0;
	private boolean finished = false;

	// Iterating variables
	int place = 0;
	int listRemaining = 0;

	public JumpList() {
		myBytes = new byte[1];
		bais = new ByteArrayInputStream(myBytes);
		dis = new DataInputStream(bais);
	}

	@Override
	public void write(DataOutput out) throws IOException {
		if (adding) {
			if (!finished)
				throw new RuntimeException("Must finish first");
			out.writeInt(baos.size());
			baos.writeTo(out);
		} else {
			out.writeInt(byteLength);
			out.write(myBytes, 0, byteLength);
		}
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		reset(false);
		byteLength = in.readInt();
		ensureByteSize(byteLength);
		in.readFully(myBytes, 0, byteLength);
		restart();
	}

	private void ensureByteSize(int byteLength) {
		if (myBytes.length < byteLength) {
			myBytes = new byte[Math.max(byteLength, myBytes.length * 2)];
			bais = new ByteArrayInputStream(myBytes);
			dis = new DataInputStream(bais);
		}
	}

	public void add(int t) {
		if (!adding)
			throw new UnsupportedOperationException("Must be adding");
		else if (finished)
			throw new RuntimeException("Already finished adding");
		if (t > lastKey + 1 || waitingSize >= Byte.MAX_VALUE) {
			try {
				writtenKey = writeOut(dos, writtenKey, startKey);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			waitingSize = 0;
			startKey = t;
		}
		waitingSize++;
		lastKey = t;
	}

	private int writeOut(DataOutput out, int writtenKey, int startKey)
			throws IOException {
		if (waitingSize > 1) {
			out.writeByte(-1);
			writeJump(writtenKey, startKey, out);
			writtenKey = startKey + waitingSize;
			assert waitingSize <= Byte.MAX_VALUE;
			out.writeByte(waitingSize);
		} else if (waitingSize == 1) {
			writeJump(writtenKey, startKey, out);
			writtenKey = startKey + 1;
		}
		return writtenKey;
	}

	private void writeJump(int writtenKey, int startKey, DataOutput out)
			throws IOException {
		int diff = startKey - writtenKey;
		assert diff >= 0;
		if (diff < Byte.MAX_VALUE)
			out.writeByte(diff);
		else if (diff < Short.MAX_VALUE) {
			out.writeByte(-2);
			out.writeShort(diff);
		} else {
			assert diff < Integer.MAX_VALUE;
			out.writeByte(-4);
			out.writeInt(diff);
		}
	}

	public void reset(boolean adding) {
		this.adding = adding;
		if (adding) {
			baos.reset();
			writtenKey = 0;
			startKey = -2;
			lastKey = -2;
			waitingSize = 0;
			finished = false;
		} else {
			byteLength = 0;
		}
	}

	public void finish() {
		try {
			writeOut(dos, writtenKey, startKey);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		waitingSize = 0;
		finished = true;
	}

	public boolean hasNext() {
		return bais.available() > myBytes.length - byteLength
				|| listRemaining > 0;
	}

	public int next() {
		try {
			if (listRemaining == 0) {
				if (bais.available() <= myBytes.length - byteLength)
					return -1;
				byte b = dis.readByte();
				if (b == -1) {
					b = dis.readByte();
					place += readJump(b, dis);
					listRemaining = dis.readByte();
				} else {
					place += readJump(b, dis);
					listRemaining = 1;
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		listRemaining--;
		return place++;
	}

	private int readJump(byte b, DataInput in) throws IOException {
		if (b >= 0)
			return b;
		else if (b == -2) {
			return in.readShort();
		} else if (b == -4) {
			return in.readInt();
		} else {
			throw new IOException("Don't know what to do with " + b);
		}
	}

	boolean noWaiting() {
		return waitingSize == 0;
	}

	public void restart() {
		if (adding)
			throw new UnsupportedOperationException(
					"Doesn't make sense when adding");
		bais.reset();
		place = 0;
		listRemaining = 0;
	}

	public boolean isEmpty() {
		if (adding)
			return baos.size() == 0;
		else
			return byteLength == 0;
	}

	public int getLast() {
		if (adding && !finished)
			return lastKey;
		else
			throw new UnsupportedOperationException();
	}
}
