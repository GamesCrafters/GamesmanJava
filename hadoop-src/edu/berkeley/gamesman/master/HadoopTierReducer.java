package edu.berkeley.gamesman.master;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.Iterator;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.GamesmanConf;
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
    private FSDataInputStream is;
    private FSDataOutputStream os;
    private SplitFileSystemDatabase writeDB;
    private FileInputStream fis;
    private FileOutputStream fos;


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
            is = new FSDataInputStream(fis);
            os = new FSDataOutputStream(fos, null);
            String dbUri = this.conf.getProperty("gamesman.hadoop.dbfolder");
            dbUri = dbUri + "_" + tier.get() + ".db";
            writeDB = new SplitFileSystemDatabase(new Path(dbUri), is, fs);

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
               RangeFile rf = it.next();
               rf.write(os);
        }
        os.writeChars("end");
        fos.close();
        os.close();
        fis.close();
        is.close();
    }
}
