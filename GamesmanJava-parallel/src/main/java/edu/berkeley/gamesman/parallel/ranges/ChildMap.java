package edu.berkeley.gamesman.parallel.ranges;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Writable;

import edu.berkeley.gamesman.parallel.writable.JumpList;

public class ChildMap implements Writable {
	private final JumpList parentList;
	private final JumpList childList;

	private int curParent;
	private int curChild;

	public ChildMap() {
		parentList = new JumpList();
		childList = new JumpList();
	}

	@Override
	public void write(DataOutput out) throws IOException {
		parentList.write(out);
		childList.write(out);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		parentList.readFields(in);
		childList.readFields(in);
		restart();
	}

	public void clear(boolean adding) {
		parentList.reset(adding);
		childList.reset(adding);
	}

	public void finish() {
		parentList.finish();
		childList.finish();
	}

	public void add(int parent, int child) {
		parentList.add(parent);
		childList.add(child);
	}

	public void restart() {
		parentList.restart();
		childList.restart();
		next();
	}

	public void next() {
		curParent = parentList.next();
		curChild = childList.next();
	}

	public int parent() {
		return curParent;
	}

	public int child() {
		return curChild;
	}
}
