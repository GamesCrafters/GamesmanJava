package edu.berkeley.gamesman.hasher;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import edu.berkeley.gamesman.util.Util;

public class PermutationHash {
	//Jython testing code
//from edu.berkeley.gamesman.hasher import PermutationHash
//from org.python.core import PyLong
//p=PermutationHash(3)
//[p.unhash(PyLong(i)) for i in range(p.maxHash().intValue()+1)]
	private final int permutationLength;
	private final BigInteger[] FACTORIAL;
	private final boolean evenPermutation;
	public PermutationHash(int permutationLength, boolean evenPermutation) {
		this.permutationLength = permutationLength;
		this.evenPermutation = evenPermutation;
		
		FACTORIAL = new BigInteger[permutationLength + 1];
		FACTORIAL[0] = BigInteger.ONE;
		for(int i = 1; i < FACTORIAL.length; i++)
			FACTORIAL[i] = BigInteger.valueOf(i).multiply(FACTORIAL[i-1]);
		if(evenPermutation)
			for(int i = 0; i < FACTORIAL.length; i++)
				FACTORIAL[i] = FACTORIAL[i].divide(BigInteger.valueOf(2));
	}
	
	public boolean isEven(Integer[] pieces) {
		Integer[] clone = pieces.clone();
		int count = 0;
		for(int i=0; i<clone.length; i++) {
			if(clone[i] != i) {
				count++;
				int j;
				for(j=i+1; j<clone.length; j++)
					if(clone[j] == i)
						break;
				clone[j] = clone[i]; //swap 'em
				clone[i] = i;
			}
		}
		//basically, we're counting the number of swaps to
		//get back to the identity permutation, so if we had
		//an even number of swaps, we have an even permutation
		return count % 2 == 0;
	}
	
	public static void main(String[] args) {
		PermutationHash ph = new PermutationHash(4, true);
		for(BigInteger h : Util.bigIntIterator(ph.maxHash())) {
			Integer[] unhash1 = Util.toArray(ph.unhash(h));
//			System.out.print(Arrays.toString(unhash1));
//			System.out.println(" " + ph.hash(unhash1) + " " + ph.isEven(unhash1));
			assert ph.isEven(unhash1);
			assert ph.hash(unhash1).equals(h);
		}
	}
	
	private ArrayList<Integer> getIdentityPermutation() {
		ArrayList<Integer> ident = new ArrayList<Integer>();
		for(int i = 0; i < permutationLength; i++)
			ident.add(i);
		return ident;
	}
	
	public BigInteger hash(ArrayList<Integer> pieces) {
		return hash(Util.toArray(pieces));
	}
	
	public BigInteger hash(Integer[] pieces) {
		assert pieces.length == permutationLength;
		ArrayList<Integer> ident = getIdentityPermutation();
		BigInteger hash = BigInteger.ZERO;
		int last = evenPermutation ? permutationLength - 2 : permutationLength - 1;
		for(int i = 0; i < last; i++) {
			int pos = ident.indexOf(pieces[i]);
			ident.remove(pos);
			hash = hash.add(FACTORIAL[ident.size()].multiply(BigInteger.valueOf(pos)));
		}
		return hash;
	}
	
	public ArrayList<Integer> unhash(BigInteger hash) {
		ArrayList<Integer> ident = getIdentityPermutation();
		ArrayList<Integer> pieces = new ArrayList<Integer>();
		int last = evenPermutation ? permutationLength - 2 : permutationLength;
		for(int i = 0; i < last; i++) {
			BigInteger fact = FACTORIAL[ident.size() - 1];
			BigInteger[] location_rem = hash.divideAndRemainder(fact);
			hash = location_rem[1];
			pieces.add(ident.remove(location_rem[0].intValue()));
		}
		if(evenPermutation) {
			assert ident.size() == 2;
			pieces.add(ident.remove(0));
			pieces.add(ident.remove(0));
			//is there a better way of doing this?
			//it could be merged with the above loop,
			//but that wouldn't this it any faster
			if(!isEven(Util.toArray(pieces)))
				Collections.swap(pieces, pieces.size()-1, pieces.size()-2);
			assert isEven(Util.toArray(pieces));
		}
		assert ident.size() == 0;
		return pieces;
	}
	
	public BigInteger maxHash() {
		return FACTORIAL[permutationLength].subtract(BigInteger.ONE);
	}
}
