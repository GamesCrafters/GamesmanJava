package edu.berkeley.gamesman.core;

import java.io.OutputStream;
import java.util.Properties;
import java.util.Set;

public class GamesmanConf extends Configuration {
	org.apache.hadoop.conf.Configuration hadoopConf;

	public GamesmanConf(org.apache.hadoop.conf.Configuration hadoopConf)
			throws ClassNotFoundException {
		super(makeProperties(hadoopConf));
		this.hadoopConf = hadoopConf;
	}

	private static Properties makeProperties(
			org.apache.hadoop.conf.Configuration hadoopConf) {
		Properties props = new Properties();
		props.put("gamesman.game", hadoopConf.get("gamesman.game"));
		props.put("record.fields",
				hadoopConf.get("record.fields", "VALUE,REMOTENESS"));
		return props;
	}

	@Override
	public Set<Object> getKeys() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void deleteProperty(String key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public GamesmanConf cloneAll() {
		try {
			GamesmanConf gc = new GamesmanConf(hadoopConf);
			gc.db = db;
			return gc;
		} catch (ClassNotFoundException e) {
			throw new Error(e);
		}
	}

	@Override
	public String toString() {
		return "Config[" + hadoopConf + "," + getGame() + "]";
	}

	@Override
	public boolean __contains__(String key) {
		return hadoopConf.get(key) != null;
	}

	@Override
	protected String getPropertyOrNull(String key) {
		return hadoopConf.get(key);
	}

	@Override
	public Object setProperty(String key, String value) {
		String first = getPropertyOrNull(key);
		hadoopConf.set(key, value);
		return first;
	}
	
	@Override
	public void store(OutputStream os, String dbType, String uri){
		throw new UnsupportedOperationException();
	}
}
