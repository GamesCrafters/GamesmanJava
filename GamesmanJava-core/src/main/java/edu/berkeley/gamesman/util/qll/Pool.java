package edu.berkeley.gamesman.util.qll;

public class Pool<T> {
	private int created = 0;
	private int released = 0;
	private final Factory<T> fact;
	private Node<T> firstNode;
	private Node<T> firstNullNode;

	public Pool(Factory<T> fact) {
		this.fact = fact;
	}

	public synchronized T get() {
		if (firstNode == null) {
			created++;
			return fact.newObject();
		} else {
			Node<T> changeNode = firstNode;
			firstNode = changeNode.next;
			changeNode.next = firstNullNode;
			firstNullNode = changeNode;
			fact.reset(changeNode.object);
			return changeNode.object;
		}
	}

	public synchronized void release(T el) {
		if (el == null)
			throw new NullPointerException("Cannot release null element");
		Node<T> changeNode;
		if (firstNullNode == null) {
			changeNode = new Node<T>();
			released++;
			if (released > created)
				throw new RuntimeException("WTF?");
		} else
			changeNode = firstNullNode;
		firstNullNode = changeNode.next;
		changeNode.next = firstNode;
		firstNode = changeNode;
		changeNode.object = el;
	}
}
