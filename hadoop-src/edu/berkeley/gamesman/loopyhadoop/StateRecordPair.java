package edu.berkeley.gamesman.loopyhadoop;

import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: dxu
 * Date: 4/12/11
 * Time: 5:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class StateRecordPair implements WritableComparable<StateRecordPair>{

    public long state;
    public long recordValue;

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

    public StateRecordPair(long state, long record) {
        this.state = state;
        this.recordValue = record;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeLong(state);
        out.writeLong(recordValue);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        state = in.readLong();
        recordValue = in.readLong();
    }
}
