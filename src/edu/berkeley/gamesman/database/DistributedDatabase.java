package edu.berkeley.gamesman.database;

import java.io.PrintStream;
import java.util.Scanner;

import edu.berkeley.gamesman.core.Configuration;

public class DistributedDatabase extends Database {

	private final Scanner readStream;
	private final PrintStream requestStream;

	public DistributedDatabase(Configuration conf, long firstRecord,
			long numRecords, DatabaseHeader header, Scanner readStream,
			PrintStream requestStream) {
		super(null, conf, false, firstRecord, numRecords, header);
		this.readStream = readStream;
		this.requestStream = requestStream;
	}

	@Override
	protected void closeDatabase() {
		readStream.close();
		requestStream.close();
	}

	@Override
	protected void getBytes(DatabaseHandle dh, long loc, byte[] arr, int off,
			int len) {
		getRecordsAsBytes(dh, loc, 0, arr, off, len, 0, true);
	}

	@Override
	protected void putBytes(DatabaseHandle dh, long loc, byte[] arr, int off,
			int len) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected synchronized void prepareRange(DatabaseHandle dh, long byteIndex,
			int firstNum, long numBytes, int lastNum) {
		super.prepareRange(dh, byteIndex, firstNum, numBytes, lastNum);
		DistributedHandle distHand = (DistributedHandle) dh;
		long firstRecord = toFirstRecord(byteIndex) + firstNum;
		long lastRecord = toLastRecord(byteIndex + numBytes)
				- (lastNum == 0 ? 0 : (recordsPerGroup - lastNum));
		long numRecords = lastRecord - firstRecord;
		requestStream.println("fetch: " + firstRecord + " " + numRecords);
		distHand.sd = new SplitDatabase(null, conf, false, firstRecord,
				numRecords, getHeader(firstRecord, numRecords), readStream);
		distHand.innerHandle = distHand.sd.getHandle();
		distHand.sd.prepareRange(distHand.innerHandle, byteIndex, firstNum,
				numBytes, lastNum);
	}

	@Override
	protected int getBytes(DatabaseHandle dh, byte[] arr, int off, int maxLen,
			boolean overwriteEdgesOk) {
		DistributedHandle distH = (DistributedHandle) dh;
		int numBytes = distH.sd.getBytes(distH.innerHandle, arr, off, maxLen,
				overwriteEdgesOk);
		distH.location += numBytes;
		if (distH.location == distH.lastByteIndex)
			distH.sd.close();
		return numBytes;
	}

	private class DistributedHandle extends DatabaseHandle {
		public SplitDatabase sd;

		public DistributedHandle() {
			super(null);
		}
	}

	@Override
	public DistributedHandle getHandle() {
		return new DistributedHandle();
	}
}
