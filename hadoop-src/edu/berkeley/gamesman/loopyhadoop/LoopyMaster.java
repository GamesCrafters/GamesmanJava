package edu.berkeley.gamesman.loopyhadoop;

import java.io.IOException;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.GenericOptionsParser;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.parallel.Input;

public class LoopyMaster implements Runnable
{
	private final org.apache.hadoop.conf.Configuration hadoopConf;
	private final Configuration gamesmanConf;
	private final FileSystem fs;

	public static void main(String[] args) throws IOException
	{
		GenericOptionsParser gop = new GenericOptionsParser(args);
		LoopyMaster loopyMaster = new LoopyMaster(gop);
		new Thread(loopyMaster).start();
	}

	public LoopyMaster(GenericOptionsParser gop) throws IOException
	{
		String[] unparsedArgs = gop.getRemainingArgs();
		try
		{
			gamesmanConf = new Configuration(unparsedArgs[0]);
		} catch (ClassNotFoundException e)
		{
			throw new Error(e);
		}
		hadoopConf = gop.getConfiguration();
		hadoopConf.set("gamesman.conf", gamesmanConf.serialize());
		fs = FileSystem.get(hadoopConf);
	}

	@Override
	public void run()
	{
		try
		{
			createDatabase();
			FileStatus primitives = markLegalPositions();
			solve(primitives);
		} catch (IOException e)
		{
			throw new Error("Our program asploded :(.", e);
		}
	}

	private void createDatabase() throws IOException
	{
		// TODO Auto-generated method stub
		Job j = new Job(hadoopConf, "Initial database creation");
		j.setJarByClass(LoopyDatabaseCreationMapper.class);
		j.setMapperClass(LoopyDatabaseCreationMapper.class);
		j.setReducerClass(LoopyDatabaseCreationReducer.class);
		j.setInputFormatClass(Input.class);
	}

	private FileStatus markLegalPositions()
	{
		return null;
		// TODO Auto-generated method stub

	}

	private void solve(FileStatus primitives)
	{
		// TODO Auto-generated method stub

	}
}