package edu.berkeley.gamesman.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import java.util.Set;

import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.util.Util;

/**
 * A Configuration object stores information related to a specific configuration
 * of Game and Records. The information should be specific enough that a
 * database will only match Configuration if the given Game will derive useful
 * information from it. Every database begins with the 18-byte header followed
 * by the configuration data. Use the load and store methods to read and write
 * the configuration from the database.
 * 
 * @author Steven Schlansker
 */
public final class Configuration {
	private final Game<?> g;

	/**
	 * Does this game use remoteness?
	 */
	public final boolean hasRemoteness;

	/**
	 * Does this game use score?
	 */
	public final boolean hasScore;

	/**
	 * Does this game use value? (false for puzzles)
	 */
	public final boolean hasValue;

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
	 * @throws ClassNotFoundException
	 *             Could not find game class or hasher class
	 */
	public Configuration(Properties props) throws ClassNotFoundException {
		this.props = props;
		Properties p = new Properties();
		String gamename = getProperty("gamesman.game");
		String[] gamenames = gamename.split(":");
		gamename = gamenames[gamenames.length - 1];
		// Python classes end with ".py"
		if (gamename.indexOf('.') == -1) {
			gamename = "edu.berkeley.gamesman.game." + gamename;
		}
		Game<?> g;
		try {
			g = Util.typedForName(gamename, Game.class)
					.getConstructor(Configuration.class).newInstance(this);
			for (int i = gamenames.length - 2; i >= 0; i--) {
				gamename = gamenames[i];
				if (gamename.indexOf('.') == -1) {
					gamename = "edu.berkeley.gamesman.game." + gamename;
				}
				g = Util.typedForName(gamename, Game.class)
						.getConstructor(Game.class).newInstance(g);
			}
		} catch (IllegalArgumentException e) {
			throw new Error(e);
		} catch (SecurityException e) {
			throw new Error(e);
		} catch (InstantiationException e) {
			throw new Error(e);
		} catch (IllegalAccessException e) {
			throw new Error(e);
		} catch (InvocationTargetException e) {
			throw new Error(e.getCause());
		} catch (NoSuchMethodException e) {
			throw new Error(e);
		}
		this.g = g;
		String fields = getProperty("record.fields", "VALUE,REMOTENESS");
		String[] splitFields = fields.split(",");
		boolean hasValue = false, hasRemoteness = false, hasScore = false;
		for (int i = 0; i < splitFields.length; i++) {
			if (splitFields[i].equalsIgnoreCase("VALUE"))
				hasValue = true;
			else if (splitFields[i].equalsIgnoreCase("REMOTENESS"))
				hasRemoteness = true;
			else if (splitFields[i].equalsIgnoreCase("SCORE"))
				hasScore = true;
		}
		if (!g.hasValue())
			hasValue = false;
		if (!g.hasRemoteness())
			hasRemoteness = false;
		if (!g.hasScore())
			hasScore = false;
		this.hasValue = hasValue;
		this.hasScore = hasScore;
		this.hasRemoteness = hasRemoteness;
	}

	/**
	 * Given a job file, will construct a Configuration
	 * 
	 * @param solvedJob
	 *            The name of the job file
	 * @throws ClassNotFoundException
	 *             Could not find game class or hasher class
	 */
	public Configuration(String solvedJob) throws ClassNotFoundException {
		this(readProperties(solvedJob));
	}

	/**
	 * @param <T>
	 *            The state this game takes
	 * @return the Game this configuration plays
	 */
	@SuppressWarnings("unchecked")
	public <T extends State> Game<T> getCheckedGame() {
		return (Game<T>) g;
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
		String s = getPropertyOrNull(key);
		if (s == null)
			throw new Error("Property " + key + " is unset and has no default!");
		return s;
	}

	private String getPropertyOrNull(String key) {
		return props.getProperty(key);
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
		String s = getPropertyOrNull(key);
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
		String value = getPropertyOrNull(key);
		if (value != null)
			return Integer.parseInt(value);
		else
			return dfl;
	}

	/**
	 * Parses a property as a Float.
	 * 
	 * @param key
	 *            the name of the configuration property
	 * @param dfl
	 *            default value
	 * @return The value associated with the key, if defined and a float.
	 *         Otherwise, returns dfl.
	 */
	public float getFloat(String key, float dfl) {
		String value = getPropertyOrNull(key);
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
	 * @return The value associated with the key, if defined and a long.
	 *         Otherwise, returns dfl.
	 */
	public long getLong(String key, long dfl) {
		String lString = getPropertyOrNull(key);
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
		String iString = getPropertyOrNull(key);
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
		String result = getPropertyOrNull(key);
		if (result == null)
			return dfl;
		else
			return result;
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
			String line;
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
			throw new Error(e);
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

	/**
	 * @return A new identical configuration with clones of the game and hasher
	 */
	public Configuration cloneAll() {
		try {
			Configuration newConf = new Configuration(props);
			return newConf;
		} catch (ClassNotFoundException e) {
			throw new Error(e);
		}
	}

	/**
	 * @return The game for this configuration
	 */
	public Game<? extends State> getGame() {
		return g;
	}

	/**
	 * Skips over the configuration at the beginning of a database
	 * 
	 * @param in
	 *            Generally, a DataInputStream wrapping the FileInputStream for
	 *            the database file
	 * @return The number of bytes skipped
	 * @throws IOException
	 *             If an IOException occurs
	 */
	public static int skipConf(DataInput in) throws IOException {
		int confLength = in.readInt();
		Util.skipFully(in, confLength);
		return 4 + confLength;
	}

	/**
	 * Stores this configuration in the output stream
	 * 
	 * @param out
	 *            An DataOutput to store to
	 * @return The number of bytes written to the DataOutput
	 * @throws IOException
	 *             If an IOException occurs while writing
	 */
	public int store(DataOutput out) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		props.store(baos, "");
		byte[] confBytes = baos.toByteArray();
		out.writeInt(confBytes.length);
		out.write(confBytes);
		return 4 + confBytes.length;
	}

	/**
	 * @param in
	 *            An DataInput to load the Configuration from
	 * @return A configuration object loaded from the input stream
	 * @throws IOException
	 *             If an IOException occurs while reading
	 * @throws ClassNotFoundException
	 *             If the game cannot be found
	 */
	public static Configuration load(DataInput in) throws IOException,
			ClassNotFoundException {
		ByteArrayInputStream bais = new ByteArrayInputStream(loadBytes(in));
		Properties props = new Properties();
		props.load(bais);
		return new Configuration(props);
	}

	/**
	 * Loads the Configuration from a serialized string
	 * 
	 * @param s
	 *            The string
	 * @return The loaded configuration
	 * @throws ClassNotFoundException
	 *             If the configuration contains a class name which is not in
	 *             Gamesman Java
	 */
	public static Configuration deserialize(String s)
			throws ClassNotFoundException {
		Properties props = new Properties();
		try {
			props.load(new StringReader(s));
		} catch (IOException e) {
			throw new Error(e);
		}
		return new Configuration(props);
	}

	/**
	 * @return This configuration object serialized as a string
	 */
	public String serialize() {
		StringWriter writer = new StringWriter();
		try {
			props.store(writer, "");
		} catch (IOException e) {
			throw new Error(e);
		}
		return writer.toString();
	}

	private static byte[] loadBytes(DataInput in) throws IOException {
		int confLength = in.readInt();
		byte[] confBytes = new byte[confLength];
		in.readFully(confBytes);
		return confBytes;
	}

	/**
	 * Parses a property as a number of bytes. The string is a number possibly
	 * followed by a letter to indicate the scale (ie "5k" = 5*2^10, "13g" =
	 * 13*2^30, "284" = 284)
	 * 
	 * @param key
	 *            The property name
	 * @param defaultValue
	 *            The default to return if the key is not present
	 * @return The retrieved number of bytes as a long
	 */
	public long getNumBytes(String key, long defaultValue) {
		String value = getPropertyOrNull(key);
		if (value == null)
			return defaultValue;
		value = value.toUpperCase();
		int shamt;
		long coef;
		int last = value.length() - 1;
		switch (value.charAt(last)) {
		case 'K':
			shamt = 10;
			coef = Long.parseLong(value.substring(0, last));
			break;
		case 'M':
			shamt = 20;
			coef = Long.parseLong(value.substring(0, last));
			break;
		case 'G':
			shamt = 30;
			coef = Long.parseLong(value.substring(0, last));
			break;
		default:
			shamt = 0;
			coef = Long.parseLong(value);
		}
		return coef << shamt;
	}
}
