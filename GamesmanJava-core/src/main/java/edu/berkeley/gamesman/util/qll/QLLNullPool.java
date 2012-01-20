package edu.berkeley.gamesman.util.qll;

class QLLNullPool<T> {
	private Node<T> firstNull = null;

	synchronized Node<T> getNode() {
		if (firstNull == null) {
			return new Node<T>();
		} else {
			Node<T> addIn = firstNull;
			firstNull = firstNull.next;
			return addIn;
		}
	}

	synchronized void giveBack(Node<T> n) {
		n.next = firstNull;
		firstNull = n;
	}

	synchronized void giveBack(Node<T> first, Node<T> last) {
		last.next = firstNull;
		firstNull = first;
	}
}