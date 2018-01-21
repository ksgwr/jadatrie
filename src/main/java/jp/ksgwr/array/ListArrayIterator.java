package jp.ksgwr.array;

import java.util.List;
import java.util.ListIterator;

/**
 * List Array (List<T[]>) iterator
 *
 * @author ksgwr
 *
 * @param <T>
 */
public class ListArrayIterator<T> implements ListIterator<T> {

	/** default value at null */
	private final T defaultValue;

	/** list array */
	private final List<T[]> vals;

	/** size */
	private final int size;

	/** current index */
	private int i;

	/** current array */
	private T[] val;

	/** current list index */
	private int currentIndex;

	/** current array index */
	private int valIndex;

	/**
	 * constructor
	 * @param vals list array
	 * @param size size
	 */
	public ListArrayIterator(List<T[]> vals, int size, T defaultValue) {
		this.vals = vals;
		this.size = size;
		this.defaultValue = defaultValue;
		this.i = 0;
		this.currentIndex = 0;
		this.valIndex = 0;
		this.val = vals.get(0);
	}

	public ListArrayIterator(List<T[]> vals, int size, int index, T defaultValue) {
		this.vals = vals;
		this.size = size;
		this.i = index;
		this.defaultValue = defaultValue;

		this.currentIndex = 0;
		this.valIndex = 0;

		int offset = 0;
		for (;this.valIndex < vals.size(); this.valIndex++) {
			this.val = vals.get(this.valIndex);
			if ((this.currentIndex = index - offset) < this.val.length) {
				break;
			}
			offset += val.length;
		}

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
		T v = val[currentIndex++];
		return v == null ? defaultValue : v;
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
		T v = val[currentIndex--];
		return v == null ? defaultValue : v;
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
