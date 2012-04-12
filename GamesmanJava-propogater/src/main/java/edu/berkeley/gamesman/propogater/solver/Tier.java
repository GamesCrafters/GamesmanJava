package edu.berkeley.gamesman.propogater.solver;

import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;

import edu.berkeley.gamesman.propogater.common.ConfParser;
import edu.berkeley.gamesman.propogater.common.IOCheckOperations;

public class Tier implements Comparable<Tier> {
	private final FileSystem fs;
	private final TierGraph graph;
	public final int num;
	public final String extension;
	public final Path workPath;
	public final Path dataPath;
	public final Path combineFolder;
	public final Path outputFolder;
	private final Path tempFolder;
	private final String workPathString;
	private final String workPathAbsString;
	public final PathFilter filter = new PathFilter() {
		@Override
		public boolean accept(Path path) {
			return Solver.underscoreFilter.accept(path)
					&& path.getName().endsWith(extension);
		}

		@Override
		public String toString() {
			return "Checking for " + extension;
		}
	};

	private Thread holdingThread = null;
	private int cpNum = 0;
	private long numRecords = 0;
	private long numCombineRecords = 0;
	private final TreeSet<Tier> parents = new TreeSet<Tier>();
	private final TreeSet<Tier> children = new TreeSet<Tier>();
	private boolean needsCreation = false, needsPropogation = false;

	public Tier(Configuration conf, int num, TierGraph g) throws IOException {
		this.graph = g;
		this.num = num;
		extension = String.format(ConfParser.EXTENSION_FORMAT, num);
		workPath = ConfParser.getTierPath(conf, num);
		workPathString = workPath.toString();
		try {
			fs = workPath.getFileSystem(conf);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		IOCheckOperations.mkdirs(fs, workPath);
		workPathAbsString = fs.getFileStatus(workPath).getPath().toString();
		dataPath = new Path(workPath, ConfParser.DATA_FOLDER);
		IOCheckOperations.mkdirs(fs, dataPath);
		combineFolder = new Path(workPath, ConfParser.COMBINE_FOLDER);
		IOCheckOperations.mkdirs(fs, combineFolder);
		outputFolder = new Path(workPath, ConfParser.OUTPUT_FOLDER);
		tempFolder = new Path(workPath, ConfParser.TEMP_FOLDER);
	}

	public boolean needsToCombine() throws IOException {
		return getCombinePaths().length > 0;
	}

	public synchronized Path[] getCombinePaths() throws IOException {
		return FileUtil.stat2Paths(fs.listStatus(combineFolder));
	}

	public synchronized boolean needsToCreate() throws IOException {
		return needsCreation;
	}

	public synchronized SortedSet<Tier> getChildren() throws IOException {
		return children;
	}

	public synchronized SortedSet<Tier> getParents() throws IOException {
		return parents;
	}

	public synchronized void lock() {
		if (holdingThread != null)
			throw new ConcurrentModificationException("Lock  already held");
		holdingThread = Thread.currentThread();
	}

	public synchronized void unlock() {
		holdingThread = null;
	}

	public synchronized boolean hasData() throws IOException {
		return fs.listStatus(dataPath).length > 0;
	}

	public synchronized Path makeCombinePath() {
		return new Path(combineFolder, String.format(ConfParser.CP_FORMAT,
				cpNum++));
	}

	public synchronized void renameDataPath(Path newName) throws IOException {
		assert validFolder(newName);
		IOCheckOperations.rename(fs, dataPath, newName);
	}

	public synchronized void deleteCombinedPaths(Path[] allPaths)
			throws IOException {
		for (Path path : allPaths) {
			assert validFolder(path);
			IOCheckOperations.delete(fs, path, true);
		}
	}

	public synchronized void replaceDataPath(Path name, long numPositions)
			throws IOException {
		assert validFolder(name);
		IOCheckOperations.delete(fs, dataPath, true);
		IOCheckOperations.rename(fs, name, dataPath);
		setNumRecords(numPositions);
	}

	public synchronized void setNeedsCreation() throws IOException {
		needsCreation = true;
	}

	public synchronized boolean startCreation() {
		if (needsCreation) {
			needsCreation = false;
			return true;
		} else
			return false;
	}

	public synchronized boolean addChild(int childTier) throws IOException {
		return children.add(graph.getTier(childTier));
	}

	public synchronized boolean addParent(int parentTier) throws IOException {
		return parents.add(graph.getTier(parentTier));
	}

	public synchronized boolean createTempFolder() throws IOException {
		assert holdsLock();
		return IOCheckOperations.mkdirs(fs, tempFolder);
	}

	public synchronized void moveToTemp(Path p) throws IOException {
		assert holdsLock();
		IOCheckOperations.rename(fs, p, new Path(tempFolder, p.getName()));
	}

	private boolean holdsLock() {
		return Thread.currentThread() == holdingThread;
	}

	public synchronized void addCombine(Path folder, long numPositions)
			throws IOException {
		assert validFolder(folder);
		IOCheckOperations.rename(fs, folder, makeCombinePath());
		addCombineRecords(numPositions);
	}

	private boolean validFolder(Path folder) {
		return folder.getName().equals(ConfParser.TEMP_FOLDER)
				|| folder.toString().startsWith(workPathString)
				|| folder.toString().startsWith(workPathAbsString);
	}

	public synchronized Path[] getCreationOutputFiles(PathFilter filt)
			throws IOException {
		return FileUtil.stat2Paths(fs.listStatus(outputFolder, filt));
	}

	public synchronized boolean deleteOutputFolder() throws IOException {
		return IOCheckOperations.delete(fs, outputFolder, true);
	}

	public Set<Tier> getDependences() throws IOException {
		Set<Tier> descendents = getDescendents();
		Set<Tier> ancestors = getAncestors();
		ancestors.removeAll(descendents);
		assert !ancestors.contains(this);
		return ancestors;
	}

	private SortedSet<Tier> getAncestors() throws IOException {
		return getAncestors(new TreeSet<Tier>());
	}

	private SortedSet<Tier> getAncestors(SortedSet<Tier> seen)
			throws IOException {
		if (seen.contains(this))
			return seen;
		else
			seen.add(this);
		for (Tier parent : getParents()) {
			parent.getAncestors(seen);
		}
		return seen;
	}

	private SortedSet<Tier> getDescendents() throws IOException {
		return getDescendents(new TreeSet<Tier>());
	}

	private SortedSet<Tier> getDescendents(SortedSet<Tier> seen)
			throws IOException {
		if (seen.contains(this))
			return seen;
		else
			seen.add(this);
		for (Tier child : getChildren()) {
			child.getDescendents(seen);
		}
		return seen;
	}

	public synchronized boolean startPropogation() {
		if (needsPropogation) {
			needsPropogation = false;
			return true;
		} else
			return false;
	}

	public synchronized boolean needsToPropogate() throws IOException {
		return needsPropogation;
	}

	public SortedSet<Tier> getCycle() throws IOException {
		SortedSet<Tier> descendents = getDescendents();
		SortedSet<Tier> ancestors = getAncestors();
		ancestors.retainAll(descendents);
		assert ancestors.contains(this);
		return ancestors;
	}

	@Override
	public int compareTo(Tier o) {
		return num - o.num;
	}

	public Set<Tier> getReverseDependences() throws IOException {
		Set<Tier> descendents = getDescendents();
		Set<Tier> ancestors = getAncestors();
		descendents.removeAll(ancestors);
		assert !descendents.contains(this);
		return descendents;
	}

	public synchronized void setNeedsPropogation() throws IOException {
		needsPropogation = true;
	}

	public Path getTempFolder() {
		assert holdsLock();
		return tempFolder;
	}

	public synchronized void setNumRecords(long poses) throws IOException {
		numRecords = poses;
	}

	public synchronized long getNumRecords() throws IOException {
		return numRecords;
	}

	public synchronized long getNumCombineRecords() {
		return numCombineRecords;
	}

	public synchronized void addCombineRecords(long numRecords) {
		numCombineRecords += numRecords;
	}

	public synchronized void clearCombineRecords() {
		numCombineRecords = 0L;
	}

	public synchronized void printStats() {
		synchronized (System.out) {
			System.out.println("Tier " + num);
			System.out.println("\tNum Records:" + numRecords);
			System.out.println("\tNum Combine Records:" + numCombineRecords);
			System.out.println("\tParents: " + parents);
			System.out.println("\tChildren: " + children);
			if (needsCreation)
				System.out.println("\tNeeds Creation");
			if (needsPropogation)
				System.out.println("\tNeeds Propogation");
		}
	}

	public String toString() {
		return Integer.toString(num);
	}
}
