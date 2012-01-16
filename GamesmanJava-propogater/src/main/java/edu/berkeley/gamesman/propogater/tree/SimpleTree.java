package edu.berkeley.gamesman.propogater.tree;

import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;

public abstract class SimpleTree<K extends WritableComparable<K>, V extends Writable, PI extends Writable, CI extends Writable>
		extends Tree<K, V, PI, CI, CI, PI> {
	@Override
	public final void receiveDown(K key, V currentValue, K parentKey,
			PI parentMessage, PI toFill) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final void receiveUp(K key, V currentValue, K childKey,
			CI childMessage, CI currentChildInfo) {
		throw new UnsupportedOperationException();
	}

	public final boolean copyUM() {
		return true;
	}

	public final boolean copyDM() {
		return true;
	}

	@Override
	public final Class<CI> getUmClass() {
		return getCiClass();
	}

	@Override
	public final Class<PI> getDmClass() {
		return getPiClass();
	}
}
