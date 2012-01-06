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
	public final Path childrenFolder;
	public final Path parentsFolder;
	public final Path needsCreatePath;
	public final Path needsPropogatePath;
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
		childrenFolder = ConfParser.getChildrenPath(conf, num);
		IOCheckOperations.mkdirs(fs, childrenFolder);
		parentsFolder = ConfParser.getParentsPath(conf, num);
		IOCheckOperations.mkdirs(fs, parentsFolder);
		needsCreatePath = ConfParser.getNeedsCreationPath(conf, num);
		needsPropogatePath = ConfParser.getNeedsPropogationPath(conf, num);
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
		return fs.exists(needsCreatePath);
	}

	private static final String CHILD_PREF = "c";
	private static final String PARENT_PREF = "p";

	private Tier[] getTierList(Path fold, String pref) throws IOException {
		Path[] paths = FileUtil.stat2Paths(fs.listStatus(fold));
		int[] children = new int[paths.length];
		for (int i = 0; i < paths.length; i++) {
			String name = paths[i].getName();
			assert name.substring(0, pref.length()).equals(pref);
			int iPart = Integer.parseInt(name.substring(pref.length()));
			children[i] = iPart;
		}
		Tier[] result = new Tier[children.length];
		for (int i = 0; i < children.length; i++) {
			result[i] = graph.getTier(children[i]);
		}
		return result;
	}

	public synchronized Tier[] getChildren() throws IOException {
		return getTierList(childrenFolder, CHILD_PREF);
	}

	public synchronized Tier[] getParents() throws IOException {
		return getTierList(parentsFolder, PARENT_PREF);
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

	public synchronized void replaceDataPath(Path name) throws IOException {
		assert validFolder(name);
		IOCheckOperations.delete(fs, dataPath, true);
		IOCheckOperations.rename(fs, name, dataPath);
	}

	public synchronized void setNeedsCreation() throws IOException {
		IOCheckOperations.createNewFile(fs, needsCreatePath);
	}

	public synchronized boolean startCreation() {
		return startSomething(needsCreatePath);
	}

	private boolean startSomething(Path filePath) {
		try {
			return IOCheckOperations.delete(fs, filePath, false);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized boolean addChild(int childTier) throws IOException {
		return IOCheckOperations.createNewFile(fs, new Path(childrenFolder,
				CHILD_PREF + childTier));
	}

	public synchronized boolean addParent(int parentTier) throws IOException {
		return IOCheckOperations.createNewFile(fs, new Path(parentsFolder,
				PARENT_PREF + parentTier));
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

	public synchronized void addCombine(Path folder) throws IOException {
		assert validFolder(folder);
		IOCheckOperations.rename(fs, folder, makeCombinePath());
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
		return startSomething(needsPropogatePath);
	}

	public synchronized boolean needsToPropogate() throws IOException {
		return fs.exists(needsPropogatePath);
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
		IOCheckOperations.createNewFile(fs, needsPropogatePath);
	}

	public Path getTempFolder() {
		assert holdsLock();
		return tempFolder;
	}
}
