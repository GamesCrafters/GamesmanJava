package edu.berkeley.gamesman.testing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.game.Connect4;
import edu.berkeley.gamesman.game.util.ItergameState;
import edu.berkeley.gamesman.util.Util;

public class TestingClass {
	static Random r = new Random();

	public static void main(String[] args) {
		if (args.length != 1) {
			Util.fatalError("Please specify a jobfile as the only argument");
		}
		Configuration conf;
		Database fd;

		try {
			File dataFile = new File(args[0]);
			FileInputStream fis = new FileInputStream(dataFile);
			int confLength = 0;
			for (int i = 28; i >= 0; i -= 8) {
				confLength <<= 8;
				confLength |= fis.read();
			}
			byte[] confBytes = new byte[confLength];
			fis.read(confBytes);
			fis.close();
			conf = Configuration.load(confBytes);
			conf.setProperty("gamesman.db.uri", args[0]);
			fd = conf.openDatabase(false);
		} catch (ClassNotFoundException e) {
			Util.fatalError("failed to load class", e);
			return;
		} catch (IOException e) {
			Util.fatalError("IO Error", e);
			return;
		}
		System.out.println(fd.getRecord(0));
	}

	public static void newerMain(String[] args) throws IOException {
		ByteArrayInputStream bais;
		GZIPInputStream pis = null;
		ByteArrayOutputStream pos = null;
		GZIPOutputStream gzo;
		pos = new ByteArrayOutputStream();
		byte[] bArray = new byte[1024];
		byte[] arr2 = new byte[1024];
		int[] positions = new int[4];
		r.nextBytes(bArray);
		for (int i = 0; i < 4; i++) {
			gzo = new GZIPOutputStream(pos, 1024);
			gzo.write(bArray, i << 8, 256);
			gzo.finish();
			positions[i] = pos.size();
		}
		pos.close();
		byte[] arr = pos.toByteArray();
		for (int i = 0; i < 4; i++) {
			int count = i << 8, tot = (i + 1) << 8;
			bais = new ByteArrayInputStream(arr,
					(i == 0 ? 0 : positions[i - 1]), positions[i]);
			pis = new GZIPInputStream(bais, 1024);
			while (count < tot)
				count += pis.read(arr2, count, tot - count);
			pis.close();
		}
		if (Arrays.equals(bArray, arr2))
			System.out.println("Worked");
	}

	public static void oldMain(String[] args) throws ClassNotFoundException {
		Configuration conf = new Configuration(Configuration
				.readProperties("jobs/Connect4_54.job"));
		Connect4 game = (Connect4) conf.getGame();
		ItergameState[] states = new ItergameState[5];
		for (int i = 0; i < 5; i++)
			states[i] = new ItergameState();
		game.setState(game.hashToState(4318920));
		game.lastMoves(states);
		System.out.println(Arrays.toString(states));
		game.setState(game.hashToState(4391851));
		game.lastMoves(states);
		System.out.println(Arrays.toString(states));
		game.setState(game.hashToState(4343263));
		game.lastMoves(states);
		System.out.println(Arrays.toString(states));
	}
}
