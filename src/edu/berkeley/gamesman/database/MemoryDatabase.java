package edu.berkeley.gamesman.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Util;

/**
 * Test DataBase for GamesCrafters Java. Right now it just writes BigIntegers to
 * memory, without byte padding.
 * 
 * 
 * @author Alex Trofimov
 * @version 1.4
 * 
 *          Change log: 05/05/09 - 1.4 - putByte() is now synchronized, for
 *          multi-threading. This is really important. 03/20/09 - 1.3 - With
 *          data sizes < 58 bits, longs are used instead of BigInts, 20%
 *          speedup. 03/15/09 - 1.2 - Slight speedup for operating on small data
 *          (< 8 bits); ensureCapacity() added. 02/22/09 - 1.1 - Switched to a
 *          byte[] instead of ArrayList<Byte> for internal storage. 02/21/09 -
 *          1.0 - Initial (working) Version.
 */
public class MemoryDatabase extends Database {
	/* Class Variables */
	private byte[] memoryStorage; // byte array to store the data

	protected boolean reading;

	private int nextPlace = 0;

	private File myFile;

	@Override
	public void initialize(String location) {
		System.out.println(getByteSize());
		this.memoryStorage = new byte[(int) getByteSize()];
		try {
			myFile = new File(location);
			if (myFile.exists()) {
				reading = true;
				try {
					FileInputStream fis = new FileInputStream(myFile);
					int confLength = 0;
					for (int i = 24; i >= 0; i -= 8) {
						confLength <<= 8;
						confLength |= fis.read();
					}
					System.out.println(confLength);
					byte[] b = new byte[confLength];
					fis.read(b);
					if (conf == null)
						conf = Configuration.load(b);
					fis.read(memoryStorage);
					fis.close();
				} catch (IOException e) {
					Util.fatalError("IO Error", e);
				} catch (ClassNotFoundException e) {
					Util.fatalError("Class Not Found", e);
				}
			} else {
				reading = false;
				myFile.createNewFile();
			}
		} catch (IOException e) {
			Util.fatalError("IO Error", e);
		}
	}

	@Override
	public void flush() {
		assert Util.debug(DebugFacility.DATABASE,
				"Flushing Memory DataBase. Does Nothing.");
	}

	@Override
	public void close() {
		if (!reading) {
			try {
				FileOutputStream fos = new FileOutputStream(myFile);
				byte[] b = conf.store();
				for (int i = 24; i >= 0; i -= 8)
					fos.write(b.length >>> i);
				fos.write(b);
				fos.write(memoryStorage);
				fos.close();
			} catch (IOException e) {
				Util.fatalError("IO Error", e);
			}
		}
	}

	/**
	 * Must be synchronized by caller
	 */
	@Override
	public void getBytes(byte[] arr, int off, int len) {
		for (int i = 0; i < len; i++)
			arr[off++] = memoryStorage[nextPlace++];
	}

	/**
	 * Must be synchronized by caller
	 */
	@Override
	public void putBytes(byte[] arr, int off, int len) {
		for (int i = 0; i < len; i++)
			memoryStorage[nextPlace++] = arr[off++];
	}

	@Override
	public void seek(long loc) {
		nextPlace = (int) loc;
	}
}
