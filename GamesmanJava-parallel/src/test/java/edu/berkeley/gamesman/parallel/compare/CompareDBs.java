//package edu.berkeley.gamesman.parallel.compare;
//
//import java.io.IOException;
//
//import junit.framework.Assert;
//
//import org.apache.hadoop.conf.Configuration;
//import org.apache.hadoop.fs.Path;
//import org.apache.hadoop.io.MapFile;
//import org.apache.hadoop.io.MapFile.Reader;
//import org.apache.hadoop.mapreduce.Partitioner;
//import org.apache.hadoop.mapreduce.lib.output.MapFileOutputFormat;
//import org.apache.hadoop.util.GenericOptionsParser;
//
//import edu.berkeley.gamesman.game.type.GameRecord;
//import edu.berkeley.gamesman.hasher.genhasher.GenHasher;
//import edu.berkeley.gamesman.hasher.genhasher.GenState;
//import edu.berkeley.gamesman.parallel.ranges.ChildMap;
//import edu.berkeley.gamesman.parallel.ranges.MainRecords;
//import edu.berkeley.gamesman.parallel.ranges.Suffix;
//import edu.berkeley.gamesman.parallel.ranges.RangeTree;
//import edu.berkeley.gamesman.parallel.ranges.RecordMap;
//import edu.berkeley.gamesman.propogater.common.ConfParser;
//
//public class CompareDBs {
//	public static <S extends GenState> void main(String[] args)
//			throws IOException, ClassNotFoundException {
//		GenericOptionsParser parser = new GenericOptionsParser(args);
//		Configuration conf = parser.getConfiguration();
//		String[] remainArgs = parser.getRemainingArgs();
//
//		RangeTree<S>[] treeList = new RangeTree[remainArgs.length];
//		Suffix<S>[] posRange = new Suffix[treeList.length];
//		MainRecords[] recs = new MainRecords[treeList.length];
//		boolean[] unused = new boolean[treeList.length];
//		MapFile.Reader[][] readers = new MapFile.Reader[remainArgs.length][];
//		Partitioner<Suffix<S>, MainRecords>[] partitioner = new Partitioner[remainArgs.length];
//		for (int i = 0; i < remainArgs.length; i++) {
//			Path p = new Path(remainArgs[i]);
//			Configuration tConf = new Configuration(conf);
//			ConfParser.addParameters(tConf, p, false);
//			treeList[i] = (RangeTree<S>) ConfParser
//					.<Suffix<S>, MainRecords, ChildMap, RecordMap, RecordMap, ChildMap> newTree(tConf);
//			Path[] outPath = new Path[1];
//			outPath[0] = ConfParser.getOutputPath(tConf);
//			readers[i] = MapFileOutputFormat.getReadersArray(outPath, tConf);
//			partitioner[i] = ConfParser
//					.<Suffix<S>, MainRecords> getPartitionerInstance(tConf);
//			recs[i] = new MainRecords();
//		}
//
//		long numPositions = treeList[0].getHasher().totalPositions();
//		for (long pos = 0; pos < numPositions; pos++) {
//			GameRecord checkAgainst = null;
//			for (int i = 0; i < treeList.length; i++) {
//				GenHasher<S> hasher = treeList[i].getHasher();
//				S position = hasher.newState();
//				hasher.unhash(pos, position);
//
//				if (posRange[i] == null || !posRange[i].matches(position)) {
//					if (posRange[i] == null)
//						posRange[i] = new Suffix<S>();
//					treeList[i]
//							.makeOutputContainingRange(position, posRange[i]);
//					MainRecords result = (MainRecords) MapFileOutputFormat
//							.getEntry(readers[i], partitioner[i], posRange[i],
//									recs[i]);
//					if (result == null)
//						unused[i] = true;
//					else
//						unused[i] = false;
//				}
//				GameRecord record = null;
//				if (!unused[i]) {
//					record = treeList[i].getRecord(posRange[i], position,
//							recs[i]);
//					if (checkAgainst == null)
//						checkAgainst = record;
//					else
//						Assert.assertEquals(checkAgainst, record);
//				}
//				if (pos % 100000 == 0) {
//					System.out.println(position);
//					System.out.println(record);
//				}
//			}
//		}
//
//		for (Reader[] readerList : readers) {
//			for (Reader r : readerList)
//				r.close();
//		}
//	}
//}
