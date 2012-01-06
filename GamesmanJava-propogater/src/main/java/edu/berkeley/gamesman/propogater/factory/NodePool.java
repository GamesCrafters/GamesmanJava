package edu.berkeley.gamesman.propogater.factory;

class NodePool<T> {
	private Node<T> first;

	Node<T> get() {
		if (first == null)
			return new Node<T>();
		else {
			Node<T> result = first;
			first = first.next;
			return result;
		}
	}

	void release(Node<T> node) {
		node.next = first;
		node.object = null;
		first = node;
	}
}
