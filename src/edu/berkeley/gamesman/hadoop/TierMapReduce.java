package edu.berkeley.gamesman.hadoop;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Hasher;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.TieredGame;
import edu.berkeley.gamesman.database.HDFSDatabase;
import edu.berkeley.gamesman.hadoop.util.BigIntegerWritable;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * 
 * @author Steven Schlansker
 */
public class TierMapReduce<S> implements Mapper<BigIntegerWritable, NullWritable, BigIntegerWritable, BigIntegerWritable>,Reducer<BigIntegerWritable, BigIntegerWritable, BigIntegerWritable, RecordWritable>{

	protected TieredGame<S> game;
	protected Hasher<S> hasher;
	protected Database db;
	//private BigIntegerWritable tempBI = new BigIntegerWritable();;
	//private RecordWritable tempDB = new RecordWritable();
	protected Configuration config;

	public void configure(JobConf conf) {
		//Class<TieredGame<Object>> gc = null;
		//Class<Database> gd = null;
		//Class<Hasher<?>> gh = null;
		final String base = "edu.berkeley.gamesman.";
		//Properties props = new Properties(System.getProperties());

		config = Configuration.load(Util.decodeBase64(conf.get("configuration_data")));
		
		db = Util.typedInstantiate(base+"database."+config.getProperty("gamesman.database"));
		
		db.initialize(conf.get("dburi"),config);
		game = Util.checkedCast(config.getGame());
		hasher = Util.checkedCast(config.getHasher());
		
		assert Util.debugFormat(DebugFacility.HADOOP, "Hadoop is ready to work! (%s, %s)", game.describe(), hasher.toString());
	}

	public void close() throws IOException {
		//db.close();
	}
	
	final BigInteger minusone = BigInteger.ZERO.subtract(BigInteger.ONE);

	public void map(BigIntegerWritable position, NullWritable nullVal,
			OutputCollector<BigIntegerWritable, BigIntegerWritable> validMoves,
			Reporter reporter) throws IOException {
		boolean seenOne = false;
		
		Record record;

		ArrayList<Record> vals = new ArrayList<Record>();
		
		S myState = game.hashToState(position.get());
		
		for(Pair<String,S> movep : game.validMoves(myState)){
			seenOne = true;
			vals.add(db.getRecord(game.stateToHash(movep.cdr)));
		}
		if(seenOne){
			record = Record.combine(config,vals);
		}else{
			record = new Record(config,game.primitiveValue(myState));
		}
		
		db.putRecord(position.get(),record);
	}

	public void reduce(
			BigIntegerWritable position,
			Iterator<BigIntegerWritable> children,
			OutputCollector<BigIntegerWritable, RecordWritable> out,
			Reporter rep) throws IOException {
	}
}


class RecordWritable implements Writable {

	private Record value;
	private final Configuration config;
	
	RecordWritable(Configuration c){
		config = c;
	}
	
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
