package edu.berkeley.gamesman.propogater.tasks;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;

import edu.berkeley.gamesman.propogater.common.ConfParser;
import edu.berkeley.gamesman.propogater.tree.TreeNode;
import edu.berkeley.gamesman.propogater.writable.Entry;
import edu.berkeley.gamesman.propogater.writable.IntEntry;
import edu.berkeley.gamesman.propogater.writable.list.WritableList;

public class TreeCreationReducer<K extends WritableComparable<K>, V extends Writable, PI extends Writable, UM extends Writable, CI extends Writable, DM extends Writable>
		extends TreeReducer<K, V, PI, UM, CI, DM> {
	private boolean hasNew = false;
	private int creationDivision;

	@Override
	protected void setup(Context context) throws IOException,
			InterruptedException {
		super.setup(context);
		Configuration conf = context.getConfiguration();
		creationDivision = ConfParser.getDivision(conf);
	}

	@Override
	protected void combine(K key, TreeNode<K, V, PI, UM, CI, DM> value,
			int division) {
		WritableList<IntEntry<Entry<K, DM>>> downList = value.getDownList();
		WritableList<IntEntry<Entry<K, PI>>> parentList = value.getParentList();
		if (tree.copyDM()) {
			parentList.steal((WritableList) downList);
		} else {
			for (int i = 0; i < downList.length(); i++) {
				IntEntry<Entry<K, DM>> mess = downList.get(i);
				IntEntry<Entry<K, PI>> parent = parentList.add();
				parent.setKey(mess.getKey());
				Entry<K, PI> parentVal = parent.getValue();
				Entry<K, DM> messVal = mess.getValue();
				tree.receiveDown(key, value.getValue(), messVal.getKey(),
						messVal.getValue(), parentVal.getValue());
				parentVal.swapKeys(messVal);
			}
			downList.clear();
		}
		if (!value.hasValue() && division == creationDivision)
			hasNew = true;
	}

	@Override
	protected void cleanup(Context context) {
		super.cleanup(context);
		if (hasNew) {
			context.getCounter("needs_creation", "t" + creationDivision)
					.increment(1);
		}
	}
}
