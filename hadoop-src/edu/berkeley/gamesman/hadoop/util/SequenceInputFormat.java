package edu.berkeley.gamesman.hadoop.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapred.InputFormat;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;

import edu.berkeley.gamesman.util.Util;

/**
 * A SequenceInputFormat is a Hadoop input format.
 * Normally an InputFormat reads data from a file, however here we just want to split over
 * a range of integers.  There's no reason to have that stored in the HDFS, so this input
 * format just throws numbers into the system
 * @author Steven Schlansker
 */
public class SequenceInputFormat implements InputFormat<LongWritable, LongWritable> {

	/**
	 * Empty constructor
	 */
	public SequenceInputFormat() {
	}
	
	public RecordReader<LongWritable, LongWritable> getRecordReader(
			InputSplit split, JobConf conf, Reporter rep) throws IOException {
		return new SequenceReader((SequenceSplit)split);
	}

	public InputSplit[] getSplits(JobConf conf, int numSplits) throws IOException {
		long cur = Long.parseLong(conf.get("first"));
		long end = Long.parseLong(conf.get("end"));
		long incr = Long.parseLong(conf.get("incr"));
		
		SequenceSplit[] splits = new SequenceSplit[numSplits];
		
		
		long step = (end-cur+numSplits-1)/numSplits;
		
		for(int i = 0; i < numSplits; i++){
			if(i == numSplits-1)
				step = end-cur;
			splits[i] = new SequenceSplit(cur,step,incr);
			cur += step;
		}
		
		System.out.println(Arrays.toString(splits));
		
		return splits;
	}

	public void validateInput(JobConf conf) throws IOException {
		// Nothing to do here...for now :-p
	}
	
}

class SequenceSplit implements InputSplit {

	long s,l, incr;
	
	public SequenceSplit() {
		s = 0;
		l = 0;
		incr = 1;
	}
	
	SequenceSplit(long start, long len, long increment){
		s = start;
		l = len;
		incr = increment;
	}
	
	public long getLength() throws IOException {
		return (l+incr-1)/incr;
	}

	public String[] getLocations() throws IOException {
		String[] hostnames = {"localhost"};
		return hostnames;
	}

	public void readFields(DataInput din) throws IOException {
		s = din.readLong();
		l = din.readLong();
		incr = din.readLong();
	}

	public void write(DataOutput dout) throws IOException {
		dout.writeLong(s);
		dout.writeLong(l);
		dout.writeLong(incr);
	}
	
	public String toString(){
		return "[@"+s+"+"+l+"/"+incr+"]";
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
		return (float)pos / (float)split.getLength();
	}

	public boolean next(LongWritable key, LongWritable value)
			throws IOException {
		if(pos <= split.getLength()){
			long realPos = pos*split.incr;
			key.set(split.s + realPos);
			pos++;
			realPos += split.incr;
			if (realPos >= split.l) {
				realPos = split.l-1;
			}
			value.set(split.s + realPos);
			return true;
		}
		return false;
	}
	
}
