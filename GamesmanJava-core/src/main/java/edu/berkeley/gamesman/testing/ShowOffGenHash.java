package edu.berkeley.gamesman.testing;

import java.util.Scanner;

import edu.berkeley.gamesman.hasher.genhasher.GenHasher;
import edu.berkeley.gamesman.hasher.genhasher.GenState;

@SuppressWarnings("javadoc")
public class ShowOffGenHash {
	public static <S extends GenState> void main(String[] args)
			throws ClassNotFoundException, InstantiationException,
			IllegalAccessException, InterruptedException {
		String className = args[0];
		@SuppressWarnings("unchecked")
		Class<? extends GenHasher<S>> toShow = (Class<? extends GenHasher<S>>) Class
				.forName("edu.berkeley.gamesman.hasher." + className)
				.asSubclass(GenHasher.class);
		GenHasher<S> instance = toShow.newInstance();
		S state = instance.newState();
		int[] seq = new int[instance.numElements];
		Scanner scan = new Scanner(System.in);
		for (int i = 0; i < instance.numElements; i++) {
			seq[i] = scan.nextInt();
		}
		instance.set(state, seq);
		System.out.println(instance.hash(state) + "\t" + state);
		Thread.sleep(5000);
		while (instance.step(state) != -1) {
			System.out.println(instance.hash(state) + "\t" + state);
			Thread.sleep(1000);
		}
	}
}
