package edu.berkeley.gamesman.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * A mutable pair is a simple data type that stores two (not final) distinct pieces of information
 * 
 * @author Steven Schlansker
 *
 * @param <A> The type of the first part
 * @param <B> The type of the second part
 */
public class MutablePair<A,B> implements Serializable {

	private static final long serialVersionUID = 3664218983748432874L;
	/**
	 * The first datum
	 */
	public A car;
	/**
	 * The second datum
	 */
	public B cdr;
	
	/**
	 * Initialize a new Pair
	 * @param ar First datum
	 * @param dr Second datum
	 */
	public MutablePair(A ar, B dr){
		car = ar;
		cdr = dr;
	}
	
	@Override
	public String toString(){
		return "("+car+"."+cdr+")";
	}
	
	/**
	 * @param <A> The type of the first collection
	 * @param <B> The type of the second collection
	 * @param a The first collection
	 * @param b The second collection
	 * @return A collection of pairs containing the respective elements of each collection.
	 */
	public static <A,B> Collection<Pair<A,B>> zip(Collection<A> a, Collection<B> b){
		ArrayList<Pair<A,B>> al = new ArrayList<Pair<A,B>>();
		Iterator<A> ai = a.iterator();
		Iterator<B> bi = b.iterator();
		while(ai.hasNext() && bi.hasNext()){
			al.add(new Pair<A,B>(ai.next(),bi.next()));
		}
		return al;
	}
	
}