package edu.berkeley.gamesman.loopyhadoop;

import edu.berkeley.gamesman.parallel.Range;
import edu.berkeley.gamesman.parallel.RangeFile;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Reducer;

/**
 * Created by IntelliJ IDEA.
 * User: dxu
 * Date: 3/18/11
 * Time: 12:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class LoopyPrimitivePassReducer extends
        Reducer<RangeFile, LongWritable, LongWritable, IntWritable> {
}
