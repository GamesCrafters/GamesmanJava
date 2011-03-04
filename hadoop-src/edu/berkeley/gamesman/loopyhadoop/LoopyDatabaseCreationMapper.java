package edu.berkeley.gamesman.loopyhadoop;

import java.io.IOException;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Mapper;

import edu.berkeley.gamesman.parallel.tier.Range;

/**
 * 
 * @author Unimportant
 * Takes in a <range, file number>, outputs <0, file status>
 */
public class LoopyDatabaseCreationMapper extends
		Mapper<Range, IntWritable, IntWritable, FileStatus>
{
	@Override
	public void map(Range key, IntWritable value, Context context)
	{
		FileStatus file = new FileStatus();//TODO: name and path?
		
		long rangeStart = key.firstRecord;
		long rangeEnd = key.numRecords + rangeStart;
		for(long hash = rangeStart; hash < rangeEnd; hash++)
		{
			//TODO:  write this hash to the file in a reasonable format
		}
		
		try
		{
			context.write(new IntWritable(0), file);
		} catch (IOException e)
		{
			e.printStackTrace();
		} catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}
}
