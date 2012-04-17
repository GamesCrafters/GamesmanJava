package edu.berkeley.gamesman.parallel;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import edu.berkeley.gamesman.game.type.GameRecord;
import edu.berkeley.gamesman.game.type.GameValue;
import edu.berkeley.gamesman.util.qll.QuickLinkedList;

public class DualRecord implements FlipRecord {

	public DualRecord() {
	}

	private final GameRecord myRecord = new GameRecord();
	private final GameRecord misereRecord = new GameRecord();

	@Override
	public void write(DataOutput out) throws IOException {
		writeRec(out, myRecord, false);
		writeRec(out, misereRecord, true);
	}

	private void writeRec(DataOutput out, GameRecord writeRec, boolean isMisere)
			throws IOException {
		GameValue value = writeRec.getValue();
		switch (value) {
		case TIE:
			out.writeByte(-1);
			break;
		case DRAW:
			out.writeByte(-2);
			break;
		default:
			int remoteness = writeRec.getRemoteness();
			assert value == getWLValue(remoteness, isMisere);
			out.writeByte(remoteness);
			break;
		}
	}

	private GameValue getWLValue(int remoteness, boolean isMisere) {
		return (isMisere ^ (remoteness & 1) != 0) ? GameValue.WIN
				: GameValue.LOSE;
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		readRecord(in, myRecord, false);
		readRecord(in, misereRecord, true);
	}

	private void readRecord(DataInput in, GameRecord writeRecord,
			boolean isMisere) throws IOException {
		int b = in.readByte();
		switch (b) {
		case -1:
			writeRecord.set(GameValue.TIE, 0);
			break;
		case -2:
			writeRecord.set(GameValue.DRAW);
			break;
		default:
			assert b >= 0;
			writeRecord.set(getWLValue(b, isMisere), b);
			break;
		}
	}

	@Override
	public void set(GameValue value) {
		myRecord.set(value);
		misereRecord.set(value.opposite());
	}

	@Override
	public void set(GameValue value, int remoteness) {
		assert remoteness == 0;
		myRecord.set(value, remoteness);
		misereRecord.set(value.opposite(), remoteness);
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof DualRecord))
			return false;
		DualRecord d = (DualRecord) other;
		return equalsIgnoreTieRem(myRecord, d.myRecord)
				&& equalsIgnoreTieRem(misereRecord, d.misereRecord);
	}

	private boolean equalsIgnoreTieRem(GameRecord rec1, GameRecord rec2) {
		return (rec1.getValue() == GameValue.TIE && rec2.getValue() == GameValue.TIE)
				|| rec1.equals(rec2);
	}

	@Override
	public String toString() {
		return recordString(myRecord) + ", " + recordString(misereRecord);
	}

	private String recordString(GameRecord rec) {
		if (rec.getValue() == GameValue.TIE)
			return "TIE";
		else
			return rec.toString();
	}

	@Override
	public void previousPosition(FlipRecord gr) {
		DualRecord d = (DualRecord) gr;
		myRecord.previousPosition(d.myRecord);
		misereRecord.previousPosition(d.misereRecord);
	}

	@Override
	public int size() {
		return 2;
	}

	public static boolean combineValues(QuickLinkedList<DualRecord> grList,
			DualRecord gr) {
		QuickLinkedList<DualRecord>.QLLIterator iter = grList.iterator();
		try {
			GameRecord bestMine = null;
			GameRecord bestMis = null;
			while (iter.hasNext()) {
				DualRecord next = iter.next();
				if (bestMine == null || next.myRecord.compareTo(bestMine) > 0) {
					bestMine = next.myRecord;
				}
				if (bestMis == null || next.misereRecord.compareTo(bestMis) > 0) {
					bestMis = next.misereRecord;
				}
			}
			boolean changed = false;
			if (bestMine != null && !gr.myRecord.equals(bestMine)) {
				gr.myRecord.set(bestMine);
				changed = true;
			}
			if (bestMis != null && !gr.misereRecord.equals(bestMis)) {
				gr.misereRecord.set(bestMis);
				changed = true;
			}
			return changed;
		} finally {
			grList.release(iter);
		}
	}

	public static GameRecord getRecord(DualRecord rec, int tieRemoteness,
			boolean misere) {
		GameRecord fetchedRec = misere ? rec.misereRecord : rec.myRecord;
		GameValue gv = fetchedRec.getValue();
		if (gv == GameValue.TIE)
			return new GameRecord(GameValue.TIE, tieRemoteness);
		else {
			assert gv == GameValue.LOSE || gv == GameValue.WIN;
			return new GameRecord(gv, fetchedRec.getRemoteness());
		}
	}
}
