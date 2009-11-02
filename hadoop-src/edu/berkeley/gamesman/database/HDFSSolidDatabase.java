package edu.berkeley.gamesman.database;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

/**
 * Simple HDFS database implementing the SolidDatabase interface.
 * @author Patrick Reiter Horn
 */
public class HDFSSolidDatabase extends SolidDatabase {

	private FileSystem fs;

	HDFSSolidDatabase(FileSystem fs) {
		this.fs = fs;
	}

	@Override
	protected InputStream openInputStream(String uri) throws IOException {
		return fs.open(new Path(new Path("file:///"), uri));
	}

	@Override
	protected OutputStream openOutputStream(String uri) throws IOException {
		// TODO Auto-generated method stub
		return fs.create(new Path(new Path("file:///"), uri));
	}

}
