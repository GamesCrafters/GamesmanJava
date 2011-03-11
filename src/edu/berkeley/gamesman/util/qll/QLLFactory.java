package edu.berkeley.gamesman.util.qll;

public class QLLFactory<T> {
	private QLLNullPool<T> pool;

	public QLLFactory() {
		this(new QLLNullPool<T>());
	}

	QLLFactory(QLLNullPool<T> pool) {
		this.pool = pool;
	}

	public QuickLinkedList<T> getList() {
		return new QuickLinkedList<T>(pool);
	}
}
