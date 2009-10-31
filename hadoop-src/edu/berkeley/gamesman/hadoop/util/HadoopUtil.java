package edu.berkeley.gamesman.hadoop.util;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
//import org.apache.hadoop.mapred.JobConf;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.hadoop.TierMap;
/**
 * Utilities for the Hadoop master
 * @author Patrick Reiter Horn
 */
public class HadoopUtil {
	/**
	 * MapReduceDatabase abstract parent for children 
	 *
	 */
	public static abstract class MapReduceDatabase extends Database {
		/**
		 * Default constructor, so this can be instantiated from a class name.
		 */
		public MapReduceDatabase() {
		}
	
		/**
		 * Convenience constructor. Equivalent to calling setFilesystem
		 * @param fs Reference to hadoop FileSystem.
		 */
		public MapReduceDatabase(FileSystem fs) {
			this.fs = fs;
		}
	
		/**
		 * All hadoop classes that need to access the disk need a FileSystem instance.
		 * Must be set before the database is used.
		 * @param fs The hadoop filesystem.
		 */
		public void setFilesystem(FileSystem fs) {
			this.fs = fs;
		}
	
		/**
		 * Called by the mapper to allow the database to communicate via
		 * TierMap.started() and TierMap.finished().
		 * @param tmr TierMap instance.
		 */
		void setDelegate(TierMap<?> tmr) {
			this.delegate = tmr;
		}
	
		/**
		 * Called by the mapper to tell the database where to dump output files.
		 * @param dir FileOutputFormat.getWorkOutputPath(jobconf));
		 */
		public void setOutputDirectory(Path dir) {
			outputFilenameBase = dir;
			// dir contains a trailing slash
		}
	
		protected TierMap<?> delegate;
	
		protected FileSystem fs;
	
		protected Path outputFilenameBase;
	}

	/**
	 * @param gmConf Gamesman Configuration
	 * @return The toplevel hadoop solve directory.
	 */
	public static Path getParentPath(Configuration gmConf) {
		return new Path(gmConf.getProperty("gamesman.db.uri"));
	}
	/**
	 * @param tier The tier number in the solve process
	 * @return The directory name for that tier (currently assumes 2 digits).
	 */
	public static String getTierDirectoryName(int tier) {
		return String.format("tier%02d", tier);
	}
	/**
	 * @param gmConf Gamesman Configuration
	 * @param tier The tier number in the solve process
	 * @return The full Path to the directory for that tier.
	 */
	public static Path getTierPath(Configuration gmConf, int tier) {
		return new Path(getParentPath(gmConf), getTierDirectoryName(tier));
	}

	/**
	 * @param tier The tier number in the solve process
	 * @return The name of the file that contains the index in the database.
	 */
	public static String getTierIndexFilename(int tier) {
		return tier+".hdb";
	}

	/**
	 * @param gmConf Gamesman Configuration
	 * @param tier The tier number in the solve process
	 * @return The full path to the file that contains the index in the database.
	 */
	public static Path getTierIndexPath(Configuration gmConf, int tier) {
		return new Path(getTierPath(gmConf, tier), getTierIndexFilename(tier));
	}
}
