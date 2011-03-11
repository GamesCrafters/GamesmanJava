package edu.berkeley.gamesman.loopyhadoop;

import java.io.IOException;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Reducer;

import edu.berkeley.gamesman.parallel.RangeFile;
import edu.berkeley.gamesman.parallel.tier.Range;

public class LoopyDatabaseCreationReducer extends
		Reducer<IntWritable, RangeFile, Range, FileStatus>
{
	@Override
	public void reduce(IntWritable zero, Iterable<RangeFile> rangeFiles, Context context)
	{
		for(RangeFile rangeFile : rangeFiles)
		{
			
			try
			{
				context.write(rangeFile.myRange, rangeFile.myFile);
			} catch (IOException e)
			{
				new Error(e);
			} catch (InterruptedException e)
			{
				new Error(e);
			}
		}
	}
}
