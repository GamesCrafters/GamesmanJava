package edu.berkeley.gamesman.propogater.tree;

import org.apache.hadoop.io.Writable;

import edu.berkeley.gamesman.propogater.writable.Entry;
import edu.berkeley.gamesman.propogater.writable.IntEntry;
import edu.berkeley.gamesman.propogater.writable.list.WritList;
import edu.berkeley.gamesman.propogater.writable.list.WritableList;

class ParList<K extends Writable, PI extends Writable> implements
		WritList<Entry<K, PI>> {
	private WritableList<IntEntry<Entry<K, PI>>> list;

	void setList(WritableList<IntEntry<Entry<K, PI>>> list) {
		this.list = list;
	}

	@Override
	public int length() {
		return list.length();
	}

	@Override
	public Entry<K, PI> get(int i) {
		return list.get(i).getValue();
	}

	@Override
	public String toString() {
		return list.toString();
	}
}
