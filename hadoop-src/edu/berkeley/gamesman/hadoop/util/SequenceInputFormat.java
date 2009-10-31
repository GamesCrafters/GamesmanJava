package edu.berkeley.gamesman.hadoop.util;

import edu.berkeley.gamesman.util.Util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapred.InputFormat;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;

/**
 * A SequenceInputFormat is a Hadoop input format.
 * Normally an InputFormat reads data from a file, however here we just want to split over
 * a range of integers.  There's no reason to have that stored in the HDFS, so this input
 * format just throws numbers into the system
 * @author Steven Schlansker
 */
@SuppressWarnings("deprecation")
public class SequenceInputFormat implements InputFormat<LongWritable, LongWritable> {
	public RecordReader<LongWritable, LongWritable> getRecordReader(
			InputSplit split, JobConf conf, Reporter rep) throws IOException {
		return new SequenceReader((SequenceSplit)split);
	}

	public InputSplit[] getSplits(JobConf conf, int numSplits) throws IOException {
		long cur = Long.parseLong(conf.get("first"));
		long end = Long.parseLong(conf.get("end"));
		int tasks = 1;//numSplits; //Integer.parseInt(conf.get("tasks"));
		int groupLength = Integer.parseInt(conf.get("recordsPerGroup"));

		long[] groups = Util.groupAlignedTasks(tasks, cur, end, groupLength);
		
		SequenceSplit[] splits = new SequenceSplit[numSplits];
		
		for(int i = 0; i < groups.length-1; i++){
			splits[i] = new SequenceSplit(groups[i], groups[i+1]);
		}
		
		System.out.println(Arrays.toString(splits));
		
		return splits;
	}
	
}

@SuppressWarnings("deprecation")
class SequenceSplit implements InputSplit {

	long s,l, incr;
	
	public SequenceSplit() {
		s = 0;
		l = 0;
	}
	
	SequenceSplit(long start, long len){
		s = start;
		l = len;
	}
	
	public long getLength() throws IOException {
		return l;
	}

	public String[] getLocations() throws IOException {
		String[] hostnames = {"localhost"};
		return hostnames;
	}

	public void readFields(DataInput din) throws IOException {
		s = din.readLong();
		l = din.readLong();
	}

	public void write(DataOutput dout) throws IOException {
		dout.writeLong(s);
		dout.writeLong(l);
	}
	
	public String toString(){
		return "[@"+s+"+"+l+"]";
	}
}

class SequenceReader implements RecordReader<LongWritable, LongWritable>{

	SequenceSplit split;
	
	long pos;
	
	SequenceReader(SequenceSplit split){
		this.split = split;
		pos = 0;
	}
	
	public void close() throws IOException {
		split = null;
	}

	public LongWritable createValue() {
		return new LongWritable();
	}

	public LongWritable createKey() {
		return new LongWritable();
	}

	public long getPos() throws IOException {
		return pos;
	}

	public float getProgress() throws IOException {
		if (pos == 0) {
			return 0;
		} else {
			return 1;
		}
	}

	public boolean next(LongWritable key, LongWritable value)
			throws IOException {
		if(pos == 0){
			key.set(split.s);
			value.set(split.s+split.l);
			return true;
		}
		return false;
	}
	
}
