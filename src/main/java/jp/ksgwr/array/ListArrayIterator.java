package jp.ksgwr.array;

import java.util.List;
import java.util.ListIterator;

public class ListArrayIterator<T> implements ListIterator<T> {

	private List<T[]> vals;

	private int size;

	private int i;

	private T[] val;

	private int currentIndex;

	private int valIndex;

	public ListArrayIterator(List<T[]> vals, int size) {
		this.vals = vals;
		this.size = size;
		this.i = 0;
		this.currentIndex = 0;
		this.valIndex = 0;
		this.val = vals.get(i);
	}

	@Override
	public boolean hasNext() {
		return i < size;
	}

	@Override
	public T next() {
		if (val.length <= currentIndex) {
			this.currentIndex = 0;
			this.val = vals.get(++valIndex);
		}
		i++;
		return val[currentIndex++];
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasPrevious() {
		return 0 <= i;
	}

	@Override
	public T previous() {
		if (currentIndex < 0) {
			this.currentIndex = 0;
			this.val = vals.get(--valIndex);
		}
		i--;
		return val[currentIndex--];
	}

	@Override
	public int nextIndex() {
		return i + 1;
	}

	@Override
	public int previousIndex() {
		return i - 1;
	}

	@Override
	public void set(T e) {
		val[currentIndex] = e;
	}

	@Override
	public void add(T e) {
		throw new UnsupportedOperationException();
	}

}
