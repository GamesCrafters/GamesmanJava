package edu.berkeley.gamesman.verification;

public enum GameVerifierType {
	RANDOM, BACKTRACK;

	public static GameVerifierType fromString(String verifierName) {
		return valueOf(verifierName.toUpperCase());
	}
}
