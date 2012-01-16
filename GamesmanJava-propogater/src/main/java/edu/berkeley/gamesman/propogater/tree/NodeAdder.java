package edu.berkeley.gamesman.propogater.tree;

import java.util.Arrays;

import org.apache.hadoop.io.Writable;

import edu.berkeley.gamesman.propogater.common.Adder;
import edu.berkeley.gamesman.propogater.common.Entry3;
import edu.berkeley.gamesman.propogater.common.SetEntry3;
import edu.berkeley.gamesman.propogater.writable.Entry;
import edu.berkeley.gamesman.propogater.writable.list.WritableList;

class NodeAdder<K extends Writable, CI extends Writable, DM extends Writable>
		implements Adder<Entry3<K, CI, DM>> {
	WritableList<Entry<K, CI>> entryList;
	WritableList<DM> messageList;
	private final SetEntry3<K, CI, DM> entry = new SetEntry3<K, CI, DM>();

	void setList(WritableList<Entry<K, CI>> entryList,
			WritableList<DM> messageList) {
		this.entryList = entryList;
		this.messageList = messageList;
	}

	@Override
	public SetEntry3<K, CI, DM> add() {
		Entry<K, CI> childEntry = entryList.add();
		entry.setEntries(childEntry.getKey(), childEntry.getValue(),
				messageList.add());
		return entry;
	}

	@Override
	public String toString() {
		SetEntry3<K, CI, DM>[] arr = new SetEntry3[entryList.length()];
		for (int i = 0; i < entryList.length(); i++) {
			arr[i] = new SetEntry3<K, CI, DM>(entryList.get(i),
					messageList.get(i));
		}
		return Arrays.toString(arr);
	}
}
