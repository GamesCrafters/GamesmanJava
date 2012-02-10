package edu.berkeley.gamesman.util.qll;

class QLLNullPool<T> {
	private Node<T> firstNull = null;
	private int currentSize = 0;

	synchronized Node<T> getNode() {
		if (firstNull == null) {
			assert currentSize == 0;
			return new Node<T>();
		} else {
			assert currentSize > 0;
			currentSize--;
			Node<T> addIn = firstNull;
			firstNull = firstNull.next;
			return addIn;
		}
	}

	synchronized void giveBack(Node<T> n) {
		currentSize++;
		n.next = firstNull;
		firstNull = n;
	}

	synchronized void giveBack(Node<T> first, Node<T> last, int numNodes) {
		currentSize += numNodes;
		last.next = firstNull;
		firstNull = first;
	}
}