package edu.berkeley.gamesman.verification;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class ProgressBar {
	private static final int TOTAL_TOKENS_TO_PRINT = 40;
	private static final NumberFormat formatter = new DecimalFormat("#0.00");
	private static final int NUM_CHARS_IN_OUTPUT = 68;
	private double nTotalElements;
	private double nCurrentElements;
	private long startTime = -1;

	public ProgressBar(int nTotalElements) {
		this.nTotalElements = nTotalElements;
	}

	public void updateNumElements(int nCurrentElements) {
		if (startTime == -1)
			startTime = System.currentTimeMillis();
		this.nCurrentElements = nCurrentElements;
	}

	public void printStatus() {
		System.out.print('\r');
		double complete;
		if (nCurrentElements > nTotalElements) {
			complete = .99;
		} else {
			complete = ((double) nCurrentElements) / nTotalElements;
		}
		System.out.print('[');
		int tokensToPrint = (int) (complete * TOTAL_TOKENS_TO_PRINT);
		for (int i = 0; i < tokensToPrint; i++) {
			System.out.print('=');
		}
		if (complete < 1)
			System.out.print('>');
		else
			System.out.print('=');
		for (int i = 0; i < TOTAL_TOKENS_TO_PRINT - tokensToPrint; i++) {
			System.out.print(' ');
		}
		System.out.print("] " + (int) (complete * 100) + "% complete ");
		if (startTime != -1) {
			double secondsElapsed = (System.currentTimeMillis() - startTime) / 1000.0;
			System.out.print(formatter.format(secondsElapsed)
					+ " seconds elapsed");
		}
	}

	public void println(String s) {
		System.out.print(s);
		for (int i = s.length(); i < NUM_CHARS_IN_OUTPUT; i++) {
			System.out.print(' ');
		}
		System.out.println();
		printStatus();
	}

	public void finish() {
		nCurrentElements = nTotalElements;
		printStatus();
		System.out.print('\n');
	}
}
