package edu.berkeley.gamesman.hadoop.ranges;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import edu.berkeley.gamesman.propogater.writable.WritableSettableComparable;

public class Suffix implements WritableSettableComparable<Suffix> {
	private int[] suffix = new int[0];
	private int suffLen;

	@Override
	public void readFields(DataInput in) throws IOException {
		setLength(in.readInt());
		for (int i = 0; i < suffLen; i++)
			suffix[i] = in.readInt();
	}

	private void setLength(int length) {
		suffLen = length;
		if (suffix.length < suffLen)
			suffix = new int[suffLen];
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(suffLen);
		for (int i = 0; i < suffLen; i++)
			out.writeInt(suffix[i]);
	}

	@Override
	public void set(Suffix t) {
		setLength(t.suffLen);
		System.arraycopy(t.suffix, 0, suffix, 0, suffLen);
	}

	@Override
	public int compareTo(Suffix o) {
		if (suffLen != o.suffLen)
			return suffLen - o.suffLen;
		for (int i = suffLen - 1; i >= 0; i--) {
			if (suffix[i] != o.suffix[i])
				return suffix[i] - o.suffix[i];
		}
		return 0;
	}

	public void set(GenKey<?, ?> t, int suffLen) {
		setLength(suffLen);
		t.getSuffix(suffix, suffLen);
	}
}
