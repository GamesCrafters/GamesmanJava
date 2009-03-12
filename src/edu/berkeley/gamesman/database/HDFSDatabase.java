package edu.berkeley.gamesman.database;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobConf;

import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.TieredHasher;
import edu.berkeley.gamesman.master.HadoopMaster;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;
import edu.berkeley.gamesman.util.bytes.ByteProducer;
import edu.berkeley.gamesman.util.bytes.ByteStorage;
import edu.berkeley.gamesman.util.bytes.HDFSFileByteProducer;
import edu.berkeley.gamesman.util.bytes.HDFSFileByteStorage;

public class HDFSDatabase extends Database implements Runnable {
	
	TieredHasher<?> th;
	BigIntRange[] splits;
	
	BlockingQueue<Pair<BigInteger,Record>> bq = new ArrayBlockingQueue<Pair<BigInteger,Record>>(100);
	
	LRULinkedHashMap<Integer, Pair<ByteProducer,FSDataInputStream>> inFiles = new LRULinkedHashMap<Integer, Pair<ByteProducer,FSDataInputStream>>();

	private boolean open = true;
	

	private final JobConf hadoopconf;
	
	public HDFSDatabase(JobConf conf) {
		hadoopconf = conf;
	}

	@Override
	public void close() {
		open = false;
		try {
			bq.put(null);
		} catch (InterruptedException e) {
		}
	}

	@Override
	public void flush() {
		
	}

	@Override
	public Record getRecord(BigInteger loc) {
		int idx = findDB(loc);
		Pair<ByteProducer,FSDataInputStream> p;
		FSDataInputStream din = null;
		synchronized(inFiles){
			p = inFiles.get(idx);
			if(p == null){
				try {
					din = FileSystem.get(hadoopconf).open(pathForSlice(idx));
				} catch (IOException e) {
					Util.fatalError("Could not open output path!",e);
				}
				
				p = new Pair<ByteProducer, FSDataInputStream>(new HDFSFileByteProducer(din),din);

				inFiles.put(idx,p);
			}
		}
		
		return Record.read(conf, p.car, loc.subtract(splits[idx].first).longValue());
	}
	
	private Path pathForSlice(int idx){
		return new Path(FileOutputFormat.getWorkOutputPath(hadoopconf),"slice_"+idx);
	}

	@Override
	protected void initialize(String uri) {
		th = Util.checkedCast(conf.getHasher());
		splits = new BigIntRange[th.numberOfTiers()];
		for(int i = 0; i < th.numberOfTiers(); i++)
			splits[i] = new BigIntRange((i == 0 ? BigInteger.ZERO : splits[i-1].last.add(BigInteger.ONE)),th.lastHashValueForTier(i),i);
		Thread t = new Thread(this);
		t.setName("HDFS Databse runner: "+uri);
		t.start();
		assert Util.debug(DebugFacility.DATABASE, "Launched HDFS runner");
	}

	@Override
	public void putRecord(BigInteger loc, Record value) {
		try {
			bq.put(new Pair<BigInteger, Record>(loc,value));
		} catch (InterruptedException e) {
			Util.fatalError("Interrupted while enqueueing put request", e);
		}
	}
	
	private int findDB(BigInteger idx){
		return Arrays.binarySearch(splits, new BigIntRange(idx));
	}
	
	private static class BigIntRange implements Comparable<BigIntRange>{

		final BigInteger first,last;
		final int idx;
		
		public BigIntRange(BigInteger f, BigInteger l, int i) {
			first = f;
			last = l;
			idx = i;
		}
		
		public BigIntRange(BigInteger f){
			first = f;
			last = null;
			idx = -1;
		}

		public int compareTo(BigIntRange o) {
			if(idx != -1 && o.idx == -1 && first.compareTo(o.first) >= 0 && last.compareTo(o.first) <= 0){
				return 0;
			}
			if(idx == -1 && o.idx != -1 && first.compareTo(o.first) >= 0 && first.compareTo(o.last) <= 0){
				return 0;
			}
			return first.compareTo(o.first);
		}
		
	}

	public void run() {
		ByteStorage fb = null;
		FSDataOutputStream dout = null;
		FSDataInputStream din = null;
		int idx = -1;
		
		while(open){
			try{
				Pair<BigInteger, Record> p = bq.take();
				if(p == null) continue;
				int newidx = findDB(p.car);
				if(idx != newidx){
					if(dout != null) dout.close();
					dout = FileSystem.get(hadoopconf).create(pathForSlice(newidx), (short) 1);
					din = FileSystem.get(hadoopconf).open(pathForSlice(newidx));
					fb = new HDFSFileByteStorage(dout,din);
					idx = newidx;
				}
				p.cdr.write(fb, p.car.subtract(splits[idx].first).longValue());
			}catch (InterruptedException e) {
			}catch(IOException e){
				Util.fatalError("Exception while writing DB",e);
			}
		}
		assert Util.debug(DebugFacility.DATABASE, "Killed HDFS runner");
	}

}

class LRULinkedHashMap<K,V> extends LinkedHashMap<K, V> {
	protected boolean removeEldestEntry(Map.Entry eldest){
		return size() > 40;
	}
}
