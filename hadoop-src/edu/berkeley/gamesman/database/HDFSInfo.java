package edu.berkeley.gamesman.database;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;

public class HDFSInfo {
	private static FileSystem hdfs;

	public static void initialize(Configuration conf) throws IOException {
		hdfs = FileSystem.get(conf);
	}

	public static FileSystem getHDFS() throws IOException {
		if (hdfs == null) {
			hdfs = FileSystem.get(new Configuration());
		}
		return hdfs;
	}
}
