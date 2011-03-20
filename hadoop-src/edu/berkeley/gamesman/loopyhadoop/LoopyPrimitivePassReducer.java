package edu.berkeley.gamesman.loopyhadoop;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.core.Value;
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
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by IntelliJ IDEA.
 * User: dxu
 * Date: 3/18/11
 * Time: 12:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class LoopyPrimitivePassReducer<S extends State> extends
        Reducer<RangeFile, LongWritable, LongWritable, IntWritable> {

    private FileSystem fs;
    private Configuration conf;
    private Game<S> game;
    private final Random random = new Random();
    private Path dbFolder;
    private LongWritable returnKey;
    private IntWritable returnVal;

    @Override
    public void setup(Context context) {
        try {
            org.apache.hadoop.conf.Configuration hadoopConf = context
                    .getConfiguration();
            conf = Configuration.deserialize(hadoopConf.get("gamesman.configuration"));
            game = conf.getCheckedGame();
            dbFolder = new Path(conf.getProperty("gamesman.hadoop.dbfolder"));
            fs = FileSystem.get(hadoopConf);
            returnKey = new LongWritable();
            returnVal = new IntWritable();

            //Donno if this is necessary
            returnVal.set(0);
        } catch (IOException e) {
            throw new Error(e);
        } catch (ClassNotFoundException e) {
            throw new Error(e);
        }
    }

    @Override
    public void reduce(RangeFile file, Iterable<LongWritable> hashes, Context context) {
        FileStatus myFileStatus = file.myFile;
        for (LongWritable lw : hashes) {
            Long hash = lw.get();
            Record hashVal = null;
            Value v;
            //TODO: Read HashVal from the database

            try {
                if (hashVal.value != Value.IMPOSSIBLE) {
                    //It was visited Already
                    continue;
                }

                //Check Primitive
                S state = game.hashToState(hash);
                v = game.primitiveValue(state);
                //TODO: Mark the Value in the DB


                //It is Primitive
                if (v != Value.UNDECIDED) {
                    //TODO: Write to the PrimitiveValues SeqFile
                //Not Primitive
                } else {

                    returnKey.set(hash);
                    context.write(returnKey, returnVal);
                }
            } catch (InterruptedException e) {
                throw new Error(e);
            } catch (IOException e) {
                throw new Error(e);
            }
        }
    }


    }
