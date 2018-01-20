package jp.ksgwr.array;

import java.io.IOException;
import java.io.Serializable;
import java.util.ListIterator;

import jp.ksgwr.array.index.SeparatableIndex;

/**
 * index iterator
 *
 * @author ksgwr
 *
 * @param <T>
 */
public class IndexIterator<T extends Serializable> implements ListIterator<T> {

	/** index instance */
	private SeparatableIndex<T> index;

	/** target class */
	private Class<T> target;

	/** current index offset */
	private int i;

	private int segmentNum;

	private int segmentOffset;

	private int nextSegmentOffset;

	/** size */
	private int size;

	/** current values */
	private T[] vals;

	private T defaultValue;

	/**
	 * constructor
	 * @param target target class
	 * @param index index
	 */
	public IndexIterator(Class<T> target, SeparatableIndex<T> index, T defaultValue) {
		this(target, index, defaultValue, null, 0);
	}

	/**
	 * constructor
	 * @param target target class
	 * @param index index
	 * @param vals current vals
	 * @param i index
	 */
	public IndexIterator(Class<T> target, SeparatableIndex<T> index, T defaultValue, T[] vals, int i) {
		this.index = index;
		this.target = target;
		this.defaultValue = defaultValue;
		this.i = i;
		this.size = index.getItemSize();
		this.segmentNum = index.getSegmentNumber(i);
		this.segmentOffset = index.getOffset(segmentNum);
		this.nextSegmentOffset = segmentOffset + index.getItemPerSegmentSize(segmentNum);
		if (vals == null) {
			try {
				this.vals = index.loadSegment(segmentNum, target);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			this.vals = vals;
		}
	}

	@Override
	public boolean hasNext() {
		return i < size;
	}

	@Override
	public T next() {
		if (i == nextSegmentOffset) {
			try {
				this.vals = index.loadSegment(++segmentNum, target);
				this.segmentOffset = this.nextSegmentOffset;
				this.nextSegmentOffset += this.vals.length;
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		int tmpi = i - segmentOffset;
		i++;
		T tmpVal = vals[tmpi];
		return tmpVal == null ? defaultValue : tmpVal;
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
		if (i < segmentOffset) {
			try {
				this.vals = index.loadSegment(--segmentNum, target);
				this.nextSegmentOffset = this.segmentOffset;
				this.segmentOffset -= this.vals.length;
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		int tmpi = i - segmentOffset;
		i--;
		T tmpVal = vals[tmpi];
		return tmpVal == null ? defaultValue : tmpVal;
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
