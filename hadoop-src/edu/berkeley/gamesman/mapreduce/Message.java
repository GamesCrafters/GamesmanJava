package edu.berkeley.gamesman.mapreduce;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Writable;

// TODO save memory!
public class Message implements Writable, Cloneable {
	public int type;
	public Node node;
	public long hash;
	public int remoteness;
	public int value;

	public static final int IDENTITY = 0;
	public static final int EXPAND = 1;
	public static final int SOLVE = 2;

	public static Message clone(Message x) {
		Message m = new Message();
		m.type = x.type;
		m.node = x.node;
		m.hash = x.hash;
		m.remoteness = x.remoteness;
		m.value = x.value;
		return m;
	}

	public static Message Identity(Node n) {
		Message m = new Message();
		m.type = IDENTITY;
		m.node = n;
		return m;
	}

	public static Message Solve(long h, int v, int r) {
		Message m = new Message();
		m.type = SOLVE;
		m.hash = h;
		m.value = v;
		m.remoteness = r;
		return m;
	}

	public static Message Expand(long h) {
		Message m = new Message();
		m.type = EXPAND;
		m.hash = h;
		return m;
	}

	public void write(DataOutput out) throws IOException {
		out.writeInt(type);
		switch (type) {
			case IDENTITY: node.write(out); break;
			case EXPAND: out.writeLong(hash); break;
			case SOLVE:
				out.writeLong(hash);
				out.writeInt(remoteness);
				out.writeInt(value);
				break;
			default:
				throw new AssertionError();
		}
	}

	public void readFields(DataInput in) throws IOException {
		type = in.readInt();
		switch (type) {
			case IDENTITY: node = Node.read(in); break;
			case EXPAND: hash = in.readLong(); break;
			case SOLVE:
				hash = in.readLong();
				remoteness = in.readInt();
				value = in.readInt();
				break;
			default:
				throw new AssertionError();
		}
	}

	public static Message read(DataInput in) throws IOException {
		Message m = new Message();
		m.readFields(in);
		return m;
	}

	public String toString() {
		switch (type) {
			case IDENTITY: return "Identity node " + hash;
			case EXPAND: return "Expanded node with parent " + hash;
			case SOLVE: return "Solved node " + hash;
		}
		return "Message: ???";
	}
}
