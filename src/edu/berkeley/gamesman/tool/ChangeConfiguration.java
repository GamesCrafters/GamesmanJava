package edu.berkeley.gamesman.tool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import edu.berkeley.gamesman.core.Configuration;

public class ChangeConfiguration {
	public static void main(String[] args) throws IOException,
			ClassNotFoundException {
		String database = args[0];
		String jobFile = args[1];
		String newDatabase = args[0] + ".tmp";
		FileInputStream fis = new FileInputStream(database);
		FileOutputStream fos = new FileOutputStream(newDatabase);
		Configuration.skipConf(fis);
		Configuration conf = new Configuration(Configuration
				.readProperties(jobFile));
		conf.store(fos);
		byte[] copyBytes = new byte[65536];
		int bytesCopied = 0;
		long remaining = fis.getChannel().size() - fis.getChannel().position();
		while (remaining > 0) {
			bytesCopied = fis.read(copyBytes);
			if (bytesCopied >= 0)
				fos.write(copyBytes, 0, bytesCopied);
			else {
				fos.write(copyBytes, 0, (int) remaining);
				break;
			}
			remaining -= bytesCopied;
		}
		fis.close();
		fos.close();
		File df = new File(database);
		df.delete();
		new File(newDatabase).renameTo(df);
	}
}
