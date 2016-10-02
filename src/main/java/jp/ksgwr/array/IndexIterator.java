package jp.ksgwr.array;

import java.io.IOException;
import java.io.Serializable;
import java.util.ListIterator;

/**
 * index iterator
 *
 * @author ksgwr
 *
 * @param <T>
 */
public class IndexIterator<T extends Serializable> implements ListIterator<T> {

	/** index instance */
	private Index<T> index;

	/** target class */
	private Class<T> target;

	/** current index offset */
	private int i;

	/** split size */
	private int splitSize;

	/** size */
	private int size;

	/** current values */
	private T[] vals;

	/**
	 * constructor
	 * @param target target class
	 * @param index index
	 * @param vals current vals, can set null (on memory).
	 */
	public IndexIterator(Class<T> target, Index<T> index, T[] vals) {
		this(target, index, vals, 0);
	}

	/**
	 * constructor
	 * @param target target class
	 * @param index index
	 * @param vals current vals
	 * @param i index
	 */
	public IndexIterator(Class<T> target, Index<T> index, T[] vals, int i) {
		this.index = index;
		this.target = target;
		this.i = i;
		this.size = index.getSize();
		this.splitSize = index.getSplitSize();

		if (splitSize == Integer.MAX_VALUE) {
			this.vals = vals;
		} else {
			try {
				this.vals = index.loadSegment((i / splitSize) * splitSize, target);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public boolean hasNext() {
		return i < size;
	}

	@Override
	public T next() {
		int tmpi;
		if (splitSize == Integer.MAX_VALUE) {
			tmpi = i;
		} else {
			tmpi = i % splitSize;
			if (tmpi == 0) {
				try {
					this.vals = index.loadSegment(i, target);
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
		i++;
		return vals[tmpi];
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	/**
	 * get current index
	 * @return current index
	 */
	public int index() {
		return i;
	}

	@Override
	public boolean hasPrevious() {
		return 0 <= i;
	}

	@Override
	public T previous() {
		int tmpi;
		if (splitSize == Integer.MAX_VALUE) {
			tmpi = i;
		} else {
			tmpi = i % splitSize;
			if (tmpi == 0) {
				try {
					T tmp = vals[tmpi];
					if (i > 0) {
						this.vals = index.loadSegment(i - splitSize, target);
					}
					i--;
					return tmp;
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
		i--;
		return vals[tmpi];
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
		throw new UnsupportedOperationException();
	}

	@Override
	public void add(T e) {
		throw new UnsupportedOperationException();
	}

}
