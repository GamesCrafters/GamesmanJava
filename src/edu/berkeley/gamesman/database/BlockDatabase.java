package edu.berkeley.gamesman.database;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;

import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Util;
import edu.berkeley.gamesman.util.bytes.ByteBufferStorage;
import edu.berkeley.gamesman.util.bytes.ByteStorage;

/**
 * The BlockDatabase is a slightly more packed version of the FileDatabase.
 * It also operates on local files, and utilizes the Byte-oriented interface of
 * the Record instead of stream.
 * @author Steven Schlansker
 */
public class BlockDatabase extends FileDatabase {

	private final int headerSize = 8;

	MappedByteBuffer buf;
	ByteStorage wrappedbuf;

	long lastRecord;

	@Override
	public void close() {
		flush();
		try {
			buf.force();
			fd.seek(offset);
			fd.writeLong(lastRecord);
			assert Util.debug(DebugFacility.DATABASE, "Database has "+lastRecord+" records");
			long dblen = offset + headerSize + (Record.bitlength(conf)*lastRecord+7)/8;
			assert Util.debug(DebugFacility.DATABASE, "Attempting to truncate to "+dblen+"(+1)");
			fd.getChannel().force(true);
			fd.getChannel().close();
		} catch (IOException e) {
			Util.warn("Could not cleanly close BlockDB",e);
		}
		buf = null;
		super.close();
	}

	@Override
	public void flush() {
		try {
			fd.seek(offset);
			fd.writeLong(lastRecord);
		} catch (IOException e) {
			Util.warn("Could not flush BlockDB",e);
		}
		buf.force();
		super.flush();
	}
	

	@Override
	public Record getRecord(BigInteger loc) {
		lastRecord = Math.max(lastRecord,loc.longValue());
		return Record.read(conf, wrappedbuf, loc.longValue());
	}
	
	@Override
	public Record getRecord(BigInteger loc, Record rec) {
		rec.read(wrappedbuf, loc.longValue());
		return rec;
	}

	@Override
	public void initialize(String loc) {
		super.initialize(loc);
		try {
			long recordCount = conf.getLong("gamesman.BlockDatabase.size", conf.getGame().lastHash().longValue()+1);
			long max = Record.bytelength(conf, recordCount);
			max += headerSize;
			buf = fd.getChannel().map(MapMode.READ_WRITE, offset + headerSize, max);
			assert Util.debug(DebugFacility.DATABASE, "Mapped BlockDatabase into memory starting from "+(offset+headerSize));
			fd.seek(offset);
			lastRecord = fd.readLong();
			wrappedbuf = new ByteBufferStorage(buf);
		} catch (IOException e) {
			Util.fatalError("Could not map ByteBuffer from blockdb", e);
		}
	}

	@Override
	public void putRecord(BigInteger loc, Record value) {
		lastRecord = Math.max(lastRecord,loc.longValue());
		value.write(wrappedbuf, loc.longValue());
		assert Util.debug(DebugFacility.DATABASE, "Stored record "+value+" to "+loc);
	}
	
}
