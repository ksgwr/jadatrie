package jp.ksgwr.jadatrie.core;

import java.util.AbstractList;
import java.util.List;
import java.util.RandomAccess;

public class EfficientNodeList extends AbstractList<Node>
implements List<Node>, RandomAccess, Cloneable, java.io.Serializable
{

	private transient Node[] nodes;

	private int size;

	public EfficientNodeList() {
		this(0);
	}

	public EfficientNodeList(int capacity) {
		nodes = new Node[capacity];
	}

	@Override
	public Node get(int index) {
		if (size <= index) {
			return null;
		}
		return nodes[index];
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public void clear() {
		size = 0;
	}

	/**
	 * clear elements
	 * @param capacity predictive add element size
	 */
	public void clear(int capacity) {
		if (nodes.length < capacity) {
			this.nodes = new Node[capacity];
		}
		size = 0;
	}

	@Override
	public boolean add(Node element) {
		Node node = nodes[size];
		if (node == null) {
			nodes[size] = node = new Node();
		}
		node.set(element);
		size++;
		return true;
	}

}
