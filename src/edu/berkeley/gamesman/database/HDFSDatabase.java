package edu.berkeley.gamesman.database;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.hadoop.TierMapReduce;
import edu.berkeley.gamesman.hadoop.TieredHadoopTool;
import edu.berkeley.gamesman.util.Util;
import edu.berkeley.gamesman.util.bytes.HDFSFileByteProducer;

public final class HDFSDatabase extends Database {

	private Path dest;
	private ArrayList<BigIntRange> ranges;
	private BigInteger[] sliceEnds;
	private FileSystem fs;
	
	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	public void flush() {
		// TODO Auto-generated method stub

	}

	@Override
	public Record getRecord(BigInteger loc) {
		BigIntRange r = ranges.get(Util.binaryRangeSearch(loc, sliceEnds));
		if(r.path == null)
			Util.fatalError("Tried to read unsolved location "+loc);
		try {
			if(r.in == null){
				r.in = fs.open(r.path);
				r.bp = new HDFSFileByteProducer(r.in,r.path,fs.getFileStatus(r.path).getLen());
			}
			
			//Record.readStream(conf, r.in);
		} catch (IOException e) {
			Util.fatalError("Could not read record",e);
		}
		
		assert loc.compareTo(r.start) >= 0 && loc.compareTo(r.end) <= 0;
		Record rec = Record.read(conf, r.bp, loc.subtract(r.start).longValue());
		//System.out.println("read "+loc+" -> "+rec);
		return rec;
	}

	@Override
	protected void initialize(String uri) {
		dest = new Path(uri);
		try {
			fs = dest.getFileSystem(TierMapReduce.jobconf);
			Path searchPath = new Path(TieredHadoopTool.OUTPUT_PREFIX+"*");
			FileStatus[] candidates = fs.globStatus(searchPath);
			ranges = new ArrayList<BigIntRange>();
			BigInteger leastStart = null;
			for(FileStatus f : candidates){
				String bits[] = f.getPath().getName().split("_");
				assert bits.length == 3;
				BigInteger start = new BigInteger(bits[1]);
				BigInteger end = new BigInteger(bits[2]);
				if(leastStart == null || start.compareTo(leastStart) < 0)
					leastStart = start;
				ranges.add(new BigIntRange(start,end,new Path(f.getPath(),"slice")));
			}
			System.out.println("Found slice ranges "+ranges);
			if(leastStart != null){
				if(leastStart.compareTo(BigInteger.ZERO) > 0)
					ranges.add(new BigIntRange(BigInteger.ZERO,leastStart,null));
				Collections.sort(ranges);

				sliceEnds = new BigInteger[ranges.size()];
				int i = 0;

				for(BigIntRange r : ranges){
					sliceEnds[i] = r.end;
					i++;
				}
			}
		} catch (IOException e) {
			Util.fatalError("Hadoop filesystem gone?",e);
		}
	}

	@Override
	public void putRecord(BigInteger loc, Record value) {
		Util.fatalError("HDFSDatabase is read only!");
	}

}

class BigIntRange implements Comparable<BigIntRange> {
	final BigInteger start,end;
	final Path path;
	
	FSDataInputStream in;
	HDFSFileByteProducer bp;
	
	BigIntRange(BigInteger s, BigInteger e, Path p){
		start = s;
		end = e;
		path = p;
	}
	public int compareTo(BigIntRange o) {
		return start.compareTo(o.start);
	}
}