package edu.berkeley.gamesman.util.qll;

public class RecycleLinkedList<T> extends QuickLinkedList<T> {
	private RLLIterator myIterator;
	private final RLLNullPool<T> pool;

	public class RLLIterator extends QLLIterator {
		public T add() {
			Node<T> addIn = pool.getNode();
			addIn.previous = nextNode.previous;
			addIn.next = nextNode;
			addIn.previous.next = addIn;
			addIn.next.previous = addIn;
			++size;
			return addIn.object;
		}
	}

	public RecycleLinkedList(Factory<T> fact) {
		this(new RLLNullPool<T>(fact));
	}

	@SuppressWarnings("unchecked")
	RecycleLinkedList(RLLNullPool<T> pool) {
		super(pool);
		myIterator = (RLLIterator) super.myIterator;
		this.pool = pool;
	}

	@Override
	public RLLIterator listIterator() {
		return new RLLIterator();
	}

	@Override
	public RLLIterator iterator() {
		return listIterator();
	}

	@Override
	public RLLIterator listIterator(int index) {
		RLLIterator rll = listIterator();
		rll.toIndex(index);
		return rll;
	}

	@Override
	public RLLIterator descendingIterator() {
		RLLIterator rll = listIterator();
		rll.toEnd();
		return rll;
	}

	public T add() {
		myIterator.toEnd();
		return myIterator.add();
	}

	public T add(int index) {
		myIterator.toIndex(index);
		return myIterator.add();
	}

	public T addFirst() {
		myIterator.toStart();
		return myIterator.add();
	}

	public T addLast() {
		return add();
	}

	public T offer() {
		return offerLast();
	}

	public T offerFirst() {
		return addFirst();
	}

	public T offerLast() {
		return addLast();
	}

	public T push() {
		return addFirst();
	}
}
