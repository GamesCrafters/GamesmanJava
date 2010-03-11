package edu.berkeley.gamesman.database;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.RecordGroup;
import edu.berkeley.gamesman.game.TieredGame;
import edu.berkeley.gamesman.hasher.TieredHasher;
import edu.berkeley.gamesman.util.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;

/**
 * @author dnspies Zips Tiers while solving
 */
public class TierZippedDatabase extends Database {
	private FileDatabase lastTier;
	private FileDatabase thisTier;
	private File[] solvedTiers;
	private GZippedFileDatabase[] zippedTiers;
	private long tierOffset = Long.MAX_VALUE;
	private long lastTierOffset = Long.MAX_VALUE;
	private int tier;
	private File parent;
	private File confFile;
	private TieredGame<?> game;
	private TieredHasher<?> hasher;

	@Override
	public void close() {
		if (thisTier != null) {
			writeOut(thisTier);
			thisTier.close();
			thisTier.myFile.delete();
		}
		if (lastTier != null) {
			lastTier.close();
			lastTier.myFile.delete();
		}
	}

	@Override
	public void flush() {
	}

	/**
	 * Open a FileDatabase for the tier that will be solved next and GZip the
	 * current tier.
	 * 
	 * @param n
	 *            The tier that will be solved next
	 */
	public void setTier(int n) {
		tier = n;
		if (lastTier != null) {
			lastTier.close();
			lastTier.myFile.delete();
		}
		if (thisTier != null) {
			thisTier.flush();
			writeOut(thisTier);
			lastTier = thisTier;
			lastTierOffset = tierOffset;
			thisTier = null;
		}
		thisTier = new FileDatabase();
		String name = (tier & 1) == 1 ? "oddTier.db" : "evenTier.db";
		thisTier.setSingleTier(conf, tier);
		thisTier.initialize(name, conf, true);
		tierOffset = hasher.hashOffsetForTier(tier) / conf.recordsPerGroup
				* conf.recordGroupByteLength;
	}

	private void writeOut(FileDatabase fd) {
		fd.flush();
		solvedTiers[tier] = new File(parent, "tier" + (tier < 10 ? "0" : "")
				+ tier);
		GZippedFileDatabase.createFromFile(fd, solvedTiers[tier], false);
	}

	@Override
	public void getBytes(byte[] arr, int off, int len) {
		lastTier.getBytes(arr, off, len);
	}

	@Override
	public void getBytes(long loc, byte[] arr, int off, int len) {
		if (zippedTiers == null)
			lastTier.getBytes(loc, arr, off, len);
		else {
			int tier = game.hashToTier(loc / conf.recordGroupByteLength
					* conf.recordsPerGroup);
			int finishTier = tier;
			long nextRecord = (loc + len) / conf.recordGroupByteLength
					* conf.recordsPerGroup;
			do {
				++finishTier;
			} while (game.hashOffsetForTier(finishTier) < nextRecord);
			boolean firstCombine = false;
			for (int i = tier; i < finishTier; i++) {
				long startByte = game.hashOffsetForTier(i)
						/ conf.recordsPerGroup * conf.recordGroupByteLength;
				long tierLen = (game.hashOffsetForTier(i + 1)
						+ conf.recordsPerGroup - 1)
						/ conf.recordsPerGroup
						* conf.recordGroupByteLength
						- startByte;
				long underLoc = loc - startByte;
				long bytesToTierEnd = tierLen + startByte - loc;
				int numBytes = (int) Math.min(len, bytesToTierEnd);
				long firstPart = 0L;
				BigInteger bigFirstPart = null;
				if (firstCombine) {
					if (conf.recordGroupUsesLong) {
						firstPart = RecordGroup.longRecordGroup(conf, arr, off);
					} else {
						bigFirstPart = RecordGroup.bigIntRecordGroup(conf, arr,
								off);
					}
				}
				zippedTiers[i].getBytes(underLoc, arr, off, numBytes);
				if (firstCombine) {
					if (conf.recordGroupUsesLong) {
						long secondPart = RecordGroup.longRecordGroup(conf,
								arr, off);
						RecordGroup.toUnsignedByteArray(conf, firstPart
								+ secondPart, arr, off);
					} else {
						BigInteger bigSecondPart = RecordGroup
								.bigIntRecordGroup(conf, arr, off);
						RecordGroup.toUnsignedByteArray(conf, bigFirstPart
								.add(bigSecondPart), arr, off);
					}
				}
				off += numBytes;
				len -= numBytes;
				firstCombine = (numBytes >= bytesToTierEnd)
						&& game.hashOffsetForTier(i + 1) % conf.recordsPerGroup != 0;
				if (firstCombine)
					off -= conf.recordGroupByteLength;
			}
		}
	}

	@Override
	public void initialize(String uri, boolean solve) {
		confFile = new File(uri);
		parent = new File(uri + "_folder");
		if (!parent.exists())
			parent.mkdir();
		try {
			if (solve) {
				confFile.createNewFile();
				if (conf == null)
					Util
							.fatalError("You must specify a configuration if the database is to be created");
				game = Util.checkedCast(conf.getGame());
				hasher = Util.checkedCast(conf.getHasher());
				solvedTiers = new File[game.numberOfTiers()];
				byte[] b = conf.store();
				FileOutputStream writer = new FileOutputStream(confFile);
				for (int i = 24; i >= 0; i -= 8)
					writer.write(b.length >> i);
				writer.write(b);
			} else {
				int headerLen = 0;
				FileInputStream scan = new FileInputStream(confFile);
				for (int i = 24; i >= 0; i -= 8) {
					headerLen <<= 8;
					headerLen |= scan.read();
				}
				byte[] header = new byte[headerLen];
				int offset = 0;
				while (headerLen > 0) {
					int bytesRead = scan.read(header, offset, headerLen);
					offset += bytesRead;
					headerLen -= bytesRead;
				}
				conf = Configuration.load(header);
				game = Util.checkedCast(conf.getGame());
				hasher = Util.checkedCast(conf.getHasher());
				zippedTiers = new GZippedFileDatabase[game.numberOfTiers()];
				for (int i = 0; i < zippedTiers.length; i++) {
					zippedTiers[i] = new GZippedFileDatabase();
					zippedTiers[i].setSingleTier(i);
					zippedTiers[i].initialize(parent.getPath()
							+ File.separatorChar + "tier"
							+ (tier < 10 ? "0" : "") + tier, conf, false);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			Util.fatalError("Conf instantiation failed", e);
		}
	}

	@Override
	public void putBytes(long loc, byte[] arr, int off, int len) {
		thisTier.putBytes(loc, arr, off, len);
	}

	@Override
	public void putBytes(byte[] arr, int off, int len) {
		thisTier.putBytes(arr, off, len);
	}

	@Override
	public void seek(long loc) {
		if (loc > lastTierOffset)
			lastTier.seek(loc);
		else {
			thisTier.seek(loc);
		}
	}
}
