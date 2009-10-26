package edu.berkeley.gamesman.hadoop.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Writable;

public class SplitDatabaseWritable implements Writable {
	String filename;
	long start;
	long end;
	int tier;

	public SplitDatabaseWritable(int tier) {
		this.tier = tier;
	}

	public void set(String filename, long startRecord, long stopRecord) {
		this.filename = filename;
		this.start = startRecord;
		this.end = stopRecord;
		if ((end-start) > 1000000000 || end <= start) {
			throw new RuntimeException("Error: file "+filename+" goes from "+start+" to "+end);
		}
	}
	public void readFields(DataInput in) throws IOException {
		filename = in.readUTF();
		start = in.readLong();
		end = start + in.readInt();
	}

        public void write(DataOutput out) throws IOException {
                out.writeUTF(getFilename());
                out.writeLong(getStart());
                out.writeInt(getLength());
	}

	public int getTier() {
		return tier;
	}

        public String getFilename(){
                return filename;
        }

	public long getStart() {
		return start;
	}

	public long getEnd() {
		return end;
	}

	public int getLength() {
       		if ((end-start) > 1000000000 || end <= start) {
			System.out.println("Error: file "+filename+" goes from "+start+" to "+end);
		}
		return (int)(end-start);
	}

        public String toString(){
                return filename+"["+start+":"+end+"]";
        }

}

