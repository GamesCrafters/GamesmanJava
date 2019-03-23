package edu.berkeley.gamesman.util.qll;

/**
 * Generic doubly linked list node
 */
class Node<T> {
	/**
	 * Object at current node
	 */
	T object;

	/**
	 * Next node
	 */
	Node<T> next;

	/**
	 * Prev node
	 */
	Node<T> previous;
}
