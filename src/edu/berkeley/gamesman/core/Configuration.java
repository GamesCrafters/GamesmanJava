package edu.berkeley.gamesman.core;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;

import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseWrapper;
import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.util.Util;

/**
 * A Configuration object stores information related to a specific configuration
 * of Game, Hasher, and Records. The information should be specific enough that
 * a database will only match Configuration if the given Game and Hasher will
 * derive useful information from it.
 * 
 * @author Steven Schlansker
 */
public class Configuration {
	private Game<?> g;

	/**
	 * The number of possible states for value. When zero, the record does not
	 * contain value.
	 */
	public int valueStates;
	/**
	 * The number of possible states for remoteness. When zero, the record does
	 * not contain remoteness.
	 */
	public int remotenessStates;
	/**
	 * The number of possible states for score. When zero, the record does not
	 * contain score.
	 */
	public int scoreStates;

	/**
	 * The database associated with this configuration
	 */
	public Database db;

	/**
	 * The properties used to create this configuration
	 */
	public final Properties props;

	/**
	 * Reads the key value pairs from the given job file into a Properties
	 * object.
	 * 
	 * @param path
	 *            A path to a job file
	 * @return A Properties object containing all the key-value pairs from the
	 *         given job file.
	 */
	public static Properties readProperties(String path) {
		Properties props = new Properties();
		addProperties(props, path);
		return props;
	}

	/**
	 * Given a Properties, will construct a Configuration
	 * 
	 * @param props
	 *            A Properties object (probably constructed from a job file).
	 * @param initLater
	 *            You must call initialize() once you have created the
	 *            appropriate Game and Hasher objects.
	 * @throws ClassNotFoundException
	 *             Could not find game class or hasher class
	 */
	public Configuration(Properties props) throws ClassNotFoundException {
		this.props = props;
		String gamename = getProperty("gamesman.game");
		initialize(gamename);
	}

	/**
	 * Calls new Configuration(Configuration.readProperties(path))
	 * 
	 * @param path
	 *            The path to the job file to read
	 * @throws ClassNotFoundException
	 *             Could not find game class or hasher class
	 */
	public Configuration(String path) throws ClassNotFoundException {
		this(readProperties(path));
	}

	// To specify the size, use ':' followed by the number of possible
	// states
	private void initializeStoredFields() {
		String fields = getProperty("record.fields", "VALUE,REMOTENESS");
		int states;
		String[] splitFields = fields.split(",");
		valueStates = 0;
		remotenessStates = 0;
		scoreStates = 0;
		for (int i = 0; i < splitFields.length; i++) {
			String[] splt = splitFields[i].split(":");
			if (splt.length > 1)
				states = Integer.parseInt(splt[1]);
			else
				states = 0;
			if (splt[0].trim().equalsIgnoreCase("VALUE")) {
				if (states == 0)
					valueStates = g.defaultValueStates();
				else
					valueStates = states;
			} else if (splt[0].trim().equalsIgnoreCase("REMOTENESS")) {
				if (states == 0)
					remotenessStates = g.defaultRemotenessStates();
				else
					remotenessStates = states;
			} else if (splt[0].trim().equalsIgnoreCase("SCORE")) {
				if (states == 0)
					scoreStates = g.defaultScoreStates();
				else
					scoreStates = states;
			}
		}
	}

	/**
	 * Initialize the Configuration with a game and a hasher object.
	 * 
	 * @param newG
	 *            The Game associated with this configuration.
	 */
	public void initialize(Game<?> newG) {
		g = newG;
		initializeStoredFields();
	}

	/**
	 * Initialize the Configuration with a game and a hasher object.
	 * 
	 * @param in_gamename
	 *            The Game associated with this configuration.
	 * @param in_hashname
	 *            The Hasher associated with this configuration.
	 * @throws ClassNotFoundException
	 *             Could not load either the hasher or game class
	 */
	public void initialize(final String in_gamename) {
		String gamename = in_gamename;

		// Python classes end with ".py"
		if (gamename.indexOf('.') == -1) {
			gamename = "edu.berkeley.gamesman.game." + gamename;
		}
		setProperty("gamesman.game", gamename);
		try {
			g = Class.forName(gamename).asSubclass(Game.class).getConstructor(
					Configuration.class).newInstance(this);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.getCause().printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		initialize(g);
	}

	/**
	 * @return the Game this configuration plays
	 */
	public Game<?> getGame() {
		return g;
	}

	public String toString() {
		return "Config[" + props + "," + g + "]";
	}

	/**
	 * Get a property by its name
	 * 
	 * @param key
	 *            the name of the configuration property
	 * @return its value
	 */
	public String getProperty(String key) {
		String s = props.getProperty(key);
		if (s == null)
			Util
					.fatalError("Property " + key
							+ " is unset and has no default!");
		return s;
	}

	/**
	 * For python compatibility.
	 * 
	 * @param key
	 *            the name of the configuration property
	 * @return its value
	 * @see #getProperty(String)
	 */
	public String __getitem__(String key) {
		return getProperty(key);
	}

	/**
	 * For python compatibility. Returns false if key does not exist.
	 * 
	 * @param key
	 *            The key to check if it's in the configuration
	 * @return is the key in the configuration?
	 * @see #getProperty(String)
	 */
	public boolean __contains__(String key) {
		return props.containsKey(key);
	}

	/**
	 * Parses a property as a boolean.
	 * 
	 * @param key
	 *            the name of the configuration property
	 * @param dfl
	 *            default value
	 * @return false iff s is "false" or s is "0", ignoring case
	 */
	public boolean getBoolean(String key, boolean dfl) {
		String s = props.getProperty(key);
		if (s == null)
			return dfl;
		else
			return !s.equalsIgnoreCase("false") && !s.equalsIgnoreCase("0");
	}

	/**
	 * Parses a property as an Integer.
	 * 
	 * @param key
	 *            the name of the configuration property
	 * @param dfl
	 *            default value
	 * @return The value associated with the key, if defined and an integer.
	 *         Otherwise, returns dfl.
	 */
	public int getInteger(String key, int dfl) {
		String value = props.getProperty(key);
		if (value != null)
			return Integer.parseInt(value);
		else
			return dfl;
	}

	/**
	 * Parses a property as an Integer.
	 * 
	 * @param key
	 *            the name of the configuration property
	 * @param dfl
	 *            default value
	 * @return The value associated with the key, if defined and an integer.
	 *         Otherwise, returns dfl.
	 */
	public float getFloat(String key, float dfl) {
		String value = props.getProperty(key);
		if (value == null)
			return dfl;
		else
			return Float.parseFloat(value);
	}

	/**
	 * Parses a property as a Long.
	 * 
	 * @param key
	 *            the name of the configuration property
	 * @param dfl
	 *            default value
	 * @return The value associated with the key, if defined and an long.
	 *         Otherwise, returns dfl.
	 */
	public long getLong(String key, long dfl) {
		String lString = props.getProperty(key);
		if (lString == null)
			return dfl;
		else
			return Long.parseLong(lString);
	}

	/**
	 * Parses a property as an array of Integers separated by the regex ", *"
	 * 
	 * @param key
	 *            the name of the configuration property
	 * @param dfl
	 *            default value
	 * @return The value associated with the key, if defined and an integer
	 *         array. Otherwise, returns dfl.
	 */
	public int[] getInts(String key, int[] dfl) {
		String iString = props.getProperty(key);
		if (iString == null)
			return dfl;
		else
			return Util.parseInts(iString.split(", *"));
	}

	/**
	 * Get a property by its name. If the property is not set, return dfl
	 * 
	 * @param key
	 *            the name of the configuration property
	 * @param dfl
	 *            default value
	 * @return its value
	 */
	public String getProperty(String key, String dfl) {
		return props.getProperty(key, dfl);
	}

	/**
	 * Set a property by its name
	 * 
	 * @param key
	 *            the name of the configuration property to set
	 * @param value
	 *            the new value
	 * @return the old value
	 */
	public Object setProperty(String key, String value) {
		return props.setProperty(key, value);
	}

	/**
	 * Read a list of properties from a file The properties should be specified
	 * as key = value pairs. Blank lines are ignored.
	 * 
	 * @param path
	 *            The file path to open
	 */
	private static void addProperties(Properties props, String path) {
		LineNumberReader r = null;
		try {
			r = new LineNumberReader(new FileReader(path));
		} catch (FileNotFoundException e) {
			Util.fatalError("Could not open property file", e);
		}
		String line;
		try {
			while ((line = r.readLine()) != null) {
				line = line.trim();
				if (line.equals("") || line.charAt(0) == '#')
					continue;
				String[] arr = line.split("=", 2); // semantics are slightly
				// different from python
				// The following line can't be here because it causes the Util
				// class to be loaded before
				// we're ready for it.
				// Util.assertTrue(arr.length == 2, "Malformed property file at
				// line \""+line+"\"");
				props.setProperty(arr[0].trim(), arr[1].trim());
			}
		} catch (IOException e) {
			Util.fatalError("Could not read from property file", e);
		}
	}

	/**
	 * Add all the properties into this configuration. Property strings should
	 * be split by = signs.
	 * 
	 * @param propStrings
	 *            the list of properties to set.
	 */
	public void addProperties(ArrayList<String> propStrings) {
		for (String line : propStrings) {
			if (line.equals(""))
				continue;
			String[] arr = line.split("\\s+=\\s+");
			Util.assertTrue(arr.length == 2,
					"Malformed property file at line \"" + line + "\"");
			setProperty(arr[0], arr[1]);
		}
	}

	/**
	 * @return all keys in the configuration
	 */
	public Set<Object> getKeys() {
		return props.keySet();
	}

	/**
	 * Remove a key from the configuration
	 * 
	 * @param key
	 *            the key to remove
	 */
	public void deleteProperty(String key) {
		props.remove(key);
	}

	private static BufferedReader in = new BufferedReader(
			new InputStreamReader(System.in));

	/**
	 * Return a property, prompting the user if it doesn't exist
	 * 
	 * @param key
	 *            the key to get
	 * @return its value
	 */
	public String getPropertyWithPrompt(String key) {
		String s = props.getProperty(key);
		if (s == null) {
			System.out
					.print("Gamesman would like you to specify the value for '"
							+ key + "'\n\t>");
			System.out.flush();
			try {
				return in.readLine();
			} catch (IOException e) {
				Util.fatalError("Could not read a line from console", e);
				return null;
			}
		}
		return s;
	}

	/**
	 * @param solve
	 *            true for solving, false for playing
	 * @return the Database used to store this particular solve
	 * @throws ClassNotFoundException
	 *             Could not load the database class
	 */
	public Database openDatabase(String uri, boolean solve)
			throws ClassNotFoundException {
		return openDatabase(uri, solve, 0, -1);
	}

	/**
	 * @param solve
	 *            true for solving, false for playing
	 * @param firstRecord
	 *            The index of the first record this database contains
	 * @param numRecord
	 *            The number of records in this database
	 * @return the Database used to store this particular solve
	 * @throws ClassNotFoundException
	 *             Could not load the database class
	 */
	public Database openDatabase(String uri, boolean solve, long firstRecord,
			long numRecord) throws ClassNotFoundException {
		if (db != null)
			return db;
		if (uri != null)
			setProperty("gamesman.db.uri", uri);
		String[] dbType = getProperty("gamesman.database").split(":");
		try {
			Class<? extends Database> dbClass = Util.checkedCast(Class
					.forName("edu.berkeley.gamesman.database."
							+ dbType[dbType.length - 1]));
			db = dbClass.newInstance();
			for (int i = dbType.length - 2; i >= 0; i--) {
				Class<? extends DatabaseWrapper> wrapperClass = Util
						.checkedCast(Class
								.forName("edu.berkeley.gamesman.database."
										+ dbType[i]));
				db = wrapperClass.getConstructor(Database.class)
						.newInstance(db);
			}
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.getCause().printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		if (numRecord >= 0)
			db.setRange(firstRecord, numRecord);
		db.initialize(getProperty("gamesman.db.uri"), this, solve);
		return db;
	}

	/**
	 * @return A new identical configuration with clones of the game and hasher
	 */
	public Configuration cloneAll() {
		try {
			Configuration newConf = new Configuration(props);
			newConf.db = db;
			return newConf;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * @param key
	 *            The key which maps to the Integer array
	 * @param dfl
	 *            The default if the given key isn't present
	 * @return An array of integers separated by ", *"
	 */
	public Integer[] getIntegers(String key, Integer[] dfl) {
		String iString = props.getProperty(key);
		if (iString == null)
			return dfl;
		else
			return Util.parseIntegers(iString.split(", *"));
	}

	public static void storeNone(OutputStream os) throws IOException {
		for (int i = 0; i < 4; i++)
			os.write(0);
	}

	public static void skipConf(InputStream is) throws IOException {
		int confLength = 0;
		for (int i = 0; i < 4; i++) {
			confLength <<= 8;
			confLength |= is.read();
		}
		byte[] skippedBytes = new byte[confLength];
		is.read(skippedBytes);
	}

	public void store(OutputStream os) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		props.store(baos, "");
		byte[] confBytes = baos.toByteArray();
		for (int i = 24; i >= 0; i -= 8) {
			os.write(confBytes.length >> i);
		}
		os.write(confBytes);
	}

	public static Configuration load(InputStream is) throws IOException,
			ClassNotFoundException {
		int confLength = 0;
		for (int i = 0; i < 4; i++) {
			confLength <<= 8;
			confLength |= is.read();
		}
		byte[] confBytes = new byte[confLength];
		is.read(confBytes);
		Properties props = new Properties();
		props.load(new ByteArrayInputStream(confBytes));
		return new Configuration(props);
	}
}