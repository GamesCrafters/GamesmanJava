//package edu.berkeley.gamesman.hadoop.util;
//
//import java.io.DataInput;
//import java.io.DataOutput;
//import java.io.IOException;
//
//import org.apache.hadoop.io.Writable;
//
//import edu.berkeley.gamesman.core.Configuration;
//import edu.berkeley.gamesman.core.Record;
//import edu.berkeley.gamesman.hadoop.TierMapReduce;
//
//public class RecordWritable implements Writable {
//
//	private Record value;
//	private final Configuration config;
//	
//	public RecordWritable(Configuration c){
//		config = c;
//	}
//	
//	RecordWritable(){
//		config = TierMapReduce.config;
//	}
//	
//	public void readFields(DataInput in) throws IOException {
//		value = Record.readStream(config,in);
//	}
//
//	public void write(DataOutput out) throws IOException {
//		value.writeStream(out);
//	}
//	
//	public Record get(){
//		return value;
//	}
//	
//	public void set(Record v){
//		value = v;
//	}
//	
//	public String toString(){
//		return value.toString();
//	}
//
//}
