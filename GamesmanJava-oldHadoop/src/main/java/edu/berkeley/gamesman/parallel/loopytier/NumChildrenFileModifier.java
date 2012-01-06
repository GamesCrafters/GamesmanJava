package edu.berkeley.gamesman.parallel.loopytier;

import java.io.IOException;
import java.util.Random;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.ArrayFile;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile.CompressionType;

import edu.berkeley.gamesman.parallel.RangeFile;

/**
 * @author Eric A class for modifying and reading the array files used to store
 *         the number of children
 */
public class NumChildrenFileModifier {
	private ArrayFile.Reader numChildrenReader = null;
	private ArrayFile.Writer numChildrenWriter = null;

	private FileSystem fs;

	private Path numChildrenPath;
	private Path numChildrenTempPath;

	/**
	 * @param rangeFile
	 *            the range file that contains the range and db file info
	 * @param fs
	 *            the hdfs file system
	 * @param conf
	 *            the hadoop configuration
	 * @throws IOException
	 *             if something goes horribly wrong
	 */
	public NumChildrenFileModifier(RangeFile rangeFile, FileSystem fs,
			org.apache.hadoop.conf.Configuration conf) throws IOException {
		this.fs = fs;
		
		Random rand = new Random();
		
		String numChildrenPathString = rangeFile.myFile.toString()
				+ "_numChildren";
		String numChildrenTempPathString = numChildrenPathString + "_"
				+ rand.nextLong();

		numChildrenPath = new Path(numChildrenPathString);
		numChildrenTempPath = new Path(numChildrenTempPathString);

		if (fs.exists(numChildrenPath)) {
			numChildrenReader = new ArrayFile.Reader(fs, numChildrenPathString,
					conf);
		}

		numChildrenWriter = new ArrayFile.Writer(conf, fs,
				numChildrenTempPathString, IntWritable.class,
				CompressionType.BLOCK, null);
	}

	/**
	 * Gets the children count for the next position, does nothing if the
	 * modifier is making a new file
	 * 
	 * @param numChildren
	 *            the int writable to put the children count into
	 * @throws IOException
	 *             if something goes horribly wrong
	 */
	public void readNextChildrenCount(IntWritable numChildren)
			throws IOException {
		if (numChildrenReader != null) {
			numChildrenReader.next(numChildren);
		}
	}

	/**
	 * @param numChildren
	 *            the children count to write
	 * @throws IOException
	 *             if something goes horribly wrong
	 */
	public void writeNextChildrenCount(IntWritable numChildren)
			throws IOException {
		numChildrenWriter.append(numChildren);
	}

	/**
	 * Closes the files and cleans up
	 * @throws IOException if something goes horribly wrong
	 */
	public void closeAndClean() throws IOException {
		if (numChildrenReader != null) {
			numChildrenReader.close();
		}
		numChildrenWriter.close();

		if (fs.exists(numChildrenPath)) {
			fs.delete(numChildrenPath, true);
		}
		
		fs.rename(numChildrenTempPath, numChildrenPath);
	}
}
