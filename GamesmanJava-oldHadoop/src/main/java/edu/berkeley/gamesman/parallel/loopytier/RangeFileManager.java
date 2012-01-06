package edu.berkeley.gamesman.parallel.loopytier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;

import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.parallel.Range;
import edu.berkeley.gamesman.parallel.RangeFile;

/**
 * @author Eric A class that keeps an array of range files that are used for
 *         database look up in the primitive pass and solver mappers
 * @param <S>
 *            The GameState class for the game being solved
 */
public class RangeFileManager<S extends State<S>> {
	private RangeFile[] rangeFiles;
	private long hashesPerFile;

	/**
	 * @param fs
	 *            the hadoop file system used in the mapper
	 * @param hadoopConf
	 *            the hadoop configuration used in the mapper
	 * @param game
	 *            the game being solved
	 */
	public RangeFileManager(FileSystem fs,
			org.apache.hadoop.conf.Configuration hadoopConf, Game<S> game) {
		try {
			SequenceFile.Reader reader;
			reader = new SequenceFile.Reader(fs, new Path(
					hadoopConf.get("db.map.path")), hadoopConf);
			ArrayList<RangeFile> ranges = new ArrayList<RangeFile>();
			while (true) {
				Range r = new Range();
				Text text = new Text();
				if (!reader.next(r, text))
					break;
				ranges.add(new RangeFile(r, text));
			}
			reader.close();

			Collections.sort(ranges);

			rangeFiles = ranges.toArray(new RangeFile[ranges.size()]);

			hashesPerFile = game.numHashes() / ranges.size();
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	/**
	 * @param positionHash
	 *            the hash of the position we're doing lookup for
	 * @return the range file that contains the given hash
	 */
	public RangeFile getFile(long positionHash) {
		int guess = Math.min((int) (positionHash / hashesPerFile),
				rangeFiles.length - 1);
		// initial guess at the location
		while (rangeFiles[guess].myRange.firstRecord > positionHash) {
			guess--;
			// we guessed too high
		}

		while (rangeFiles[guess].myRange.firstRecord
				+ rangeFiles[guess].myRange.numRecords < positionHash) {
			guess++;
			// guessed too low
		}

		return rangeFiles[guess];
	}
}
