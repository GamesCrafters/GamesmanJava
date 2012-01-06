package edu.berkeley.gamesman.database;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;

public class HDFSInfo {
	private static Configuration conf = null;

	public static void initialize(Configuration conf) throws IOException {
		if (HDFSInfo.conf == null)
			HDFSInfo.conf = conf;
	}

	public static FileSystem getHDFS(String uri) throws IOException,
			URISyntaxException {
		if (conf == null)
			conf = new Configuration();
		return FileSystem.get(new URI(uri), conf);
	}
}
