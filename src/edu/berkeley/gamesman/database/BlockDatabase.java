package edu.berkeley.gamesman.database;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.Iterator;

import edu.berkeley.gamesman.core.RecordGroup;
import edu.berkeley.gamesman.util.Util;

/**
 * The BlockDatabase operates on local files, and utilizes the Byte-oriented
 * interface of the Record instead of stream.
 * 
 * @author Steven Schlansker
 */
public class BlockDatabase extends FileDatabase {

	MappedByteBuffer buf;

	@Override
	public void close() {
		flush();
		try {
			fd.getChannel().close();
			super.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void flush() {
		buf.force();
		super.flush();
	}

	@Override
	public RecordGroup getRecordGroup(long loc) {
		buf.position((int) loc);
		buf.get(rawRecord);
		return new RecordGroup(conf, rawRecord);
	}

	@Override
	public void initialize(String loc) {
		super.initialize(loc);
		try {
			buf = fd.getChannel()
					.map(MapMode.READ_WRITE, offset, getByteSize());
			buf.load();
		} catch (IOException e) {
			Util.fatalError("Could not map ByteBuffer from blockdb", e);
		}
	}

	@Override
	public synchronized void putRecordGroup(long loc, RecordGroup value) {
		buf.position((int) loc);
		value.outputUnsignedBytes(buf);
	}

	@Override
	public Iterator<RecordGroup> getRecordGroups(long loc, int numGroups) {
		groupsLength = numGroups * conf.recordGroupByteLength;
		if (groups == null || groups.length < groupsLength)
			groups = new byte[groupsLength];
		buf.position((int) (loc + offset));
		buf.get(groups);
		RecordGroupByteIterator rgi = new RecordGroupByteIterator();
		return rgi;
	}

	@Override
	public void putRecordGroups(long loc, Iterator<RecordGroup> recordGroups,
			int numGroups) {
		groupsLength = numGroups * conf.recordGroupByteLength;
		if (groups == null || groups.length < groupsLength)
			groups = new byte[groupsLength];
		int onByte = 0;
		for (int i = 0; i < numGroups; i++)
			recordGroups.next().toUnsignedByteArray(groups, onByte);
		buf.position((int) (loc + offset));
		buf.put(groups);
	}
}
