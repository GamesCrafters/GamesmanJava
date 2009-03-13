package edu.berkeley.gamesman.hasher;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;

public class PermutationHash {
	//Jython testing code
//from edu.berkeley.gamesman.hasher import PermutationHash
//from org.python.core import PyLong
//p=PermutationHash(3)
//[p.unhash(PyLong(i)) for i in range(p.maxHash().intValue()+1)]
	private final int permutationLength;
	private final BigInteger[] FACTORIAL;
	public PermutationHash(int permutationLength) {
		this.permutationLength = permutationLength;
		
		FACTORIAL = new BigInteger[permutationLength + 1];
		FACTORIAL[0] = BigInteger.ONE;
		for(int i = 1; i < FACTORIAL.length; i++)
			FACTORIAL[i] = BigInteger.valueOf(i).multiply(FACTORIAL[i-1]);
//		for(int i = 1; i < FACTORIAL.length; i++)
//			FACTORIAL[i] = FACTORIAL[i].divide(BigInteger.valueOf(2));
	}
	
	public BigInteger hash(Integer[] pieces) {
		return hash(new ArrayList<Integer>(Arrays.asList(pieces)));
	}
	
	public BigInteger hash(ArrayList<Integer> pieces) {
		BigInteger hash = BigInteger.ZERO;
		for(int i = 0; i < permutationLength - 1; i++) {
			int pos = pieces.indexOf(i);
			pieces.remove(pos);
			hash = hash.add(FACTORIAL[pieces.size()].multiply(BigInteger.valueOf(pos)));
		}
		return hash;
	}
	
	public ArrayList<Integer> unhash(BigInteger hash) {
		ArrayList<Integer> pieces = new ArrayList<Integer>(permutationLength);
		for(int i = 0; i < permutationLength; i++) {
			int location = hash.divide(FACTORIAL[pieces.size()]).mod(BigInteger.valueOf(i+1)).intValue();
//			System.out.println("\t" + location + " " + ((permutationLength - 1)-i));
			pieces.add(location, (permutationLength - 1)-i);
		}
		return pieces;
	}
	
	public BigInteger maxHash() {
		return FACTORIAL[permutationLength].subtract(BigInteger.ONE);
	}
}
