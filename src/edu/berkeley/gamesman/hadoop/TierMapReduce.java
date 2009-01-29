package edu.berkeley.gamesman.hadoop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.TieredGame;
import edu.berkeley.gamesman.database.DBValue;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.hadoop.util.BigIntegerWritable;
import edu.berkeley.gamesman.hadoop.util.DBValueWritable;
import edu.berkeley.gamesman.core.Hasher;
import edu.berkeley.gamesman.util.IteratorWrapper;
import edu.berkeley.gamesman.util.Util;

public class TierMapReduce implements Mapper<BigIntegerWritable, NullWritable, BigIntegerWritable, BigIntegerWritable>,Reducer<BigIntegerWritable, BigIntegerWritable, BigIntegerWritable, DBValueWritable>{

	protected TieredGame<Object,DBValue> game;
	protected Hasher hasher;
	protected Database db;
	private BigIntegerWritable tempBI = new BigIntegerWritable();;
	private DBValueWritable tempDB = new DBValueWritable();

	public void configure(JobConf conf) {
		Class<TieredGame<Object,DBValue>> gc = null;
		Class<Database> gd = null;
		Class<Hasher> gh = null;
		try {
			gc = (Class<TieredGame<Object, DBValue>>) Class.forName(conf.get("gameclass","NullGame"));
			gd = (Class<Database>) Class.forName(conf.get("databaseclass", "NullDatabase"));
			gh = (Class<Hasher>) Class.forName(conf.get("hasherclass","NullHasher"));
		} catch (ClassNotFoundException e) {
			Util.fatalError("Could not find class :( "+e);
		}
		
		try {
			game = gc.newInstance();
			hasher = gh.newInstance();
			db = gd.newInstance();
		} catch (InstantiationException e) {
			Util.fatalError("Could not create class "+e);
		} catch (IllegalAccessException e) {
			Util.fatalError("Could not access class "+e);
		}
		
		game.setHasher(hasher);
		tempDB.set(game.getDBValueExample());
		db.initialize(conf.get("dburl"),new Configuration(game,hasher), tempDB.get());
		
		Util.debug("Hadoop is ready to work!");
		
	}

	public void close() throws IOException {}

	public void map(BigIntegerWritable position, NullWritable nullVal,
			OutputCollector<BigIntegerWritable, BigIntegerWritable> validMoves,
			Reporter reporter) throws IOException {
		for(Object move : game.validMoves(game.hashToState(position.get()))){
			tempBI.set(game.stateToHash(move));
			validMoves.collect(position, tempBI);
		}
	}
	
	public void reduce(BigIntegerWritable position,
			Iterator<BigIntegerWritable> children,
			OutputCollector<BigIntegerWritable, DBValueWritable> out,
			Reporter rep) throws IOException {
		DBValue v = game.getDBValueExample();
		ArrayList<DBValue> vals = new ArrayList<DBValue>();
		for(BigIntegerWritable child : new IteratorWrapper<BigIntegerWritable>(children)){
			vals.add(db.getValue(child.get()));
		}
		db.setValue(position.get(), v.fold(vals));
	}
}
