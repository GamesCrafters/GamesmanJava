package edu.berkeley.gamesman.hadoop;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Hasher;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.TieredGame;
import edu.berkeley.gamesman.hadoop.util.BigIntegerWritable;
import edu.berkeley.gamesman.hadoop.util.RecordWritable;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * 
 * @author Steven Schlansker
 */
public class TierMapReduce<S> implements Mapper<BigIntegerWritable, NullWritable, BigIntegerWritable, RecordWritable>,Reducer<BigIntegerWritable, RecordWritable, BigIntegerWritable, RecordWritable>{

	protected TieredGame<S> game;
	protected Hasher<S> hasher;
	protected Database db;
	//private BigIntegerWritable tempBI = new BigIntegerWritable();;
	//private RecordWritable tempDB = new RecordWritable();
	public static Configuration config;
	public static JobConf jobconf;

	public void configure(JobConf conf) {
		//Class<TieredGame<Object>> gc = null;
		//Class<Database> gd = null;
		//Class<Hasher<?>> gh = null;
		final String base = "edu.berkeley.gamesman.";
		//Properties props = new Properties(System.getProperties());

		try {
			config = Configuration.load(Util.decodeBase64(conf.get("configuration_data")));
			jobconf = conf;
			db = Util.typedInstantiate(base+"database."+config.getProperty("gamesman.database"), Database.class);
		} catch (ClassNotFoundException e) {
			Util.fatalError("failed to load configuration class!", e);
			return;
		}
		
		//db.initialize(conf.get("dburi"),config);
		db.initialize(FileOutputFormat.getOutputPath(conf).toString(), config);
		game = Util.checkedCast(config.getGame());
		hasher = Util.checkedCast(config.getHasher());
		
		assert Util.debugFormat(DebugFacility.HADOOP, "Hadoop is ready to work! (%s, %s)", game.describe(), hasher.toString());
	}

	public void close() throws IOException {
		//db.close();
	}
	
	final private BigInteger minusone = BigInteger.ZERO.subtract(BigInteger.ONE);

	public void map(BigIntegerWritable position, NullWritable nullVal,
			OutputCollector<BigIntegerWritable, RecordWritable> outRec,
			Reporter reporter) throws IOException {
		boolean seenOne = false;
		
		Record record;

		ArrayList<Record> vals = new ArrayList<Record>();
		
		S myState = game.hashToState(position.get());
		
		PrimitiveValue pv = game.primitiveValue(myState);
		if(pv.equals(PrimitiveValue.UNDECIDED)){
			for(Pair<String,S> movep : game.validMoves(myState)){
				seenOne = true;
				vals.add(db.getRecord(game.stateToHash(movep.cdr)));
			}
		}
		if(seenOne){
			record = Record.combine(config,vals);
		}else{
			record = new Record(config,pv);
		}
		
		//db.putRecord(position.get(),record);
		
		RecordWritable rw = new RecordWritable(config);
		rw.set(record);
		
		outRec.collect(position,rw);
	}

	public void reduce(
			BigIntegerWritable position,
			Iterator<RecordWritable> record,
			OutputCollector<BigIntegerWritable, RecordWritable> out,
			Reporter rep) throws IOException {
		RecordWritable r = record.next();
		//System.out.println("write "+position+": "+r.get());
		out.collect(position,r);
	}
}