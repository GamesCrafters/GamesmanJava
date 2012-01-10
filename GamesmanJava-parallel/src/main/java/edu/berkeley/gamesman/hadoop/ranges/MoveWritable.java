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

	private final WritableList<TripletWritable> moveList = new WritableList<TripletWritable>(
			TripletWritable.class, null);

	@Override
	public void readFields(DataInput in) throws IOException {
		moveList.readFields(in);
	}

	@Override
	public void write(DataOutput out) throws IOException {
		moveList.write(out);
	}

	@Override
	public void set(MoveWritable t) {
		moveList.set(t.moveList);
	}

	public void set(CacheMove move) {
		moveList.clear();
		for (int i = 0; i < move.numChanges; i++) {
			TripletWritable tw = moveList.add();
			tw.place = move.getChangePlace(i);
			tw.from = move.getChangeFrom(i);
			tw.to = move.getChangeTo(i);
		}
	}

}
