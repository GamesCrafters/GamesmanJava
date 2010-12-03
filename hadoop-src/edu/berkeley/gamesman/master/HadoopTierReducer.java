package edu.berkeley.gamesman.master;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.Iterator;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.GamesmanConf;
import edu.berkeley.gamesman.database.GZippedFileSystemDatabase;
import edu.berkeley.gamesman.database.SplitDatabase;
import edu.berkeley.gamesman.database.SplitFileSystemDatabase;
import edu.berkeley.gamesman.game.TierGame;
import edu.berkeley.gamesman.solver.TierSolver;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Reducer;

public class HadoopTierReducer extends
        Reducer<IntWritable, RangeFile, IntWritable, FileStatus> {
    Configuration conf;
    private TierGame game;
    FileSystem fs;
    private final IntWritable tier = new IntWritable();
    private TierSolver solver;
    private SplitDatabase writeDb;
    private FileInputStream fis;
    private FileOutputStream fos;
    private String dbUri;


    @Override
    public void setup(Context context) {
        try {
            org.apache.hadoop.conf.Configuration conf = context
                    .getConfiguration();
            this.conf = new GamesmanConf(conf);
            game = (TierGame) this.conf.getGame();

            fs = FileSystem.get(conf);
            tier.set(conf.getInt("tier", -1));
            solver = new TierSolver(this.conf);
            fis =  new FileInputStream("temp");
            fos = new FileOutputStream("temp");
            dbUri = this.conf.getProperty("gamesman.hadoop.tierDb");
            dbUri = dbUri + "_" + tier.get() + ".db";
            //writeDB = new SplitFileSystemDatabase(new Path(dbUri), is, fs);
            writeDb = SplitDatabase.openSplitDatabase(dbUri, this.conf, true, true);

        } catch (ClassNotFoundException e) {
            throw new Error(e);
        } catch (IOException e) {
            throw new Error(e);
        }
    }


    public void reduce(IntWritable key, Iterable<RangeFile> values,
                       Context context) throws IOException {
        Iterator<RangeFile> it = values.iterator();
        while (it.hasNext()) {
                RangeFile temp = it.next();
                long firstRecord = temp.myRange.firstRecord;
                long numRecords = temp.myRange.numRecords;
                String uri = temp.myFile.getPath().toString();
               writeDb.addDb(GZippedFileSystemDatabase.class.getName(), uri, firstRecord, numRecords);
        }
        writeDb.close();
        fs.copyFromLocalFile(new Path(dbUri), new Path(dbUri));
    }
}
