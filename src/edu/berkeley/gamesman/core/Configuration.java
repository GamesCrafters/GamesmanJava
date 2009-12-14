package edu.berkeley.gamesman.core;

import java.io.*;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;

import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Util;
import edu.berkeley.gamesman.util.biginteger.BigInteger;

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

	private Hasher<?> h;

	/**
	 * 
	 */
	public long totalStates;

	protected BigInteger bigIntTotalStates;

	BigInteger[] multipliers;

	long[] longMultipliers;

	/**
	 * The number of records contained in a RecordGroup
	 */
	public int recordsPerGroup;

	/**
	 * The number of bytes in a RecordGroup
	 */
	public int recordGroupByteLength;

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
	 * Whether the record group size is small enough to fit in a long. If this
	 * is not true, solving is slowed immensely
	 */
	public boolean recordGroupUsesLong;

	/**
	 * The database associated with this configuration
	 */
	public Database db;

	/**
	 * The properties used to create this configuration
	 */
	public final Properties props;

	/**
	 * Should records be compressed by grouping them as integers of the
	 * appropriate base?
	 */
	public boolean superCompress;

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
	public Configuration(Properties props, boolean initLater)
			throws ClassNotFoundException {
		this.props = props;
		if (!initLater) {
			String gamename = getProperty("gamesman.game");
			String hashname = getProperty("gamesman.hasher");
			initialize(gamename, hashname);
		}
	}

	/**
	 * Given a Properties, will construct a Configuration
	 * 
	 * @param props
	 *            A Properties object (probably constructed from a job file).
	 * @throws ClassNotFoundException
	 *             Could not find game class or hasher class
	 */
	public Configuration(Properties props) throws ClassNotFoundException {
		this(props, false);
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
		this(readProperties(path), false);
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
					valueStates = 4;
				else
					valueStates = states;
			} else if (splt[0].trim().equalsIgnoreCase("REMOTENESS")) {
				if (states == 0)
					remotenessStates = 2 << 6;
				else
					remotenessStates = states;
			} else if (splt[0].trim().equalsIgnoreCase("SCORE")) {
				if (states == 0)
					scoreStates = 2 << 4;
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
	 * @param newH
	 *            The Hasher associated with this configuration.
	 */
	public void initialize(Game<?> newG, Hasher<?> newH) {
		initialize(g, h, true);
	}

	/**
	 * Initialize the Configuration with a game and a hasher object.
	 * 
	 * @param newG
	 *            The Game associated with this configuration.
	 * @param newH
	 *            The Hasher associated with this configuration.
	 * @param prepare
	 *            Whether to call prepare for the game being passed
	 */
	public void initialize(Game<?> newG, Hasher<?> newH, boolean prepare) {
		g = newG;
		h = newH;
		if (prepare) {
			g.prepare();
		}
		initializeStoredFields();
		totalStates = g.recordStates();
		double requiredCompression = Double.parseDouble(getProperty(
				"record.compression", "0")) / 100;
		double compression;
		if (requiredCompression == 0D) {
			superCompress = false;
			int bits = (int) (Math.log(totalStates) / Math.log(2));
			if ((1 << bits) < totalStates)
				++bits;
			recordGroupByteLength = (bits + 7) >> 3;
			recordsPerGroup = 1;
		} else {
			superCompress = true;
			int recordGuess;
			int bitLength;
			double log2;
			log2 = Math.log(totalStates) / Math.log(2);
			if (log2 > 8) {
				recordGuess = 1;
				bitLength = (int) Math.ceil(log2);
				compression = (log2 / 8) / ((bitLength + 7) >> 3);
				while (compression < requiredCompression) {
					recordGuess++;
					bitLength = (int) Math.ceil(recordGuess * log2);
					compression = (recordGuess * log2 / 8)
							/ ((bitLength + 7) >> 3);
				}
			} else {
				bitLength = 8;
				recordGuess = (int) (8D / log2);
				compression = recordGuess * log2 / 8;
				while (compression < requiredCompression) {
					bitLength += 8;
					recordGuess = (int) (bitLength / log2);
					compression = (recordGuess * log2 / 8) / (bitLength >> 3);
				}
			}
			recordsPerGroup = recordGuess;
			multipliers = new BigInteger[recordsPerGroup + 1];
			BigInteger multiplier = BigInteger.ONE;
			bigIntTotalStates = BigInteger.valueOf(totalStates);
			for (int i = 0; i <= recordsPerGroup; i++) {
				multipliers[i] = multiplier;
				multiplier = multiplier.multiply(bigIntTotalStates);
			}
			recordGroupByteLength = (bigIntTotalStates.pow(recordsPerGroup)
					.bitLength() + 7) >> 3;
		}
		if (recordGroupByteLength < 8) {
			recordGroupUsesLong = true;
			longMultipliers = new long[recordsPerGroup + 1];
			long longMultiplier = 1;
			for (int i = 0; i <= recordsPerGroup; i++) {
				longMultipliers[i] = longMultiplier;
				longMultiplier *= totalStates;
			}
		} else {
			recordGroupUsesLong = false;
			longMultipliers = null;
		}
		Util.debug(DebugFacility.CORE, recordsPerGroup + " records per group\n"
				+ recordGroupByteLength + " bytes per group");
	}

	/**
	 * Initialize the Configuration with a game and a hasher object.
	 * 
	 * @param in_gamename
	 *            The Game associated with this configuration.
	 * @param in_hashname
	 *            The Hasher associated with this configuration.
	 * @param prepare
	 *            Whether to call prepare for the game being passed
	 * @throws ClassNotFoundException
	 *             Could not load either the hasher or game class
	 */
	public void initialize(final String in_gamename, final String in_hashname,
			boolean prepare) throws ClassNotFoundException {
		String gamename = in_gamename, hashname = in_hashname;

		// Python classes end with ".py"
		if (gamename.indexOf('.') == -1) {
			gamename = "edu.berkeley.gamesman.game." + gamename;
		}
		setProperty("gamesman.game", gamename);
		if (hashname.indexOf('.') == -1) {
			hashname = "edu.berkeley.gamesman.hasher." + hashname;
		}
		setProperty("gamesman.hasher", hashname);
		Game<?> g = Util.typedInstantiateArg(gamename, Game.class, this);
		Hasher<?> h = Util.typedInstantiateArg(hashname, Hasher.class, this);
		initialize(g, h, prepare);
	}

	/**
	 * @param gamename
	 *            The name of the game class
	 * @param hashname
	 *            The name of the hash class
	 * @throws ClassNotFoundException
	 *             If either class is not found
	 */
	public void initialize(String gamename, String hashname)
			throws ClassNotFoundException {
		initialize(gamename, hashname, true);
	}

	/**
	 * @return the Game this configuration plays
	 */
	public Game<?> getGame() {
		return g;
	}

	/**
	 * Unserialize a configuration from a bytestream
	 * 
	 * @param barr
	 *            Bytes to deserialize
	 * @return a Configuration
	 * @throws ClassNotFoundException
	 *             Could not find game class or hasher class
	 */
	public static Configuration load(byte[] barr) throws ClassNotFoundException {
		try {
			DataInputStream instream = new DataInputStream(
					new ByteArrayInputStream(barr));
			Properties props = new Properties();

			byte[] t = new byte[instream.readInt()];
			instream.readFully(t);
			ByteArrayInputStream bin = new ByteArrayInputStream(t);
			props.load(bin);
			Configuration conf = new Configuration(props, true);

			// assert Util.debug(DebugFacility.CORE, "Deserialized
			// properties:\n"+props);

			String gamename = instream.readUTF();
			String hashername = instream.readUTF();

			conf.valueStates = instream.readInt();
			conf.remotenessStates = instream.readInt();
			conf.scoreStates = instream.readInt();

			try {
				conf.g = Util.typedInstantiateArg(gamename, Game.class, conf);
			} catch (ClassNotFoundException e) {
				conf.g = Util.typedInstantiateArg(conf
						.getProperty("gamesman.game"), Game.class, conf);
			}
			try {
				conf.h = Util.typedInstantiateArg(hashername, Hasher.class,
						conf);
			} catch (ClassNotFoundException e) {
				conf.h = Util.typedInstantiateArg(conf
						.getProperty("gamesman.hasher"), Hasher.class, conf);
			}

			conf.initialize(conf.g, conf.h);

			return conf;
		} catch (IOException e) {
			Util.fatalError(
					"Could not resuscitate Configuration from bytes :(\n"
							+ new String(barr), e);
		}
		return null;
	}

	/**
	 * Return a serialized version of the Configuration suitable for storing
	 * persistently
	 * 
	 * @return a String with the Configuration information
	 */
	public byte[] store() {

		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(baos);

			ByteArrayOutputStream baos2 = new ByteArrayOutputStream();

			props.store(baos2, null);

			out.writeInt(baos2.size());
			out.write(baos2.toByteArray());

			baos2.close();

			out.writeUTF(g.getClass().getCanonicalName());
			out.writeUTF(h.getClass().getCanonicalName());

			out.writeInt(valueStates);
			out.writeInt(remotenessStates);
			out.writeInt(scoreStates);

			out.close();

			return baos.toByteArray();
		} catch (IOException e) {
			Util.fatalError("IO Exception shouldn't have happened here", e);
			return null;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Configuration))
			return false;
		Configuration c = (Configuration) o;
		return c.props.equals(props) && c.g.getClass().equals(g.getClass())
				&& c.h.getClass().equals(h.getClass());
	}

	/**
	 * @return the Hasher this Configuration is using
	 */
	public Hasher<?> getHasher() {
		return h;
	}

	public String toString() {
		return "Config[" + props + "," + g + "," + h + "]";
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
		if (value == null)
			return dfl;
		else
			return Integer.parseInt(value);

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
	public Integer[] getIntegers(String key, Integer[] dfl) {
		String iString = props.getProperty(key);
		if (iString == null)
			return dfl;
		else
			return Util.parseIntegers(iString.split(", *"));
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
	 * @return the Database used to store this particular solve
	 * @throws ClassNotFoundException
	 *             Could not load the database class
	 */
	public Database openDatabase() throws ClassNotFoundException {
		if (db != null)
			return db;
		String[] dbType = getProperty("gamesman.database").split(":");
		if (dbType.length > 1 && dbType[0].trim().equals("cached")) {
			db = Util.typedInstantiate("edu.berkeley.gamesman.database."
					+ dbType[1], Database.class);
		} else {
			db = Util.typedInstantiate("edu.berkeley.gamesman.database."
					+ dbType[0], Database.class);
		}
		db.initialize(getProperty("gamesman.db.uri"), this);
		return db;
	}

	/**
	 * @return A new identical configuration with clones of the game and hasher
	 */
	public Configuration cloneAll() {
		try {
			Configuration newConf = new Configuration(props, false);
			return newConf;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}
}