package edu.berkeley.gamesman.hadoop.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapred.InputFormat;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;

import edu.berkeley.gamesman.util.Util;

/**
 * A SequenceInputFormat is a Hadoop input format
 * Normally an InputFormat reads data from a file, however here we just want to split over
 * a range of integers.  There's no reason to have that stored in the HDFS, so this input
 * format just throws numbers into the system
 * @author Steven Schlansker
 */
public class SequenceInputFormat implements InputFormat<BigIntegerWritable, NullWritable> {

	/**
	 * Empty constructor
	 */
	public SequenceInputFormat() {
	}
	
	public RecordReader<BigIntegerWritable, NullWritable> getRecordReader(
			InputSplit split, JobConf conf, Reporter rep) throws IOException {
		return new SequenceReader((SequenceSplit)split);
	}

	public InputSplit[] getSplits(JobConf conf, int numSplits) throws IOException {
		BigInteger lastHash = new BigInteger(conf.get("lasthash"));
		
		SequenceSplit[] splits = new SequenceSplit[numSplits];
		
		BigInteger cur = new BigInteger(conf.get("firsthash"));
		
		BigInteger step = lastHash.subtract(cur).divide(BigInteger.valueOf(numSplits));
		
		for(int i = 0; i < numSplits; i++){
			if(i == numSplits-1)
				step = lastHash.subtract(cur);
			splits[i] = new SequenceSplit(cur,step);
			cur = cur.add(step);
		}
		
		System.out.println(Arrays.toString(splits));
		
		return splits;
	}

	public void validateInput(JobConf conf) throws IOException {
		// Nothing to do here...for now :-p
	}
	
}

class SequenceSplit implements InputSplit {

	BigInteger s,l;
	
	public SequenceSplit() {
		s = BigInteger.ZERO;
		l = BigInteger.ZERO;
	}
	
	SequenceSplit(BigInteger start, BigInteger len){
		s = start;
		l = len;
	}
	
	public long getLength() throws IOException {
		return l.longValue();
	}

	public String[] getLocations() throws IOException {
		String[] hostnames = {"localhost"};
		return hostnames;
	}

	public void readFields(DataInput din) throws IOException {
		byte[] b;
		b = new byte[din.readInt()];
		din.readFully(b);
		s = new BigInteger(b);
		b = new byte[din.readInt()];
		din.readFully(b);
		l = new BigInteger(b);
	}

	public void write(DataOutput dout) throws IOException {
		byte[] b = s.toByteArray();
		dout.writeInt(b.length);
		dout.write(b);
		b = l.toByteArray();
		dout.writeInt(b.length);
		dout.write(b);
	}
	
	public String toString(){
		return "[@"+s+"+"+l+"]";
	}
	
}

class SequenceReader implements RecordReader<BigIntegerWritable, NullWritable>{

	SequenceSplit split;
	
	long pos;
	
	SequenceReader(SequenceSplit split){
		this.split = split;
		pos = 0;
	}
	
	public void close() throws IOException {
		split = null;
	}

	public NullWritable createValue() {
		return NullWritable.get();
	}

	public BigIntegerWritable createKey() {
		return new BigIntegerWritable();
	}

	public long getPos() throws IOException {
		return pos;
	}

	public float getProgress() throws IOException {
		return (float)pos / (float)split.getLength();
	}

	public boolean next(BigIntegerWritable key, NullWritable value)
			throws IOException {
		if(pos <= split.getLength()){
			key.set(split.s.add(BigInteger.valueOf(pos)));
			//Util.debug("split: "+key.get()+" ("+split.s+")");
			pos++;
			return true;
		}
		return false;
	}
	
}