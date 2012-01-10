package edu.berkeley.gamesman.hadoop.ranges;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import edu.berkeley.gamesman.hasher.cachehasher.CacheMove;
import edu.berkeley.gamesman.propogater.writable.WritableSettable;
import edu.berkeley.gamesman.propogater.writable.list.WritableList;

public class MoveWritable implements WritableSettable<MoveWritable> {
	private static class TripletWritable implements
			WritableSettable<TripletWritable> {
		int place, from, to;

		@Override
		public void readFields(DataInput in) throws IOException {
			place = in.readInt();
			from = in.readInt();
			to = in.readInt();
		}

		@Override
		public void write(DataOutput out) throws IOException {
			out.writeInt(place);
			out.writeInt(from);
			out.writeInt(to);
		}

		@Override
		public void set(TripletWritable t) {
			place = t.place;
			from = t.from;
			to = t.to;
		}
	}

	private final WritableList<TripletWritable> changeList = new WritableList<TripletWritable>(
			TripletWritable.class, null);

	@Override
	public void readFields(DataInput in) throws IOException {
		changeList.readFields(in);
	}

	@Override
	public void write(DataOutput out) throws IOException {
		changeList.write(out);
	}

	@Override
	public void set(MoveWritable t) {
		changeList.set(t.changeList);
	}

	public void set(CacheMove move) {
		changeList.clear();
		for (int i = 0; i < move.numChanges; i++) {
			TripletWritable tw = changeList.add();
			tw.place = move.getChangePlace(i);
			tw.from = move.getChangeFrom(i);
			tw.to = move.getChangeTo(i);
		}
	}

	public int numChanges() {
		return changeList.length();
	}

	public int getChangePlace(int i) {
		return changeList.get(i).place;
	}

	public int getChangeFrom(int i) {
		return changeList.get(i).from;
	}

	public int getChangeTo(int i) {
		return changeList.get(i).to;
	}

}
