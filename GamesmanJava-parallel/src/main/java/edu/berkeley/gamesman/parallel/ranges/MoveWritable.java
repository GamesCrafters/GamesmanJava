package edu.berkeley.gamesman.parallel.ranges;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

import org.apache.hadoop.io.Writable;

import edu.berkeley.gamesman.hasher.genhasher.Move;
import edu.berkeley.gamesman.propogater.writable.list.WritableList;

public class MoveWritable implements Writable, Move {
	private static class TripletWritable implements Writable {
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

		// public void set(TripletWritable t) {
		// place = t.place;
		// from = t.from;
		// to = t.to;
		// }

		@Override
		public String toString() {
			return Arrays.toString(new int[] { place, from, to });
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

//	public void set(MoveWritable t) {
//		changeList.set(t.changeList);
//	}
//
//	public void set(Move move) {
//		changeList.clear();
//		for (int i = 0; i < move.numChanges(); i++) {
//			TripletWritable tw = changeList.add();
//			tw.place = move.getChangePlace(i);
//			tw.from = move.getChangeFrom(i);
//			tw.to = move.getChangeTo(i);
//		}
//	}

	@Override
	public int numChanges() {
		return changeList.length();
	}

	@Override
	public int getChangePlace(int i) {
		return changeList.get(i).place;
	}

	@Override
	public int getChangeFrom(int i) {
		return changeList.get(i).from;
	}

	@Override
	public int getChangeTo(int i) {
		return changeList.get(i).to;
	}

	@Override
	public String toString() {
		return changeList.toString();
	}
}
