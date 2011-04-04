package edu.berkeley.gamesman.loopyhadoop;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Mapper;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.database.FileDatabase;
import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.parallel.Range;
import edu.berkeley.gamesman.parallel.RangeFile;

/**
 * 
 * @author Unimportant Takes in a <range, file number>, outputs <0, file status>
 */
public class LoopyDatabaseCreationMapper extends
		Mapper<Range, IntWritable, IntWritable, RangeFile> {
	private FileSystem fs;
	private Configuration conf;
	private Game<?> game;
	private final Random random = new Random();
	private Path dbFolder;

	@Override
	public void setup(Context context) {
		try {
			org.apache.hadoop.conf.Configuration hadoopConf = context
					.getConfiguration();
			conf = Configuration.deserialize(hadoopConf
					.get("gamesman.configuration"));
			game = conf.getGame();
			dbFolder = new Path(conf.getProperty("gamesman.hadoop.dbfolder"));
			fs = FileSystem.get(hadoopConf);
		} catch (IOException e) {
			throw new Error(e);
		} catch (ClassNotFoundException e) {
			throw new Error(e);
		}
	}

	@Override
	public void map(Range rangeToMap, IntWritable ignore, Context context) {
		try {
			long rangeStart = rangeToMap.firstRecord;
			long numRecords = rangeToMap.numRecords;
			
			String outputName = "range" + rangeStart + "to"
					+ (rangeStart + numRecords - 1);
			
			FileDatabase db = new FileDatabase(outputName, conf, rangeStart,
					numRecords, false, true);
			// create local db to be filled in
			
			Record impossible = game.newRecord();
			impossible.value = Value.IMPOSSIBLE;
			DatabaseHandle dh = db.getHandle(false);
			db.fill(dh, game.recordToLong(null, impossible));
			db.close();
			// fill in all records to have value impossible(unreachable)
			
			Path localFile = new Path(outputName);
			
			Path tempHDFSName = new Path(dbFolder, outputName + "_"
					+ random.nextLong());
			fs.copyFromLocalFile(localFile, tempHDFSName);
			// create a temp file on hdfs with the records using a
			// random in the filename to avoid collision
			
			Path hdfsName = new Path(dbFolder, outputName);
			fs.rename(tempHDFSName, hdfsName);// rename hdfs file
			
			File dbFile = new File(outputName);
			dbFile.delete();// delete local shit
			
			context.write(new IntWritable(0),
					new RangeFile(rangeToMap, fs.getFileStatus(hdfsName)));// output
		} catch (IOException e) {
			throw new Error(e);
		} catch (InterruptedException ie) {
			throw new Error(ie);
		}
	}

	private String createDBFiles(Range rangeToMap, Context context)
			throws IOException, InterruptedException {
		long rangeStart = rangeToMap.firstRecord;
		long numRecords = rangeToMap.numRecords;
		
		String outputName = "range" + rangeStart + "to"
				+ (rangeStart + numRecords - 1);
		
		FileDatabase db = new FileDatabase(outputName, conf, rangeStart,
				numRecords, false, true);
		// create local db to be filled in

		Record impossible = game.newRecord();
		impossible.value = Value.IMPOSSIBLE;
		DatabaseHandle dh = db.getHandle(false);
		db.fill(dh, game.recordToLong(null, impossible));
		db.close();
		// fill in all records to have value impossible(unreachable)

		Path localFile = new Path(outputName);
		
		Path tempHDFSName = new Path(dbFolder, outputName + "_"
				+ random.nextLong());
		fs.copyFromLocalFile(localFile, tempHDFSName);
		// create a temp file on hdfs with the records using a
		// random in the filename to avoid collision

		Path hdfsName = new Path(dbFolder, outputName);
		fs.rename(tempHDFSName, hdfsName);// rename hdfs file

		File dbFile = new File(outputName);
		dbFile.delete();// delete local shit

		context.write(new IntWritable(0),
				new RangeFile(rangeToMap, fs.getFileStatus(hdfsName)));// output
		return outputName;
	}
}
