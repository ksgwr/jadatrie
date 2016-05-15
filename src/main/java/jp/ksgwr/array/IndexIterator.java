package jp.ksgwr.array;

import java.io.IOException;
import java.io.Serializable;
import java.util.ListIterator;

public class IndexIterator<T extends Serializable> implements ListIterator<T> {

	private Index<T> index;

	private Class<T> target;

	private int i;

	private int splitSize;

	private int size;

	private T[] vals;

	public IndexIterator(Class<T> target, Index<T> index, T[] vals) {
		this.index = index;
		this.target = target;
		this.i = 0;
		this.size = index.getSize();
		this.splitSize = index.getSplitSize();

		if (splitSize == 0) {
			this.vals = vals;
		}
	}

	@Override
	public boolean hasNext() {
		return i < size;
	}

	@Override
	public T next() {
		int tmpi;
		if (splitSize == 0) {
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
		if (splitSize == 0) {
			tmpi = i;
		} else {
			tmpi = i % splitSize;
			if (tmpi == 0) {
				try {
					this.vals = index.loadSegment(i - splitSize, target);
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
