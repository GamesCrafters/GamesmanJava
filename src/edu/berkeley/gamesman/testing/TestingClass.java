package edu.berkeley.gamesman.testing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.ItergameState;
import edu.berkeley.gamesman.game.Connect4;

public class TestingClass {
	static Random r = new Random();

	public static void main(String[] args) throws IOException {
		InputStream pis;
		ByteArrayOutputStream pos;
		GZIPOutputStream gzo;
		pos = new ByteArrayOutputStream();
		gzo = new GZIPOutputStream(pos);
		byte[] bArray = new byte[1024];
		byte[] arr2 = new byte[1024];
		r.nextBytes(bArray);
		gzo.write(bArray);
		gzo.close();
		byte[] arr = pos.toByteArray();
		pis = new GZIPInputStream(new ByteArrayInputStream(arr));
		for (int i = 0; i < 1024; i++) {
			arr2[i] = (byte) pis.read();
		}
		pis.close();
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
