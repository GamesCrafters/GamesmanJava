package edu.berkeley.gamesman.database;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public class ChangeFolderPaths {
	public static void main(String[] args) throws ClassNotFoundException,
			IOException, URISyntaxException {
		String databaseFile = args[0];
		String oldPath = args[1];
		String newPath = args[2];
		changeDB(databaseFile, databaseFile, oldPath, newPath, true);
	}

	private static void changeDB(String databaseFile, String sendTo,
			String oldPath, String newPath, boolean andUnderlying)
			throws IOException, URISyntaxException {
		DataInputStream in = new DataInputStream(new FileInputStream(
				databaseFile));
		byte[] firstHeader = new byte[16];
		in.readFully(firstHeader);
		int numBytes = in.readInt();
		byte[] secondHeader = new byte[numBytes];
		in.readFully(secondHeader);
		String newName = databaseFile + "_tmp";
		DataOutputStream out = new DataOutputStream(new FileOutputStream(
				newName));
		out.write(firstHeader);
		out.writeInt(numBytes);
		out.write(secondHeader);
		while (true) {
			String className;
			try {
				className = in.readUTF();
			} catch (EOFException e) {
				break;
			}
			out.writeUTF(className);
			String filename = in.readUTF();
			if (andUnderlying)
				replaceHDFSFile(filename, oldPath, newPath);
			String replacedname = filename.replaceFirst(oldPath, newPath);
			System.out.println(filename + " becomes " + replacedname);
			out.writeUTF(replacedname);
			out.writeLong(in.readLong());
			out.writeLong(in.readLong());
		}
		in.close();
		out.close();
		new File(databaseFile).delete();
		new File(newName).renameTo(new File(sendTo));
	}

	private static void replaceHDFSFile(String filename, String oldPath,
			String newPath) throws IOException, URISyntaxException {
		Path path = new Path(filename);
		String localFileName = filename + "_local";
		FileSystem hdfs = HDFSInfo.getHDFS(filename);
		hdfs.copyToLocalFile(path, new Path(localFileName));
		String outFile = localFileName + "_tmp";
		changeDB(localFileName, outFile, oldPath, newPath, false);
		hdfs.copyFromLocalFile(new Path(outFile), path);
	}
}
