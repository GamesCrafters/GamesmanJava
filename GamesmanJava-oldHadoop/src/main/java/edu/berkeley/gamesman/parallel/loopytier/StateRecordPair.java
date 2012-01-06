package edu.berkeley.gamesman.parallel.loopytier;

import org.apache.hadoop.io.WritableComparable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA. User: dxu Date: 4/12/11 Time: 5:23 PM To change
 * this template use File | Settings | File Templates.
 */
public class StateRecordPair implements WritableComparable<StateRecordPair> {

	/**
	 * A long corresponding to a game state
	 */
	public long state;
	/**
	 * A long corresponding to a record for the game state
	 */
	public long record;

	@Override
	public int compareTo(StateRecordPair o) {
		if (this.state > o.state) {
			return 1;
		} else if (this.state < o.state) {
			return -1;
		} else {
			return 0;
		}
	}

	/**
	 * An empty constructor
	 */
	public StateRecordPair() {
	}

	/**
	 * @param state the value to set state to
	 * @param record the value to set record to
	 */
	public StateRecordPair(long state, long record) {
		this.state = state;
		this.record = record;
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeLong(state);
		out.writeLong(record);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		state = in.readLong();
		record = in.readLong();
	}
}
