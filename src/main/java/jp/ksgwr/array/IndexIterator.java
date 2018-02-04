package jp.ksgwr.array;

import java.io.IOException;
import java.io.Serializable;
import java.util.ListIterator;
import java.util.Map;

import jp.ksgwr.array.index.SeparableIndex;

/**
 * index iterator
 *
 * @author ksgwr
 *
 * @param <E>
 */
public class IndexIterator<E extends Serializable> implements ListIterator<E> {

	/** index instance */
	private final SeparableIndex<E> index;

	/** target class */
	private final Class<E> target;

	/** size */
	private final int size;

	/** default value in null */
	private final E defaultValue;

	private final Map<Integer, E[]> cacheSegment;

	/** current index offset */
	private int i;

	private int segmentNum;

	private int segmentOffset;

	private int nextSegmentOffset;

	/** current values */
	private E[] vals;

    public IndexIterator(Class<E> target, SeparableIndex<E> index, E defaultValue) {
        this(target, index, defaultValue, 0, null);
    }


	/**
	 * constructor
	 * @param target target class
	 * @param index index
	 * @param i index
	 */
	public IndexIterator(Class<E> target, SeparableIndex<E> index, E defaultValue, int i, Map<Integer, E[]> cacheSegment) {
		this.index = index;
		this.target = target;
		this.defaultValue = defaultValue;
		this.cacheSegment = cacheSegment;

		this.i = i;
		this.size = index.getItemSize();
		this.segmentNum = index.getSegmentNumber(i);
		this.segmentOffset = index.getOffset(segmentNum);
		this.nextSegmentOffset = segmentOffset + index.getItemPerSegmentSize(segmentNum);
		this.vals = getSegment(segmentNum);
	}

	private E[] getSegment(int segmentNum) {
        E[] vals = null;
	    if (cacheSegment != null) {
            vals = cacheSegment.get(segmentNum);
        }
	    if (vals == null) {
            try {
                vals = index.loadSegment(segmentNum, target);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return vals;
    }

	@Override
	public boolean hasNext() {
		return i < size;
	}

	@Override
	public E next() {
		if (i == nextSegmentOffset) {
            this.vals = getSegment(++segmentNum);
            this.segmentOffset = this.nextSegmentOffset;
            this.nextSegmentOffset += this.vals.length;
		}
		int tmpi = i - segmentOffset;
		i++;
		E tmpVal = vals[tmpi];
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
	public E previous() {
		if (i < segmentOffset) {
            this.vals = getSegment(--segmentNum);
            this.nextSegmentOffset = this.segmentOffset;
            this.segmentOffset -= this.vals.length;
		}
		int tmpi = i - segmentOffset;
		i--;
		E tmpVal = vals[tmpi];
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
	public void set(E e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void add(E e) {
		throw new UnsupportedOperationException();
	}

}
