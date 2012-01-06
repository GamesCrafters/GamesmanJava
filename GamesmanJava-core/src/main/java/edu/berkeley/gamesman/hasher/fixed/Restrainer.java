package edu.berkeley.gamesman.hasher.fixed;

interface Restrainer {
	int getMult(int dig);

	int numPieces(int dig);
}
