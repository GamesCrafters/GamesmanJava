package edu.berkeley.gamesman.parallel.writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Writable;

import edu.berkeley.gamesman.util.qll.Pool;
import edu.berkeley.gamesman.util.qll.QLLFactory;

public class JumpList implements Writable {

	private final WritableQLL<IntWritable> objs;

	public JumpList(QLLFactory<IntWritable> fact, Pool<IntWritable> pool) {
		objs = new WritableQLL<IntWritable>(fact, pool);
	}

	int waitingSize;

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(size());
		objs.restart();
		assert waitingSize == 0;
		int writtenKey = 0;
		int startKey = -2;
		int lastKey = -2;
		for (int i = 0; i < objs.size(); i++) {
			IntWritable obj = objs.next();
			int nextKey = obj.get();
			if (nextKey > lastKey + 1 || waitingSize >= Byte.MAX_VALUE) {
				writtenKey = writeOut(out, writtenKey, startKey);
				waitingSize = 0;
				startKey = nextKey;
			}
			waitingSize++;
			lastKey = nextKey;
		}
		writeOut(out, writtenKey, startKey);
		waitingSize = 0;
	}

	boolean noWaiting() {
		return waitingSize == 0;
	}

	public int size() {
		return objs.size();
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

	@Override
	public void readFields(DataInput in) throws IOException {
		objs.clear();
		int size = in.readInt();
		int place = 0;
		int listRemaining = 0;
		for (int i = 0; i < size; i++) {
			IntWritable obj = objs.add();
			if (listRemaining == 0) {
				byte b = in.readByte();
				if (b == -1) {
					b = in.readByte();
					place += readJump(b, in);
					listRemaining = in.readByte();
				} else {
					place += readJump(b, in);
					listRemaining = 1;
				}
			}
			obj.set(place++);
			listRemaining--;
		}
		restart();
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

	public void restart() {
		objs.restart();
	}

	public int next() {
		IntWritable next = objs.next();
		return next == null ? -1 : next.get();
	}

	public void clear() {
		objs.clear();
	}

	public boolean isEmpty() {
		return objs.isEmpty();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof JumpList && equals((JumpList) other);
	}

	public boolean equals(JumpList other) {
		return objs.equals(other.objs);
	}

	@Override
	public int hashCode() {
		return objs.hashCode();
	}

	@Override
	public String toString() {
		return objs.toString();
	}

	public void add(int i) {
		if (!objs.isEmpty() && i <= objs.getLast().get())
			throw new RuntimeException("Cannot add " + i
					+ ", must be greater than " + objs.getLast().get());
		objs.add().set(i);
	}

	public int getLast() {
		return objs.getLast().get();
	}
}
