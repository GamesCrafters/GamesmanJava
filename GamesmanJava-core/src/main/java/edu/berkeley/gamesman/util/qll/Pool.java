package edu.berkeley.gamesman.util.qll;

/**
 * A pool of reusable instances
 *
 * <p>To acquire an instance, call {@link #get}. After finishing using the
 * instance, manually call {@link #release} so the acquired object is available
 * for reuse in the future.
 *
 * <p>Avoid calling {@link #release} on objects that are not acquired from this pool.
 *
 * @param <T> Type of objects in the pool
 */
public class Pool<T> {
	private int created = 0;
	private int released = 0;
	private int currentSize;
	private final Factory<T> fact;
	private Node<T> firstNode;
	private Node<T> firstNullNode;

	/**
	 * @param fact A factory that instantiates objects
	 */
	public Pool(Factory<T> fact) {
		this.fact = fact;
	}

	public synchronized T get() {
		if (firstNode == null) {
			assert currentSize == 0;
			created++;
			return fact.newObject();
		} else {
			assert currentSize > 0;
			currentSize--;
			Node<T> changeNode = firstNode;
			firstNode = changeNode.next;
			changeNode.next = firstNullNode;
			firstNullNode = changeNode;
			return changeNode.object;
		}
	}

	/**
	 * Give back an instance for reuse
	 *
	 * <p>No further changes should be made to the object after given for release.
	 *
	 * @param el Instance to be released
	 */
	public synchronized void release(T el) {
		if (el == null)
			throw new NullPointerException("Cannot release null element");
		fact.reset(el);
		currentSize++;
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
