package edu.berkeley.gamesman.core;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
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
	
	private Record(Configuration conf2){
		conf = conf2;
		setupBits();
	}

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
		Record r = new Record(conf);
		r.readStream(in);
		return r;
	}

	private void readStream(DataInput in) {
		try {
			for (int i = 0; i < bits.length; i++) {
				fields[i] = in.readInt();
				System.out.println("Read field "+i+" to "+fields[i]);
				if(fields[1] == 131072) Util.fatalError("WTF");
			}
		} catch (IOException e) {
			Util.fatalError("Error while writing record: " + e);
		}
	}

	public void write(ByteBuffer buf, long index) {

	}

	public static Record read(Configuration conf, ByteBuffer buf, long index) {
		Record r = new Record(conf);
		r.read(buf,index);
		return r;
	}
	
	private void read(ByteBuffer buf, long index){
		
	}

	public final int length() {
		return (maxbits + 7) / 8;
	}
	
	public static int length(final Configuration conf){
		return new Record(conf,PrimitiveValue.Win).length();
	}

	public final void set(final RecordFields field, final int value) {
		System.out.println("Set "+field+" to "+value);
		fields[sf.get(field).car] = value;
	}

	public final int get(final RecordFields field) {
		System.out.println("Get "+field+" is "+fields[sf.get(field).car]);
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

	public static Record combine(final Configuration conf, final List<Record> vals) {
		Record rec = new Record(conf);
		
		final EnumMap<RecordFields,ArrayList<Integer>> map = new EnumMap<RecordFields, ArrayList<Integer>>(RecordFields.class);
		for(RecordFields rf : conf.getStoredFields().keySet()){
			map.put(rf, new ArrayList<Integer>(vals.size()));
		}
		for(Record r : vals){
			for(RecordFields rf : conf.getStoredFields().keySet()){
				map.get(rf).add(r.get(rf));
			}
		}
		

		loop: for(RecordFields rf : conf.getStoredFields().keySet()){
			switch(rf){
			case Value:
				rec.set(rf,primitiveCombine(map.get(rf)).value);
				break;
			case Remoteness:
				rec.set(rf, Collections.min(map.get(rf))+1);
				break;
			default:
				Util.fatalError("Default case shouldn't have been reached");
			}
		}
		
		return rec;
	}
	
	private static PrimitiveValue primitiveCombine(List<Integer> vals){
		boolean seentie = false;
		for(Integer iv : vals){
			PrimitiveValue v = PrimitiveValue.values()[iv];
			switch(v){
			case Undecided:
				return PrimitiveValue.Undecided;
			case Lose:
				return PrimitiveValue.Win;
			case Tie:
				seentie = true;
				break;
			case Win:
				break;
			default:
				Util.fatalError("Illegal primitive value: "+iv);
			}
		}
		if(seentie) return PrimitiveValue.Tie;
		return PrimitiveValue.Lose;
	}

}
