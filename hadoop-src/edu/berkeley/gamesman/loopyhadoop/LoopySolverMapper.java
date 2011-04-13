package edu.berkeley.gamesman.loopyhadoop;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.game.Undoable;
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

/**
 * Created by IntelliJ IDEA.
 * User: dxu
 * Date: 4/12/11
 * Time: 5:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class LoopySolverMapper<S extends State> extends
        Mapper<LongWritable, LongWritable, RangeFile, StateRecordPair> {

    private FileSystem fs;
    private Configuration conf;
    private Game<S> game;
    private S[] parentStates;
    private S position;
    private RangeFile[] rangeFiles;
    private LongWritable longWritable;
    private long hashesPerFile;
    private Record rec;

    @Override
    public void setup(Context context) {
        try {
            org.apache.hadoop.conf.Configuration hadoopConf = context
                    .getConfiguration();
            conf = Configuration.deserialize(hadoopConf
                    .get("gamesman.configuration"));
            game = conf.getCheckedGame();
            parentStates = game.newStateArray(((Undoable)game).maxParents());
            position = game.newState();
            fs = FileSystem.get(hadoopConf);
            longWritable = new LongWritable();
            rec = new Record(conf);

            SequenceFile.Reader reader = new SequenceFile.Reader(fs, new Path(
                    hadoopConf.get("db.map.path")), hadoopConf);
            List<RangeFile> ranges = new ArrayList<RangeFile>();
            while (true) {
                Range r = new Range();
                FileStatus fileStatus = new FileStatus();
                if (!reader.next(r, fileStatus))
                    break;
                ranges.add(new RangeFile(r, fileStatus));
            }
            reader.close();

            rangeFiles = ranges.toArray(new RangeFile[ranges.size()]);

            hashesPerFile = game.numHashes() / ranges.size();
        } catch (IOException e) {
            throw new Error(e);
        } catch (ClassNotFoundException e) {
            throw new Error(e);
        } catch (ClassCastException e) {
            throw new Error("Game is not Undoable");
        }
    }

    @Override
	public void map(LongWritable positionToMap, LongWritable record,
			Context context) {
        State pos = game.hashToState(positionToMap.get());
        //game.longToRecord(pos, record.get(), rec);
        ((Undoable)game).possibleParents(pos, parentStates);


    }

}
