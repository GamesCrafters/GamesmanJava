package edu.berkeley.gamesman.hadoop.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Writable;

/**
 * SplitDatabaseOutputFormat represents a series of hashes and the corresponding
 * database filename. This is used as the output to Map and the input to Reduce.
 * 
 * @see SplitDatabaseWritableList
 * @author Patrick Horn
 */
public class SplitDatabaseWritable implements Writable {
	String filename;
	long start;
	long end;
	int tier;

	/**
	 * Default constructor, given tier.
	 * @param tier Which tier in the solving process. Used to determine directory name.
	 */
	public SplitDatabaseWritable(int tier) {
		this.tier = tier;
	}

	/**
	 * @param filename Filename of the newly written database.
	 * @param startRecord First record in database.
	 * @param stopRecord 1 + Last record in database
	 */
	public void set(String filename, long startRecord, long stopRecord) {
		this.filename = filename;
		this.start = startRecord;
		this.end = stopRecord;
		if ((end-start) > 1000000000 || end <= start) {
			throw new RuntimeException("Error: file "+filename+" goes from "+start+" to "+end);
		}
	}
	public void readFields(DataInput in) throws IOException {
		filename = in.readUTF();
		start = in.readLong();
		end = start + in.readInt();
	}

        public void write(DataOutput out) throws IOException {
                out.writeUTF(getFilename());
                out.writeLong(getStart());
                out.writeInt(getLength());
	}

	/**
	 * @return tier number for this database.
	 */
	public int getTier() {
		return tier;
	}

	/**
	 * @return database filename
	 */
	public String getFilename(){
		return filename;
	}

	/**
	 * @return first record in DB.
	 */
	public long getStart() {
		return start;
	}

	/**
	 * @return 1 + last record in DB.
	 */
	public long getEnd() {
		return end;
	}

	/**
	 * @return getEnd() - getStart(), cast to int.
	 */
	public int getLength() {
       		if ((end-start) > 1000000000 || end <= start) {
			System.out.println("Error: file "+filename+" goes from "+start+" to "+end);
		}
		return (int)(end-start);
	}

	public String toString(){
		return filename+"["+start+":"+end+"]";
	}

}

