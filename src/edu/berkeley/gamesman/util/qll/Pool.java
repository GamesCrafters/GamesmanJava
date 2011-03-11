package edu.berkeley.gamesman.util.qll;

public class Pool<T> {
	private final Factory<T> fact;
	private Node<T> firstNode;
	private Node<T> firstNullNode;

	public Pool(Factory<T> fact) {
		this.fact = fact;
	}

	public synchronized T get() {
		if (firstNode == null)
			return fact.newObject();
		else {
			Node<T> changeNode = firstNode;
			firstNode = changeNode.next;
			changeNode.next = firstNullNode;
			firstNullNode = changeNode;
			fact.reset(changeNode.object);
			return changeNode.object;
		}
	}

	public synchronized void release(T el) {
		Node<T> changeNode;
		if (firstNullNode == null)
			changeNode = new Node<T>();
		else
			changeNode = firstNullNode;
		firstNullNode = changeNode.next;
		changeNode.next = firstNode;
		firstNode = changeNode;
		changeNode.object = el;
	}
}
