package jp.ksgwr.array;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public abstract class ExArrayList<T extends Serializable> implements List<T> {

	protected Class<T> target;

	public ExArrayList(Class<T> target) {
		this.target = target;
	}

	abstract public void resize(int size);

	abstract public void compress();

	abstract public boolean addAll(T[] val);

	abstract public void load(File directory) throws IOException, ClassNotFoundException;

	public void save(File directory) throws IOException, ClassNotFoundException {
		save(directory, Integer.MAX_VALUE);
	}

	public void save(File directory, int splitSize) throws IOException, ClassNotFoundException {
		directory.mkdirs();
		Index<T> index = new Index<T>(directory);
		index.setSize(this.size());
		index.setSplitSize(splitSize);
		index.setIterator(this.iterator());
		index.save();
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public boolean contains(Object o) {
		Iterator<T> ite = this.iterator();
		while (ite.hasNext()) {
			T t = ite.next();
			if (t == o) {
				return true;
			}
		}
		return false;
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

	@SuppressWarnings({ "hiding", "unchecked" })
	@Override
	public <T> T[] toArray(T[] a) {
		return (T[])toArray();
	}
}
