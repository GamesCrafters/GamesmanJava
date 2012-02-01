package edu.berkeley.gamesman.game.tree;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.WritableComparable;

import edu.berkeley.gamesman.game.type.GameRecord;
import edu.berkeley.gamesman.game.type.GameValue;
import edu.berkeley.gamesman.propogater.common.Adder;
import edu.berkeley.gamesman.propogater.common.Entry3;
import edu.berkeley.gamesman.propogater.tree.SimpleTree;
import edu.berkeley.gamesman.propogater.writable.Entry;
import edu.berkeley.gamesman.propogater.writable.list.WritList;
import edu.berkeley.gamesman.propogater.writable.list.WritableList;

public abstract class GameTree<STATE extends WritableComparable<STATE>> extends
		SimpleTree<STATE, GameRecord, NullWritable, GameRecord> {

	public abstract void getChildren(STATE position, Adder<STATE> toFill);

	public abstract GameValue getPrimitiveValue(STATE position);

	@Override
	public final Class<GameRecord> getValClass() {
		return GameRecord.class;
	}

	private final GameAdder<STATE> childAdder = new GameAdder<STATE>();

	@Override
	public void firstVisit(STATE key, GameRecord valueToFill,
			WritList<Entry<STATE, NullWritable>> parents,
			Adder<Entry3<STATE, GameRecord, NullWritable>> childrenToFill) {
		GameValue primitiveValue = getPrimitiveValue(key);
		if (primitiveValue == null) {
			valueToFill.set(GameValue.DRAW);
			childAdder.setList(childrenToFill);
			getChildren(key, childAdder);
		} else
			valueToFill.set(primitiveValue, 0);
	}

	@Override
	public void combineDown(STATE key, GameRecord value,
			WritList<Entry<STATE, NullWritable>> parents, int firstNewParent,
			WritableList<Entry<STATE, GameRecord>> children) {
	}

	@Override
	public boolean combineUp(STATE key, GameRecord value,
			WritList<Entry<STATE, NullWritable>> parents,
			WritableList<Entry<STATE, GameRecord>> children) {
		GameRecord bestRecord = null;
		for (int i = 0; i < children.length(); i++) {
			GameRecord rec = children.get(i).getValue();
			if (bestRecord == null || rec.compareTo(bestRecord) > 0)
				bestRecord = rec;
		}
		if (bestRecord != null && !value.equals(bestRecord)) {
			value.set(bestRecord);
			return true;
		} else
			return false;
	}

	@Override
	public void sendUp(STATE key, GameRecord value, STATE parentKey,
			NullWritable parentInfo, GameRecord toFill) {
		toFill.previousPosition(value);
	}

	@Override
	public Class<NullWritable> getPiClass() {
		return NullWritable.class;
	}

	@Override
	public Class<GameRecord> getCiClass() {
		return GameRecord.class;
	}
}
