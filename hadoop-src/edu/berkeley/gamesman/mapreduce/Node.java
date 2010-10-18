package edu.berkeley.gamesman.mapreduce;

import java.lang.Math;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Writable;

// TODO save memory!
public class Node implements Writable {
	public boolean expanded;
	public boolean solved;
	public long hash;
	public int rchildren;
	public int remoteness = Integer.MAX_VALUE;
	public int value;
	public long[] parents = new long[0];

	public static final int DRAW = 0;
	public static final int LOSE = 1;
	public static final int TIE = 2;
	public static final int WIN = 3;

	public void addParent(long parent_hash) {
		long[] newparents = new long[parents.length + 1];
		System.arraycopy(parents, 0, newparents, 0, parents.length);
		newparents[newparents.length-1] = parent_hash;
		parents = newparents;
	}

	// NOTE: not the same as value!
	// can use this to get final values to e.g. write into database
	public int getCurrentValue() {
		if (value == WIN)
			return WIN;
		if (rchildren > 0)
			return DRAW;
		return value;
	}

	public String stringify(int value) {
		switch (value) {
			case LOSE: return "LOSE";
			case WIN: return "WIN";
			case TIE: return "TIE";
			case DRAW: return "DRAW";
		}
		return "UNKNOWN";
	}

	public int valueInvert(int value) {
		switch (value) {
			case LOSE: return WIN;
			case WIN: return LOSE;
			case TIE: return TIE;
			case DRAW:
			default:
				throw new IllegalArgumentException("value not invertible");
		}
	}

	public void update(Message m) {
		if (m.type != Message.SOLVE)
			throw new IllegalArgumentException();
		if (solved || rchildren <= 0)
			throw new IllegalStateException();
		rchildren -= 1;
		int inverted = valueInvert(m.value);
		if (value < inverted) {
			value = inverted;
			remoteness = m.remoteness + 1;
		} else if (value == inverted) {
			if (value == LOSE)
				remoteness = Math.max(remoteness, m.remoteness + 1);
			else
				remoteness = Math.min(remoteness, m.remoteness + 1);
		}
		if (rchildren == 0 || value == WIN)
			solved = true;
	}

	public void write(DataOutput out) throws IOException {
		out.writeBoolean(expanded);
		out.writeBoolean(solved);
		out.writeLong(hash);
		out.writeInt(rchildren);
		out.writeInt(remoteness);
		out.writeInt(value);
		out.writeInt(parents.length);
		for (long i : parents)
			out.writeLong(i);
	}

	public void readFields(DataInput in) throws IOException {
		expanded = in.readBoolean();
		solved = in.readBoolean();
		hash = in.readLong();
		rchildren = in.readInt();
		remoteness = in.readInt();
		value = in.readInt();
		int nparents = in.readInt();
		parents = new long[nparents];
		for (int i=0; i < parents.length; i++)
			parents[i] = in.readLong();
	}

	public static Node read(DataInput in) throws IOException {
		Node n = new Node();
		n.readFields(in);
		return n;
	}

	// for FileInputFormat
	public static Node fromString(String str) throws IOException {
		Node n = new Node();
		if (str.contains("\t")) // XXX is output record format
			str = str.split("\t")[1];
		String[] x = str.split(" ");
		if (x.length == 1) {
			n.hash = Integer.parseInt(x[0]);
		} else {
			n.expanded = str.charAt(0) == '1';
			n.solved = str.charAt(1) == '1';
			n.hash = Long.parseLong(x[1]);
			n.rchildren = Integer.parseInt(x[2]);
			n.remoteness = Integer.parseInt(x[3]);
			n.value = Integer.parseInt(x[4]);
			n.parents = new long[x.length - 6];
			for (int i=0; i < n.parents.length; i++)
				n.parents[i] = Long.parseLong(x[5+i]);
		}
		return n;
	}

	// FileOutputFormat uses this
	public String toString() {
		String repr = "";
		repr += expanded ? 1 : 0;
		repr += solved ? 1 : 0;
		repr += " " + hash;
		repr += " " + rchildren;
		repr += " " + remoteness;
		repr += " " + value;
		for (long parent : parents)
			repr += " " + parent;
		repr += " " + stringify(getCurrentValue());
		if (!solved)
			repr += "?";
		return repr;
	}
}
