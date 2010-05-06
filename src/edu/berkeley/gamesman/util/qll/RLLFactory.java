package edu.berkeley.gamesman.util.qll;

public class RLLFactory<T> extends QLLFactory<T> {
	private final RLLNullPool<T> pool;

	public RLLFactory(Factory<T> factory) {
		this(new RLLNullPool<T>(factory));
	}

	RLLFactory(RLLNullPool<T> pool) {
		super(pool);
		this.pool = pool;
	}

	@Override
	public RecycleLinkedList<T> getList() {
		return new RecycleLinkedList<T>(pool);
	}
}
