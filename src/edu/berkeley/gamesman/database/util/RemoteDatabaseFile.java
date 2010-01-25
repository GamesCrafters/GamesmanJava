package edu.berkeley.gamesman.database.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;

import edu.berkeley.gamesman.util.Util;

public class RemoteDatabaseFile {
	private BufferedWriter ddWriter;
	private InputStream byteReader;
	private String filePointer;
	private boolean firstByteRead = false;
	private byte firstByte;
	private Thread skipper;

	public void close() {
		try {
			String s = "exit\n";
			ddWriter.write(s);
			ddWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void getBytes(long loc, byte[] arr, int off, int len) {
		try {
			int skipBytes = (int) (loc & 511L);
			String s = "dd if=" + filePointer + " skip=" + (loc >> 9)
					+ " count=" + ((len + skipBytes + 511) >> 9) + "\n";
			ddWriter.write(s);
			ddWriter.flush();
			if (!firstByteRead) {
				skipper.join();
				if (skipBytes > 0) {
					--skipBytes;
					firstByteRead = true;
				}
			}
			byte[] dumbArray = new byte[skipBytes];
			while (skipBytes > 0)
				skipBytes -= byteReader.read(dumbArray, 0, skipBytes);
			int bytesRead;
			if (!firstByteRead) {
				bytesRead = 1;
				arr[off] = firstByte;
				firstByteRead = true;
			} else
				bytesRead = 0;
			while (bytesRead < len) {
				bytesRead += byteReader.read(arr, off + bytesRead, len
						- bytesRead);
			}
			loc += bytesRead;
			int remainBytes = 512 - (int) (loc & 511L);
			dumbArray = new byte[remainBytes];
			while (remainBytes > 0)
				remainBytes -= byteReader.read(dumbArray, 0, remainBytes);
		} catch (IOException e) {
			Util.fatalError("Could not write request", e);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public synchronized void initialize(String uri) {
		try {
			Runtime r = Runtime.getRuntime();
			String s = "ssh " + uri.substring(0, uri.indexOf(':')) + "\n";
			final Process p = r.exec(s);
			filePointer = uri.substring(uri.indexOf(':') + 1);
			ddWriter = new BufferedWriter(new OutputStreamWriter(p
					.getOutputStream()));
			byteReader = p.getInputStream();
			final Thread waiter = new Thread() {
				public synchronized void run() {
					try {
						wait(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			};
			skipper = new Thread() {
				public void run() {
					while (waiter.isAlive()) {
						byte b = 0;
						try {
							b = (byte) byteReader.read();
						} catch (IOException e) {
							e.printStackTrace();
						}
						if (waiter.isAlive())
							System.out.print((char) b);
						else {
							firstByte = b;
							System.out.println();
						}
					}
				}
			};
			waiter.start();
			skipper.start();
			waiter.join();
		} catch (IOException e1) {
			Util.fatalError("Can't ssh", e1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
