package edu.berkeley.gamesman.hadoop;

import java.io.IOException;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;

public class TierMap extends MapReduceBase implements Mapper<BigIntegerWritable, NullWritable, BigIntegerWritable, BigIntegerWritable>{

	public TierMap(){
		
	}
	
	public void map(BigIntegerWritable position, NullWritable nullVal,
			OutputCollector<BigIntegerWritable, BigIntegerWritable> validMoves,
			Reporter reporter) throws IOException {
		
	}

}
