package edu.berkeley.gamesman.hadoop;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Properties;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.RecordFields;
import edu.berkeley.gamesman.core.TieredGame;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.hadoop.util.BigIntegerWritable;
import edu.berkeley.gamesman.core.Hasher;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Util;

/**
 * 
 * @author Steven Schlansker
 */
public class TierMapReduce implements Mapper<BigIntegerWritable, NullWritable, BigIntegerWritable, BigIntegerWritable>,Reducer<BigIntegerWritable, BigIntegerWritable, BigIntegerWritable, TierMapReduce.RecordWritable>{

	protected TieredGame<Object> game;
	protected Hasher<?> hasher;
	protected Database db;
	//private BigIntegerWritable tempBI = new BigIntegerWritable();;
	//private RecordWritable tempDB = new RecordWritable();
	protected Configuration config;

	public void configure(JobConf conf) {
		Class<TieredGame<Object>> gc = null;
		Class<Database> gd = null;
		Class<Hasher<?>> gh = null;
		final String base = "edu.berkeley.gamesman.";
		OptionProcessor.initializeOptions(conf.getStrings("args"));
		Properties props = new Properties(System.getProperties());
		
		gc = Util.typedForName(base+"game."+conf.get("gameclass","NullGame"));
		gd = Util.typedForName(base+"database."+conf.get("databaseclass", "NullDatabase"));
		gh = Util.typedForName(base+"hasher."+conf.get("hasherclass","NullHasher"));
		
		try {
			game = gc.newInstance();
			hasher = gh.newInstance();
			db = gd.newInstance();
		} catch (InstantiationException e) {
			Util.fatalError("Could not create class "+e);
		} catch (IllegalAccessException e) {
			Util.fatalError("Could not access class "+e);
		}
		
		config = new Configuration(props,game,hasher,EnumSet.of(RecordFields.Value));
		
		game.initialize(config);
		db.initialize(conf.get("dburi"),config);
		
		Util.debug(DebugFacility.Hadoop,"Hadoop is ready to work!");
		
	}

	public void close() throws IOException {
		db.close();
	}
	
	final BigInteger minusone = BigInteger.ZERO.subtract(BigInteger.ONE);

	public void map(BigIntegerWritable position, NullWritable nullVal,
			OutputCollector<BigIntegerWritable, BigIntegerWritable> validMoves,
			Reporter reporter) throws IOException {
		boolean seenOne = false;
		
		Record record;

		ArrayList<Record> vals = new ArrayList<Record>();
		
		Object myHash = game.hashToState(position.get());
		
		for(Object move : game.validMoves(myHash)){
			seenOne = true;
			vals.add(db.getValue(game.stateToHash(move)));
		}
		if(seenOne){
			record = Record.combine(config,vals);
		}else{
			record = new Record(config,game.primitiveValue(myHash));
		}
		
		db.setValue(position.get(),record);
	}

	public void reduce(
			BigIntegerWritable position,
			Iterator<BigIntegerWritable> children,
			OutputCollector<BigIntegerWritable, RecordWritable> out,
			Reporter rep) throws IOException {
	}
	
	private class RecordWritable implements Writable {

		private Record value;
		
		public void readFields(DataInput in) throws IOException {
			value = Record.readStream(config,in);
		}

		public void write(DataOutput out) throws IOException {
			value.writeStream(out);
		}
		
		public Record get(){
			return value;
		}
		
		public void set(Record v){
			value = v;
		}
		
		public String toString(){
			return value.toString();
		}

	}
}
