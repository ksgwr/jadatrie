package jp.ksgwr.array;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jp.ksgwr.array.index.SeparableIndex;

/**
 * Extra Array List
 * this list is constant capacity size.
 * it expects to call "get" and "set" than "add", so it can access null items.
 * it implemented by distributed internal array.
 * it can allocate internal array efficiently and write external file partially.
 *
 * @author ksgwr
 *
 * @param <E> item class
 */
public abstract class AbstractExArrayList<E> implements ExArrayList<E> {

	/** item target class */
	protected final Class<E> target;

	protected final E defaultValue;

	/**
	 * constructor
	 * @param target target class
	 */
	protected AbstractExArrayList(Class<E> target) {
		this(target, null);
	}

	/**
	 * constructor
	 * @param target target class
	 */
	protected AbstractExArrayList(Class<E> target, E defaultValue) {
		this.target = target;
		this.defaultValue = defaultValue;
	}

	/**
	 * resize capacity
	 * @param size capacity size
	 */
	abstract public void resize(int size);

	/**
	 * compress capacity, free unuse space
	 * should call on only get use case.
	 * @return compress size
	 */
	abstract public int compress();

	/**
	 * add all item array
	 * @param val item array
	 * @return if true success.
	 */
	abstract public boolean addAll(E[] val);

	/**
	 * add all item array
	 * @param index index
	 * @param val item array
	 * @return if true success.
	 */
	abstract public boolean addAll(int index, E[] val);



	@Override
	public void close() throws IOException {

	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
    public boolean remove(Object o) {
        if (o == null) {
			for (E e : this) {
				if (e == null) {
					// remove
					return true;
				}
			}
        } else {
			for (E e : this) {
				if (o.equals(e)) {
					// remove
					return true;
				}
			}
        }
        return false;
    }

	@Override
	public boolean contains(Object o) {
		return indexOf(o) >= 0;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for (Object e: c) {
			if (!contains(e)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int indexOf(Object o) {
		if (o == null) {
			for (int i = 0, size = size(); i < size; i++) {
				if (get(i) == null) {
					return i;
				}
			}
		} else {
			for (int i = 0, size = size(); i < size; i++) {
				if (o.equals(get(i))) {
					return i;
				}
			}
		}
		return -1;
	}

	@Override
	public int lastIndexOf(Object o) {
		if (o == null) {
			for (int i = size() - 1; i >= 0; i--) {
				if (get(i) == null) {
					return i;
				}
			}
		} else {
			for (int i = size() - 1; i >= 0; i--) {
				if (o.equals(get(i))) {
					return i;
				}
			}
		}
		return -1;
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		List<E> subList = new ArrayList<>(toIndex - fromIndex);
		for (int i = fromIndex; i <= toIndex; i++) {
			subList.add(this.get(i));
		}
		return subList;
	}

	@SuppressWarnings({ "hiding", "unchecked" })
	@Override
	public <T> T[] toArray(T[] a) {
		return (T[])toArray();
	}
}
