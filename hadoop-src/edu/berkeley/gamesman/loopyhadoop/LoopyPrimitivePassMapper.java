package edu.berkeley.gamesman.loopyhadoop;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.parallel.Range;
import edu.berkeley.gamesman.parallel.RangeFile;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by IntelliJ IDEA.
 * User: dxu
 * Date: 3/13/11
 * Time: 10:12 AM
 * To change this template use File | Settings | File Templates.
 */
public class LoopyPrimitivePassMapper<S extends State> extends Mapper<LongWritable, IntWritable, RangeFile, LongWritable> {
    private FileSystem fs;
    private Configuration conf;
    private Game<S> game;
    private final Random random = new Random();
    private Path dbFolder;
    private S[] childStates;
    private S position;
    private RangeFile[] rangeFiles;
    private LongWritable longWritable;

    @Override
    public void setup(Context context) {
        try {
            org.apache.hadoop.conf.Configuration hadoopConf = context
                    .getConfiguration();
            conf = Configuration.deserialize(hadoopConf.get("gamesman.configuration"));
            game = conf.getCheckedGame();
            childStates = game.newStateArray(game.maxChildren());
            position = game.newState();
            dbFolder = new Path(conf.getProperty("gamesman.hadoop.dbfolder"));
            fs = FileSystem.get(hadoopConf);
            longWritable = new LongWritable();

            SequenceFile.Reader reader = new SequenceFile.Reader(fs, new Path(hadoopConf.get("db.map.path")), hadoopConf);
            List<RangeFile> ranges = new ArrayList<RangeFile>();
            while(true) {
                Range r = new Range();
                FileStatus fileStatus = new FileStatus();
                if(!reader.next(r, fileStatus))
                    break;
                ranges.add(new RangeFile(r, fileStatus));
            }
            reader.close();
            rangeFiles = (RangeFile[])ranges.toArray();
        } catch (IOException e) {
            throw new Error(e);
        } catch (ClassNotFoundException e) {
            throw new Error(e);
        }
    }
    @Override
    public void map(LongWritable positionToMap, IntWritable ignore, Context context) {
        long pos = positionToMap.get();
        game.hashToState(pos, position);
        int numChildren = game.validMoves(position, childStates);
        long HashesPerFile = game.numHashes()/rangeFiles.length;
        for (int i = 0; i < numChildren; i++) {
            long childHash = game.stateToHash(childStates[i]);
            RangeFile childFile = rangeFiles[(int)(childHash/HashesPerFile)];
            longWritable.set(childHash);
            try {
                context.write(childFile, longWritable);
            } catch (IOException e) {
                throw new Error(e);
            } catch (InterruptedException e) {
                throw new Error(e);
            }
        }
    }



}
