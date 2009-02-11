package edu.berkeley.gamesman.util;

import java.io.Serializable;

/**
 * A pair is a simple data type that stores two distinct pieces of information
 * 
 * @author Steven Schlansker
 *
 * @param <A> The type of the first part
 * @param <B> The type of the second part
 */
public class Pair<A,B> implements Serializable {

	private static final long serialVersionUID = 3664218983748432874L;
	/**
	 * The first datum
	 */
	final public A car;
	/**
	 * The second datum
	 */
	final public B cdr;
	
	/**
	 * Initialize a new Pair
	 * @param ar First datum
	 * @param dr Second datum
	 */
	public Pair(A ar, B dr){
		car = ar;
		cdr = dr;
	}
	
	@Override
	public String toString(){
		return "("+car+"."+cdr+")";
	}
	
}
