package edu.berkeley.gamesman.parallel.hadoop;

import java.io.IOException;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapred.InputFormat;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;

public class SolveInput implements InputFormat<LongWritable, LongWritable> {
	public class Reader implements RecordReader<LongWritable, LongWritable> {
		private SolveSplit mySplit;
		private boolean read = false;

		public Reader(SolveSplit s) {
			mySplit = s;
		}

		public void close() throws IOException {
		}

		public LongWritable createKey() {
			return new LongWritable();
		}

		public LongWritable createValue() {
			return new LongWritable();
		}

		public long getPos() throws IOException {
			if (read)
				return mySplit.getLength();
			else
				return 0L;
		}

		public float getProgress() throws IOException {
			if (read)
				return 1F;
			else
				return 0F;
		}

		public boolean next(LongWritable first, LongWritable num)
				throws IOException {
			if (read)
				return false;
			first.set(mySplit.first);
			num.set(mySplit.num);
			read = true;
			return true;
		}
	}

	public RecordReader<LongWritable, LongWritable> getRecordReader(
			InputSplit is, JobConf conf, Reporter reporter) throws IOException {
		Reader rr = new Reader((SolveSplit) is);
		return rr;
	}

	public InputSplit[] getSplits(JobConf conf, int splits) throws IOException {
		long num = conf.getLong("numRecords", -1L);
		if (num < 0)
			throw new Error("numRecords not set");
		InputSplit[] is = new InputSplit[splits];
		long first = conf.getLong("firstRecord", -1L);
		if (first < 0)
			throw new Error("firstRecord not set");
		long last = first;
		for (int i = 0; i < splits; i++) {
			long next = first + num * (i + 1) / splits;
			is[i] = new SolveSplit(last, (int) (next - last));
			last = next;
		}
		return is;
	}

	public void validateInput(JobConf conf) throws IOException {

	}

}