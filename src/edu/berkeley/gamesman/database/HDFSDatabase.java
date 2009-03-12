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
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;
import edu.berkeley.gamesman.util.bytes.ByteProducer;
import edu.berkeley.gamesman.util.bytes.ByteStorage;
import edu.berkeley.gamesman.util.bytes.HDFSFileByteProducer;
import edu.berkeley.gamesman.util.bytes.HDFSFileByteStorage;

public class HDFSDatabase extends Database implements Runnable {
	
	TieredHasher<?> th;
	BigInteger[] ends;
	
	BlockingQueue<Pair<BigInteger,Record>> bq = new ArrayBlockingQueue<Pair<BigInteger,Record>>(100);
	
	LRULinkedHashMap<Pair<Integer,Integer>, Pair<ByteProducer,FSDataInputStream>> inFiles = new LRULinkedHashMap<Pair<Integer,Integer>, Pair<ByteProducer,FSDataInputStream>>();

	private boolean open = true;
	private final BigInteger SPLIT_SIZE = BigInteger.valueOf(8388608L);
	
	private static final Pair<BigInteger,Record> FLUSH_MARKER = new Pair<BigInteger,Record>(BigInteger.ZERO,null);
	

	private final JobConf hadoopconf;
	
	public HDFSDatabase(JobConf conf) {
		hadoopconf = conf;
	}

	@Override
	public void close() {
		if(!open) return;
		open = false;
		Util.warn("Closed HDFSDatabase...");
		flush();
	}

	@Override
	public void flush() {
		try {
			bq.put(FLUSH_MARKER);
		} catch (InterruptedException e) {
			Util.warn("Interrupted while trying to flush (?)",e);
		}
	}

	@Override
	public Record getRecord(BigInteger loc) {
		Pair<Integer,Integer> l = findDB(loc);
		int idx = l.car;
		int slice = l.cdr;
		Pair<ByteProducer,FSDataInputStream> p;
		FSDataInputStream din = null;

		if(currentWriteBuffer != null){
			synchronized(currentWriteBuffer){
				while(idx == currentWriteTier && slice == currentWriteSlice){
					try {
						flush();
						currentWriteBuffer.wait();
					} catch (InterruptedException e) {}
				}
			}
		}
		
		synchronized(inFiles){
			p = inFiles.get(l);
			if(p == null){
				try {
					din = FileSystem.get(hadoopconf).open(pathForSlice(idx,slice));
				} catch (IOException e) {
					Util.fatalError("Could not open output path!",e);
				}
				
				p = new Pair<ByteProducer, FSDataInputStream>(new HDFSFileByteProducer(din),din);

				inFiles.put(l,p);
				Util.debug(DebugFacility.DATABASE, "Opening "+idx+"_"+slice+" for read");
			}
		}
		
		return Record.read(conf, p.car, loc.subtract(sval(idx)).subtract(BigInteger.valueOf(l.cdr).multiply(SPLIT_SIZE)).longValue());
	}
	
	private Path pathForSlice(int idx, int slice){
		return new Path(FileOutputFormat.getWorkOutputPath(hadoopconf),"tier_"+idx+"_slice_"+slice);
	}

	@Override
	protected void initialize(String uri) {
		th = Util.checkedCast(conf.getHasher());
		ends = new BigInteger[th.numberOfTiers()];
		for(int i = 0; i < th.numberOfTiers(); i++)
			ends[i] = th.lastHashValueForTier(i);
		Thread t = new Thread(this);
		t.setName("HDFS Database runner: "+uri);
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
	
	private Pair<Integer,Integer> findDB(BigInteger idx){
		int l = 0, r = ends.length;
		int p;
		//System.out.println("search "+idx);
		while(true){
			p = (r-l)/2;

			if(p == 0 && ends[p].compareTo(idx) < 0)
				Util.fatalError("Index "+idx+" not in binary search "+Arrays.toString(ends));
			p += l;
			//System.out.print(p);
			if(ends[p].compareTo(idx) >= 0){
				if(p == 0 || ends[p-1].compareTo(idx) < 0){
					//System.out.println(idx+" E "+idx+" ("+sval(p)+"-"+ends[p]+")");
					break;
				}

				//System.out.println(" r");
				r = p;
			}else{
				//System.out.println(" l");
				l = p;
			}
		}
		assert p>=0 && p < ends.length;
		BigInteger off = idx.subtract(sval(p));
		return new Pair<Integer,Integer>((Integer)p,off.divide(SPLIT_SIZE).intValue());
	}
	
	private final BigInteger sval(int tier){
		if(tier == 0) return BigInteger.ZERO;
		return ends[tier-1].add(BigInteger.ONE);
	}
	

	private ByteStorage currentWriteBuffer = null;
	private int currentWriteTier = -1;
	private int currentWriteSlice = -1;
	
	public void run() {
		try{
			FSDataOutputStream dout = null;

			while(open){
				try{
					Pair<BigInteger, Record> p = bq.take();
					if(p == FLUSH_MARKER) {
						synchronized(currentWriteBuffer){
							currentWriteTier = -1;
							currentWriteSlice = -1;
							currentWriteBuffer.close();
							dout.close();
							if(currentWriteBuffer != null)
								currentWriteBuffer.notifyAll();
							currentWriteBuffer = null;
							dout = null;
							assert Util.debug(DebugFacility.DATABASE, "Closing "+currentWriteTier+"_"+currentWriteSlice);
						}
					}
					Pair<Integer,Integer> loc = findDB(p.car);
					int newidx = loc.car;
					int newslice = loc.cdr;
					if(currentWriteTier != newidx || newslice != currentWriteSlice){
						synchronized((currentWriteBuffer != null ? currentWriteBuffer : this)){
							if(dout != null) dout.close();
							if(currentWriteBuffer != null) currentWriteBuffer.close();
							dout = FileSystem.get(hadoopconf).create(pathForSlice(newidx,newslice), (short) 1);
							currentWriteTier = newidx;
							currentWriteSlice = newslice;
							currentWriteBuffer = new HDFSFileByteStorage(dout,SPLIT_SIZE.intValue());
							synchronized(currentWriteBuffer){
								currentWriteBuffer.notifyAll();
							}
							assert Util.debug(DebugFacility.DATABASE, "Opening "+currentWriteTier+"_"+currentWriteSlice+" for write");
						}
					}
					long dbloc = p.car.subtract(sval(currentWriteTier)).subtract(SPLIT_SIZE.multiply(BigInteger.valueOf(currentWriteSlice))).longValue();
					if(dbloc < 0)
						System.out.println(p.car+" => "+currentWriteTier+" ("+sval(currentWriteTier)+") "+"_"+currentWriteSlice+" => "+dbloc);
					p.cdr.write(currentWriteBuffer, dbloc);
					//System.out.println("!");
				}catch (InterruptedException e) {
				}catch(IOException e){
					Util.fatalError("Exception while writing DB",e);
				}
			}
			assert Util.debug(DebugFacility.DATABASE, "Killed HDFS runner");
		}catch(Exception e){
			Util.fatalError("HDFSDatabase runner died unexpectedly!",e);
		}
	}

}

class LRULinkedHashMap<K,V> extends LinkedHashMap<K, V> {
	protected boolean removeEldestEntry(Map.Entry eldest){
		return size() > 40;
	}
}
