package edu.berkeley.gamesman.game.tree;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Writable;

import edu.berkeley.gamesman.game.type.GameRecord;
import edu.berkeley.gamesman.game.type.GameValue;
import edu.berkeley.gamesman.propogater.common.Adder;
import edu.berkeley.gamesman.propogater.common.Entry3;

class GameAdder<K extends Writable> implements Adder<K> {
	private Adder<Entry3<K, GameRecord, NullWritable>> list;

	public void setList(
			Adder<Entry3<K, GameRecord, NullWritable>> childrenToFill) {
		this.list = childrenToFill;
	}

	@Override
	public K add() {
		Entry3<K, GameRecord, NullWritable> entry = list.add();
		entry.getT2().set(GameValue.DRAW);
		return entry.getT1();
	}

	@Override
	public String toString() {
		return list.toString();
	}
}
