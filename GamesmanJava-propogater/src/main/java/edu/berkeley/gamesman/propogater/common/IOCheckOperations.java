package edu.berkeley.gamesman.propogater.common;

import java.io.IOException;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public class IOCheckOperations {
	public static boolean mkdirs(FileSystem fs, Path p) throws IOException {
		boolean result = !fs.exists(p);
		fs.mkdirs(p);
		if (!fs.exists(p) || fs.isFile(p))
			throw new IOException("mkdirs failed for " + p);
		return result;
	}

	public static boolean delete(FileSystem fs, Path p, boolean recursive)
			throws IOException {
		boolean result = fs.delete(p, recursive);
		if (fs.exists(p))
			throw new IOException("Delete failed for " + p);
		return result;
	}

	public static boolean createNewFile(FileSystem fs, Path p)
			throws IOException {
		assert fs.exists(p.getParent());
		boolean result = fs.createNewFile(p);
		if (!fs.exists(p) || !fs.isFile(p)) {
			throw new IOException("createNewFile failed for " + p);
		}
		return result;
	}

	public static void rename(FileSystem fs, Path p1, Path p2)
			throws IOException {
		assert fs.exists(p2.getParent());
		if (!fs.rename(p1, p2))
			throw new IOException("Rename failed for " + p1 + " to " + p2);
	}
}
