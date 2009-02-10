package edu.berkeley.gamesman.core;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * A Record stores a single game state's relevant information, possibly including
 * (but not limited to) primitive value, remoteness, etc.
 * 
 * Each Record has a list of fields specified in the Configuration object.
 * 
 * A Record is responsible for being able to write itself both to a Stream or a ByteBuffer.
 * These two different storage methods are not miscible.
 * @author Steven Schlansker
 */
public final class Record {

	private Configuration conf;

	private Map<RecordFields, Pair<Integer, Integer>> sf;

	private int[] bitOffset;
	private int[] bits;
	private int[] fields;

	/**
	 * Convenience constructor for a Record with only a PrimitiveValue specified.
	 * @param conf2 Configuration of the database
	 * @param primitiveValue The value of this record
	 */
	public Record(Configuration conf2, PrimitiveValue primitiveValue) {
		conf = conf2;
		setupBits();
		set(RecordFields.Value,primitiveValue.value);
	}
	
	private Record(Configuration conf2){
		conf = conf2;
		setupBits();
	}

	/**
	 * Write this record to a DataOutput.
	 * Using this stream-based method causes all output to be
	 * byte-aligned as it's not possible to easily seek/combine
	 * adjacent records.
	 * @param out The output to write to
	 * @see Record#readStream(Configuration,DataInput)
	 */
	public void writeStream(DataOutput out) {
		try {
			for (int i = 0; i < bits.length; i++) {
				out.writeByte(fields[i]);
			}
		} catch (IOException e) {
			Util.fatalError("Error while writing record: " + e);
		}
	}

	/**
	 * Read a record from the front of a given DataInput
	 * @param conf The configuration of the database
	 * @param in The DataInput to read from
	 * @return a new Record that was earlier stored with writeStream
	 * @see Record#writeStream(DataOutput)
	 */
	public static Record readStream(Configuration conf, DataInput in) {
		Record r = new Record(conf);
		r.readStream(in);
		return r;
	}

	private void readStream(DataInput in) {
		try {
			for (int i = 0; i < bits.length; i++) {
				fields[i] = in.readByte();
			}
		} catch (IOException e) {
			Util.fatalError("Error while reading record: " + e);
		}
	}

	/**
	 * Write this record to a specific location in a ByteBuffer.
	 * This method does not respect byte boundaries - if you have records
	 * of length 2 bits, the 3rd record should be halfway through the first byte.
	 * @param buf The buffer to write to
	 * @param index The record number location to write
	 * @see #read(Configuration, ByteBuffer, long)
	 */
	public void write(ByteBuffer buf, long index) { //TODO: writeme

	}

	/**
	 * Read a record from a specific index in a ByteBuffer.
	 * @see #write(ByteBuffer, long)
	 * @param conf The configuration of the database
	 * @param buf The buffer to read from
	 * @param index The location of the record
	 * @return a new Record with the loaded data
	 */
	public static Record read(Configuration conf, ByteBuffer buf, long index) {
		Record r = new Record(conf);
		r.read(buf,index);
		return r;
	}
	
	private void read(ByteBuffer buf, long index){ // TODO: writeme
		
	}

	/**
	 * @return the length of a single record in bits
	 */
	public final int length() {
		return bits.length;
		//return (maxbits + 7) / 8;
	}
	
	/**
	 * @param conf a Configuration
	 * @return the length of any record created with the specified Configuration
	 */
	public static int length(final Configuration conf){
		return new Record(conf).length();
	}

	/**
	 * Set a field in this Record
	 * @param field which field to set
	 * @param value the value to store
	 */
	public final void set(final RecordFields field, final int value) {
		fields[sf.get(field).car] = value;
	}

	/**
	 * Get a field from this Record
	 * @param field the field to get
	 * @return the value of that field
	 */
	public final int get(final RecordFields field) {
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
		//maxbits = bitOffset[bitOffset.length - 1] + bits[bits.length - 1]; TODO: delete?
	}

	/**
	 * Combine a list of records into one single record at a higher level.
	 * This is the basic operation for a game tree - the use case is that a single record
	 * for a state is created from the records of all its child game states
	 * @param conf the Configuration of the database
	 * @param vals the child records we'd like to combine
	 * @return a new Record that has the fields of the child Records
	 */
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
		

		for(RecordFields rf : conf.getStoredFields().keySet()){
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
	
	public String toString(){
		StringBuilder b = new StringBuilder();
		for(int fieldnum = 0; fieldnum < fields.length; fieldnum++){
			if(sf.get(RecordFields.Value).car == fieldnum)
				b.append(PrimitiveValue.values()[fields[fieldnum]]);
			else
				b.append(fields[fieldnum]);
			b.append(" ");
		}
		return b.toString();
	}

}
