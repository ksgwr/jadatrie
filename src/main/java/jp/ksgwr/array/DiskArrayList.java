package jp.ksgwr.array;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * TODO: rename PartialCachedIndexArrayList
 * @author kohei
 *
 * @param <T>
 */
public class DiskArrayList<T extends Serializable> extends ExArrayList<T> {

	private SeparatableIndex<T> index;

	private T[] vals;

	// 境界値付近の場合を備えて1つ前をcacheする
	private T[] cacheVals;

	private int cacheOffset;

	private int cacheSegmentNum;

	private int offset;

	private int segmentNum;

	private int size;

	private int segmentSize;

	private boolean isUpdated;

	private boolean isUpdatedInfo;

	public DiskArrayList(Class<T> target, File directory, int size) throws IOException {
		this(target, new InfoSegmentIndex<>(directory, "", new ObjectStreamIndexer<T>()), size);
	}

	public DiskArrayList(Class<T> target, File directory, int size, int separateSize) throws IOException {
		this(target, new InfoFixSeparateSegmentIndex<>(directory, "", separateSize, new ObjectStreamIndexer<T>()), size);
	}

	@SuppressWarnings("unchecked")
	public DiskArrayList(Class<T> target, SeparatableIndex<T> index, int size) throws IOException {
		super(target);
		this.cacheVals = null;
		this.cacheSegmentNum = -1;

		this.size = -1;
		this.offset = 0;
		this.segmentNum = 0;
		this.index = index;
		if (index != null) {
			int valSize = index.getItemPerSegmentSize(segmentNum);
			this.vals = (T[]) Array.newInstance(target, valSize);
			index.cleanup();
			updateSize(size);
			this.segmentSize = index.getSegmentSize();
		}
	}

	public DiskArrayList(Class<T> target, SeparatableIndex<T> index) throws IOException, ClassNotFoundException {
		super(target);
		this.cacheVals = null;
		this.cacheSegmentNum = -1;

		this.offset = 0;
		this.segmentNum = 0;
		this.vals = index.loadSegment(segmentNum, target);

		this.index = index;
		this.size = index.getItemSize();
		this.segmentSize = index.getSegmentSize();
	}

	private final void updateSize(int size) {
		if (this.size != size) {
			isUpdatedInfo = true;
			this.size = size;
			index.updateItemSize(size);
			this.segmentSize = index.getSegmentSize();
		}
	}

	@Override
	public void close() throws IOException {
		saveCacheSegmentIfUpdated();
		saveInfoIfUpdated();
	}

	public void saveCacheSegmentIfUpdated() throws IOException {
		if (isUpdated) {
			index.saveSegment(segmentNum, vals);
			isUpdated = false;
		}
	}

	public void saveInfoIfUpdated() throws IOException {
		if (isUpdatedInfo) {
			index.saveInfo();
			isUpdatedInfo = false;
		}
	}

	private final void initForAllScan() {
		if (this.segmentSize > 1) {
			try {
				saveCacheSegmentIfUpdated();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private void loadCacheOrNewSegment(int i) {
		try {
			int tmpOffset = this.offset;
			int tmpSegmentNum = this.segmentNum;
			saveCacheSegmentIfUpdated();
			this.segmentNum = index.getSegmentNumber(i);
			this.offset = index.getOffset(segmentNum);
			if (this.segmentNum == this.cacheSegmentNum) {
				// swap cache and current value
				T[] tmpVals = this.cacheVals;
				this.cacheVals = this.vals;

				this.vals = tmpVals;
			} else {
				this.cacheVals = this.vals;

				this.vals = index.loadSegment(segmentNum, target);
			}
			this.cacheOffset = tmpOffset;
			this.cacheSegmentNum = tmpSegmentNum;
		} catch (ClassNotFoundException e1) {
			throw new RuntimeException(e1);
		} catch (IOException e1) {
			throw new RuntimeException(e1);
		}
	}

	@Override
	public T get(int i) {
		try {
			return vals[i - offset];
		} catch (NullPointerException e) {
			loadCacheOrNewSegment(i);
			return vals[i - offset];
		} catch (ArrayIndexOutOfBoundsException e) {
			loadCacheOrNewSegment(i);
			return vals[i - offset];
		}
	}

	@Override
	public T set(int i, T t) {
		try {
			// TODO: 未使用領域にアクセスする可能性があるのでoffsetチェックを行った方が良い
			vals[i - offset] = t;
		} catch (NullPointerException e) {
			loadCacheOrNewSegment(i);
			vals[i - offset] = t;
		} catch (ArrayIndexOutOfBoundsException e) {
			loadCacheOrNewSegment(i);
			vals[i - offset] = t;
		}
		isUpdated = true;
		return t;
	}

	@Override
	public Iterator<T> iterator() {
		initForAllScan();
		try {
			T[] val = getSegmentBySegmentNumber(0);
			return new IndexIterator<T>(target, index, val, 0);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private T[] getSegmentBySegmentNumber(int segmentNum) throws ClassNotFoundException, IOException {
		if (this.segmentNum == segmentNum) {
			return this.vals;
		} else if(this.cacheSegmentNum == segmentNum) {
			return this.cacheVals;
		} else {
			return index.loadSegment(segmentNum, target);
		}
	}

	@SuppressWarnings("unchecked")
	private void changeCacheValSize(int newValSize) {
		int lastSegmentNum = this.segmentSize - 1;
		T[] tmpVal = null;
		if (this.segmentNum == lastSegmentNum) {
			tmpVal = this.vals;
		} else if(this.cacheSegmentNum == lastSegmentNum) {
			tmpVal = this.cacheVals;
			this.offset = this.cacheOffset;
			this.segmentNum = lastSegmentNum;
		}

		if (tmpVal != null) {
			if(tmpVal.length < newValSize) {
				int maxValSize = Math.max(newValSize, index.getItemPerSegmentSize(segmentNum));
				this.vals = (T[]) Array.newInstance(target, maxValSize);
				System.arraycopy(tmpVal, 0, this.vals, 0, tmpVal.length);
			} else if(newValSize < tmpVal.length  && 0 < newValSize) {
				this.vals = tmpVal;
				Arrays.fill(tmpVal, newValSize, tmpVal.length, null);
				isUpdated = true;
			}
		}
	}

	@Override
	public void resize(int size) {
		try {
			if (size == 0) {
				if (this.size != size) {
					index.cleanup();
					updateSize(size);
					this.vals = null;
					this.cacheVals = null;
				}
			} else if (size < 0) {
				throw new ArrayIndexOutOfBoundsException(size);
			} else {
				updateSize(size);
				changeCacheValSize(size);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void compress() {
		try {
			int lastSegmentNum = this.segmentSize - 1;
			int lastOffset = index.getOffset(lastSegmentNum);
			T[] lastVal = getSegmentBySegmentNumber(lastSegmentNum);
			int i = size - 1;

			int valIndex = -1;
			while (i >= 0) {
				valIndex = i - lastOffset;
				while(valIndex >= 0 && lastVal[valIndex] == null) {
					valIndex--;
					i--;
				}
				if (valIndex < 0) {
					index.deleteSegment(lastSegmentNum);
					lastSegmentNum--;
					lastVal = getSegmentBySegmentNumber(lastSegmentNum);
					lastOffset = index.getOffset(lastSegmentNum);

					if (lastSegmentNum < this.segmentNum) {
						this.offset = lastOffset;
						this.segmentNum = lastSegmentNum;
						this.vals = lastVal;
					}
					if (lastSegmentNum < this.cacheSegmentNum) {
						this.cacheOffset = lastOffset;
						this.cacheSegmentNum = lastSegmentNum;
						this.cacheVals = lastVal;
					}
				} else {
					break;
				}
			}
			i++;
			if (0 <= valIndex && valIndex < lastVal.length - 1 && i != size) {
				isUpdated = true;
			}
			updateSize(i);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public int size() {
		return size;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void load(SeparatableIndex<T> index) throws IOException, ClassNotFoundException {
		this.cacheOffset = -1;
		this.cacheSegmentNum = -1;
		this.cacheVals = null;
		this.offset = 0;
		this.segmentNum = 0;

		updateSize(index.getItemSize());

		IndexIterator<T> iterator = new IndexIterator<>(target, index);
		if (this.segmentSize > 1) {
			this.index.save(iterator, size, target);
			this.vals = this.index.loadSegment(this.segmentNum, target);
			this.isUpdated = false;
		} else {
			// load cache only, not save at time
			this.vals = (T[]) Array.newInstance(target, index.getItemPerSegmentSize(segmentNum));
			while (iterator.hasNext()) {
				this.vals[iterator.index()] = iterator.next();
			}
			this.isUpdated = true;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object[] toArray() {
		T[] obj = (T[]) Array.newInstance(target, size);
		Iterator<T> ite = this.iterator();
		int i = 0;
		while (ite.hasNext()) {
			obj[i++] = ite.next();
		}
		return obj;
	}

	@Override
	public boolean add(T t) {
		try {
			int oldSize = size;
			updateSize(size + 1);
			int lastSegmentNum = this.segmentSize - 1;
			T[] vals = getSegmentBySegmentNumber(lastSegmentNum);
			int offset = index.getOffset(lastSegmentNum);
			if (vals.length == oldSize - offset) {
				// 効率化のために余分にサイズを拡大
				changeCacheValSize(oldSize + vals.length);
			}
			this.set(oldSize, t);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return true;
	}


	@Override
	public void add(int i, T element) {
		if (i == size) {
			add(element);
		} else if(i < 0 && size < i) {
			throw new ArrayIndexOutOfBoundsException(i);
		} else {
			try {
				int oldSize = size;
				updateSize(size + 1);
				int tmpSegmentNum = this.segmentSize - 1;
				T[] tmpVals = getSegmentBySegmentNumber(tmpSegmentNum);
				int tmpOffset = index.getOffset(tmpSegmentNum);
				if (tmpVals.length == oldSize - tmpOffset) {
					// 効率化のために余分にサイズを拡大
					changeCacheValSize(oldSize + vals.length);
					tmpVals = this.vals;
				}

				if (tmpSegmentNum > 0) {
					this.saveCacheSegmentIfUpdated();
				}
				while (tmpSegmentNum > 0) {
					T[] targetVals = tmpVals;
					System.arraycopy(tmpVals, 0, tmpVals, 1, tmpVals.length - 1);
					tmpSegmentNum--;
					tmpOffset = index.getOffset(tmpSegmentNum);
					tmpVals = index.loadSegment(tmpSegmentNum, target);
					targetVals[0] = tmpVals[tmpVals.length - 1];
					index.saveSegment(tmpSegmentNum + 1, targetVals);
				}
				int tmpi = i - tmpOffset;
				System.arraycopy(tmpVals, tmpi, tmpVals, tmpi + 1, tmpVals.length - tmpi - 1);
				tmpVals[tmpi] = element;
				this.cacheSegmentNum = -1;
				this.cacheVals = null;
				this.segmentNum = tmpSegmentNum;
				this.offset = tmpOffset;
				this.vals = tmpVals;
				this.isUpdated = true;
			} catch (IOException e) {
				throw new RuntimeException(e);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public T remove(int i) {
		T ret = null;
		if (i < 0 && size <= i) {
			throw new ArrayIndexOutOfBoundsException(i);
		} else {
			try {
				if (this.segmentSize > 1) {
					this.saveCacheSegmentIfUpdated();
				}
				int tmpSegmentNum = this.segmentSize - 1;
				T[] tmpVal = getSegmentBySegmentNumber(tmpSegmentNum);
				T tmpItem = null;
				int lastIndex;
				while (tmpSegmentNum > 0) {
					lastIndex = tmpVal.length - 1;
					T firstItem = tmpVal[0];
					System.arraycopy(tmpVal, 1, tmpVal, 0, lastIndex);
					tmpVal[lastIndex] = tmpItem;
					tmpItem = firstItem;
					index.saveSegment(tmpSegmentNum, tmpVal);
					tmpSegmentNum--;
					tmpVal = index.loadSegment(tmpSegmentNum, target);
				}
				int tmpOffset = index.getOffset(tmpSegmentNum);
				int tmpi = i - tmpOffset;
				ret = tmpVal[tmpi];
				System.arraycopy(tmpVal, tmpi + 1, tmpVal, tmpi, tmpVal.length - tmpi - 1);
				tmpVal[tmpVal.length - 1] = tmpItem;

				updateSize(size - 1);
				this.cacheSegmentNum = -1;
				this.cacheVals = null;
				this.segmentNum = tmpSegmentNum;
				this.offset = tmpOffset;
				this.vals = tmpVal;
				this.isUpdated = true;
			} catch (IOException e) {
				throw new RuntimeException(e);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
		return ret;
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		for (T item : c) {
			add(item);
		}
		return true;
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		int mvSize = size - index;
		int colSize = c.size();
		this.resize(size + colSize);
		for (int i = 0, j = size - 1; i < mvSize; i++, j--) {
			this.set(j, get(j - colSize));
		}
		Iterator<? extends T> ite = c.iterator();
		while (ite.hasNext()) {
			this.set(index++, ite.next());
		}
		return true;
	}

	@Override
	public boolean addAll(T[] val) {
		for (T item : val) {
			add(item);
		}
		return true;
	}

	@Override
	public boolean addAll(int index, T[] val) {
		int mvSize = size - index;
		int colSize = val.length;
		this.resize(size + colSize);
		for (int i = 0, j = size - 1; i < mvSize; i++, j--) {
			this.set(j, get(j - colSize));
		}
		for (T item : val) {
			this.set(index++, item);
		}
		return true;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return batchRemove(c, false);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return batchRemove(c, true);
	}

	private boolean batchRemove(Collection<?> c, boolean complement) {
		int w = 0;
		boolean modified = false;
		for (int i = 0; i < size; i++) {
			T val = this.get(i);
			if (c.contains(val) == complement) {
				this.set(w++, val);
			}
		}
		if (w != size) {
			size = w;
			updateSize(size);
			modified = true;
			isUpdated = true;
		}
		return modified;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void clear() {
		this.cacheOffset = -1;
		this.cacheVals = null;
		this.isUpdated = true;
		this.isUpdatedInfo = true;

		if (this.segmentSize == 1) {
			Arrays.fill(vals, null);
		} else {
			try {
				this.segmentNum = 0;
				this.offset = 0;
				int valSize = index.getItemPerSegmentSize(segmentNum);
				this.vals = (T[]) Array.newInstance(target, valSize);
				index.saveSegment(segmentNum, vals);
				for (int i = 1; i < segmentSize; i++) {
					valSize = index.getItemPerSegmentSize(i);
					T[] val = (T[]) Array.newInstance(target, valSize);
					index.saveSegment(i, val);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public int indexOf(Object o) {
		initForAllScan();
		return super.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		initForAllScan();
		return super.lastIndexOf(o);
	}

	@Override
	public ListIterator<T> listIterator() {
		initForAllScan();
		try {
			T[] val = getSegmentBySegmentNumber(0);
			return new IndexIterator<T>(target, index, val, 0);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public ListIterator<T> listIterator(int index) {
		initForAllScan();
		try {
			int segmentNum = this.index.getSegmentNumber(index);
			T[] val = getSegmentBySegmentNumber(segmentNum);
			return new IndexIterator<T>(target, this.index, val, index);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<T> subList(int fromIndex, int toIndex) {
		initForAllScan();
		return super.subList(fromIndex, toIndex);
	}

}
