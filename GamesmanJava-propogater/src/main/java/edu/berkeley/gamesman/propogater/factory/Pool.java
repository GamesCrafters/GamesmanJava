package edu.berkeley.gamesman.propogater.factory;

import org.apache.hadoop.conf.Configuration;

public class Pool<T> {
	private final Factory<? extends T> myFactory;
	private final NodePool<T> myPool = new NodePool<T>();
	private Node<T> first = null;

	public Pool(Class<? extends T> type, Configuration conf) {
		this(FactoryUtil.makeFactory(type, conf));
	}

	public Pool(Factory<? extends T> fact) {
		myFactory = fact;
	}

	public T get() {
		if (first == null)
			return myFactory.create();
		else {
			Node<T> resNode = first;
			first = first.next;
			T obj = resNode.object;
			myPool.release(resNode);
			return obj;
		}
	}

	public void release(T obj) {
		Node<T> toStore = myPool.get();
		toStore.object = obj;
		toStore.next = first;
		first = toStore;
	}
}
