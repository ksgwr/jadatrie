package jp.ksgwr.array;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Extra Array List
 * this list is constant capacity size.
 * it expects to call "get" and "set" than "add", so it can access null items.
 * it implemented by distributed internal array.
 * it can allocate internal array efficiently and write external file partially.
 *
 * @author ksgwr
 *
 * @param <T> item class
 */
public abstract class ExArrayList<T extends Serializable> implements List<T> {

	/** item target class */
	protected Class<T> target;

	/**
	 * constructor
	 * @param target target class
	 */
	public ExArrayList(Class<T> target) {
		this.target = target;
	}

	/**
	 * resize capacity
	 * @param size capacity size
	 */
	abstract public void resize(int size);

	/**
	 * compress capacity, free unuse space
	 * should call on only get use case.
	 */
	abstract public void compress();

	/**
	 * add all item array
	 * @param val item array
	 * @return if true success.
	 */
	abstract public boolean addAll(T[] val);

	/**
	 * load external index
	 * @param directory index directory
	 * @throws IOException file error
	 * @throws ClassNotFoundException target class error
	 */
	abstract public void load(File directory) throws IOException, ClassNotFoundException;

	/**
	 * save index (1 info, 1 data file)
	 * @param directory index directory
	 * @throws IOException file error
	 * @throws ClassNotFoundException target class error
	 */
	public void save(File directory) throws IOException, ClassNotFoundException {
		save(directory, Integer.MAX_VALUE);
	}

	/**
	 * save index
	 * @param directory index directory
	 * @param splitSize split item size
	 * @throws IOException file error
	 * @throws ClassNotFoundException target class error
	 */
	public void save(File directory, int splitSize) throws IOException, ClassNotFoundException {
		directory.mkdirs();
		Index<T> index = new Index<T>(directory);
		index.setSize(this.size());
		index.setSplitSize(splitSize);
		index.save(this.iterator());
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}
	
	@Override
    public boolean remove(Object o) {
        if (o == null) {
            for (int i = 0, size = size(); i < size; i++) {
                if (get(i) == null) {
                	// remove
                    return true;
                }
            }
        } else {
        	for (int i = 0, size = size(); i < size; i++) {
                if (o.equals(get(i))) {
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
	public List<T> subList(int fromIndex, int toIndex) {
		List<T> subList = new ArrayList<T>(toIndex - fromIndex);
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
