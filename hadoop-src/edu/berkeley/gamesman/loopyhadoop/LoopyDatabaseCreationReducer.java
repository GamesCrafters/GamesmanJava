package edu.berkeley.gamesman.loopyhadoop;

import java.io.IOException;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import edu.berkeley.gamesman.parallel.Range;
import edu.berkeley.gamesman.parallel.RangeFile;

/**
 * @author Eric
 * The reducer for the database creation phase of the loopy hadoop solver
 */
public class LoopyDatabaseCreationReducer extends
		Reducer<IntWritable, RangeFile, Range, Text> {
	@Override
	public void reduce(IntWritable zero, Iterable<RangeFile> rangeFiles,
			Context context) {
		for (RangeFile rangeFile : rangeFiles) {
			try {
				context.write(rangeFile.myRange, rangeFile.myFile);
			} catch (IOException e) {
				new Error(e);
			} catch (InterruptedException e) {
				new Error(e);
			}
		}
	}
}
