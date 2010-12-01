package edu.berkeley.gamesman.master;


import org.apache.hadoop.mapreduce.InputSplit;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: Dan
 * Date: Nov 30, 2010
 * Time: 10:14:16 AM
 * To change this template use File | Settings | File Templates.
 */
public class Split extends InputSplit {
	private final static String[] hosts = new String[0];
	Range r;
	public Split() {
	}
	public Split(Range r) {
		this.r = r;
	}
        @Override
	public long getLength() throws IOException {
		return r.getLength();
	}
         @Override
	public String[] getLocations() throws IOException {
		return hosts;
	}
}
