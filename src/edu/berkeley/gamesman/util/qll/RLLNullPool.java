package edu.berkeley.gamesman.util.qll;

class RLLNullPool<T> extends QLLNullPool<T> {
	private final Factory<T> myFactory;

	RLLNullPool(Factory<T> oFactory) {
		myFactory = oFactory;
	}

	@Override
	Node<T> getNode() {
		if (firstNull == null) {
			Node<T> result = new Node<T>();
			result.object = myFactory.newObject();
			return result;
		} else {
			Node<T> result = firstNull;
			firstNull = firstNull.next;
			myFactory.reset(result.object);
			return result;
		}
	}
}
