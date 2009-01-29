package edu.berkeley.gamesman.core;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

public final class Record {

	private Configuration conf;

	private Map<RecordFields, Pair<Integer, Integer>> sf;

	private int[] bitOffset;
	private int[] bits;
	private int[] fields;

	private int maxbits;

	public Record(Configuration conf2, PrimitiveValue primitiveValue) {
		conf = conf2;
		setupBits();
		set(RecordFields.Value,primitiveValue.value);
	}
	
	private Record(){}

	public void writeStream(DataOutput out) {
		try {
			for (int i = 0; i < bits.length; i++) {
				out.writeInt(fields[i]);
			}
		} catch (IOException e) {
			Util.fatalError("Error while writing record: " + e);
		}
	}

	public static Record readStream(Configuration conf, DataInput in) {
		Record r = new Record();
		r.conf = conf;
		r.setupBits();
		r.readStream(in);
		return r;
	}

	private void readStream(DataInput in) {
		try {
			for (int i = 0; i < bits.length; i++) {
				fields[i] = in.readInt();
			}
		} catch (IOException e) {
			Util.fatalError("Error while writing record: " + e);
		}
	}

	public void write(ByteBuffer buf, long index) {

	}

	public static Record read(Configuration conf, ByteBuffer buf, long index) {
		Record r = new Record();
		r.conf = conf;
		r.setupBits();
		r.read(buf,index);
		return r;
	}
	
	private void read(ByteBuffer buf, long index){
		
	}

	public int length() {
		return (maxbits + 7) / 8;
	}
	
	public static int length(Configuration conf){
		return new Record(conf,PrimitiveValue.Win).length();
	}

	public final void set(RecordFields field, int value) {
		fields[sf.get(field).car] = value;
	}

	public final int get(RecordFields field) {
		return fields[sf.get(field).car];
	}

	private final void setupBits() {
		sf = conf.getStoredFields();
		bitOffset = new int[sf.size()];
		bits = new int[sf.size()];
		fields = new int[sf.size()];
		for (RecordFields key : sf.keySet()) {
			final Pair<Integer, Integer> info = sf.get(key);
			bits[info.car] = info.cdr;
			bitOffset[info.car] = (info.car > 0 ? bitOffset[info.car - 1] : 0)
					+ info.cdr;
		}
		maxbits = bitOffset[bitOffset.length - 1] + bits[bits.length - 1];
	}

	public static Record combine(Configuration conf, List<Record> vals) {
		Util.fatalError("TODO"); //TODO
		return null;
	}

}
