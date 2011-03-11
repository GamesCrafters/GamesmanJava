package edu.berkeley.gamesman.database;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;

public class HDFSInfo {
	private static Configuration conf;
	private static FileSystem hdfs;

	public static void initialize(Configuration conf) throws IOException {
		HDFSInfo.conf = conf;
		hdfs = FileSystem.get(conf);
	}

	public static FileSystem getHDFS() throws IOException {
		if (hdfs == null) {
			conf = new Configuration();
			hdfs = FileSystem.get(conf);
		}
		return hdfs;
	}
}
