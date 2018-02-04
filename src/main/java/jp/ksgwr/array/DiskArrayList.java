package jp.ksgwr.array;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.*;

import jp.ksgwr.array.exception.DiskArrayListClassNotFoundException;
import jp.ksgwr.array.exception.DiskArrayListIOException;
import jp.ksgwr.array.index.InfoFixSeparateSegmentIndex;
import jp.ksgwr.array.index.InfoSegmentIndex;
import jp.ksgwr.array.index.ObjectStreamIndexer;
import jp.ksgwr.array.index.SeparableIndex;

/**
 * TODO: rename PartialCachedIndexArrayList
 *
 * ファイル書き出し戦略:getでキャッシュにヒットしないセグメントを読み出す時、resizeで不要なセグメントを削除する時
 *
 * @author kohei
 *
 * @param <E>
 */
public class DiskArrayList<E extends Serializable> extends AbstractExArrayList<E> implements IndexableExArrayList<E> {

	private SeparableIndex<E> index;

	private E[] cacheVals;

	private int cacheOffset;

	private int cacheSegmentNum;

	// 境界値付近の場合を備えて1つ前をcacheする
	private E[] secondCacheVals;

	private int secondCacheOffset;

	private int secondSegmentNum;

	private int size;

	private int segmentSize;

	private boolean isUpdatedCache;

	private boolean isUpdatedSecondCache;

	private boolean isUpdatedInfo;

	public DiskArrayList(Class<E> target, File directory, int size) {
		this(target, new InfoSegmentIndex<>(directory, "", new ObjectStreamIndexer<>()), size);
	}

	public DiskArrayList(Class<E> target, File directory, int size, int separateSize) {
		this(target, new InfoFixSeparateSegmentIndex<>(directory, "", separateSize, new ObjectStreamIndexer<>()), size);
	}

	public DiskArrayList(Class<E> target, SeparableIndex<E> index, int size) {
		this(target, null, index, size);
	}

	@SuppressWarnings("unchecked")
	public DiskArrayList(Class<E> target, E defaultValue, SeparableIndex<E> index, int size) {
		super(target, defaultValue);
		this.secondCacheVals = null;
		this.secondSegmentNum = -1;

		this.size = -1;
		this.cacheOffset = 0;
		this.cacheSegmentNum = 0;
		this.index = index;
		if (index != null) {
			index.cleanup();
			increaseSize(size);
			int valSize = index.getItemPerSegmentSize(cacheSegmentNum);
			this.cacheVals = (E[]) Array.newInstance(target, valSize);
			this.segmentSize = index.getSegmentSize();
		}
	}

	public DiskArrayList(Class<E> target, SeparableIndex<E> index) throws IOException, ClassNotFoundException {
		this(target, null, index);
	}

	public DiskArrayList(Class<E> target, E defaultValue, SeparableIndex<E> index) throws IOException, ClassNotFoundException {
		super(target, defaultValue);
		this.secondCacheVals = null;
		this.secondSegmentNum = -1;

		this.cacheOffset = 0;
		this.cacheSegmentNum = 0;
		this.cacheVals = index.loadSegment(cacheSegmentNum, target);

		this.index = index;
		index.loadInfo();
		this.size = index.getItemSize();
		this.segmentSize = index.getSegmentSize();
	}

	private void increaseSize(int size) {
		isUpdatedInfo = true;
		this.size = size;
		index.increaseItemSize(size);
		this.segmentSize = index.getSegmentSize();
	}

	private void decreaseSize(int size) {
		isUpdatedInfo = true;
		this.size = size;
		index.decreaseItemSize(size);
	}

	private void increaseAllocateSize(int size) {
		isUpdatedInfo = true;
		index.increaseAllocateSize(size);
		this.segmentSize = index.getSegmentSize();
	}

	private void decreaseAllocateSize(int size) {
		isUpdatedInfo = true;
		index.decreaseAllocateSize(size);
		this.segmentSize = index.getSegmentSize();
	}

	@Override
	public void close() throws IOException {
		saveCacheSegmentIfUpdated();
		saveInfoIfUpdated();
	}

	private void saveSecondCacheSegment() throws IOException {
		saveSegment(secondSegmentNum, secondCacheVals, secondCacheOffset);
		isUpdatedSecondCache = false;
	}

	public void saveCacheSegmentIfUpdated() throws IOException {
		if (isUpdatedCache) {
			saveSegment(cacheSegmentNum, cacheVals, cacheOffset);
			isUpdatedCache = false;
		}
		if (isUpdatedSecondCache) {
			saveSecondCacheSegment();
		}
	}

	public void saveInfoIfUpdated() throws IOException {
		if (isUpdatedInfo) {
			index.saveInfo();
			isUpdatedInfo = false;
		}
	}

	private void saveSegment(int segmentNum, E[] vals, Integer offset) throws IOException {
		if (segmentNum == this.segmentSize - 1) {
			// 最後のsegmentはsize分のみを保存する
			if (offset == null) {
				offset = index.getOffset(segmentNum);
			}
			int length = size - offset;
			index.saveSegment(segmentNum, vals, length);
		} else {
			index.saveSegment(segmentNum, vals, vals.length);
		}
	}

	private void loadCacheOrNewSegment(int i) {
		loadCacheOrNewSegmentBySegmentNum(index.getSegmentNumber(i));
	}

	private void loadCacheOrNewSegmentBySegmentNum(int segmentNum) {
		try {
			if (this.secondSegmentNum == segmentNum) {
				// swap cache
				int tmpSegmentNum = this.cacheSegmentNum;
				int tmpOffset = this.cacheOffset;
				E[] tmpVal = this.cacheVals;
				boolean tmpIsUpdatedCache = this.isUpdatedCache;

				this.cacheSegmentNum = this.secondSegmentNum;
				this.cacheOffset = this.secondCacheOffset;
				this.cacheVals = this.secondCacheVals;
				this.isUpdatedCache = this.isUpdatedSecondCache;

				this.secondSegmentNum = tmpSegmentNum;
				this.secondCacheOffset = tmpOffset;
				this.secondCacheVals = tmpVal;
				this.isUpdatedSecondCache = tmpIsUpdatedCache;
			} else {
				// load new segment
				if (this.isUpdatedSecondCache && !this.isUpdatedCache) {
					// out cache, and not save
				} else {
					if (this.isUpdatedSecondCache) {
						saveSegment(this.secondSegmentNum, this.secondCacheVals, this.secondCacheOffset);
					}
					// out second cache
					this.secondSegmentNum = this.cacheSegmentNum;
					this.secondCacheOffset = this.cacheOffset;
					this.secondCacheVals = this.cacheVals;
					this.isUpdatedSecondCache = this.isUpdatedCache;
				}

				this.cacheSegmentNum = segmentNum;
				this.cacheOffset = index.getOffset(segmentNum);
				this.cacheVals = index.loadSegment(segmentNum, target);
				this.isUpdatedCache = false;
			}
		} catch (ClassNotFoundException e) {
			throw new DiskArrayListClassNotFoundException(e);
		} catch (IOException e) {
			throw new DiskArrayListIOException(e);
		}
	}

	@Override
	public E get(int i) {
		E v;
		try {
			v = cacheVals[i - cacheOffset];
		} catch (NullPointerException e) {
			loadCacheOrNewSegment(i);
			v = cacheVals[i - cacheOffset];
		} catch (ArrayIndexOutOfBoundsException e) {
			loadCacheOrNewSegment(i);
			try {
				v = cacheVals[i - cacheOffset];
			} catch (ArrayIndexOutOfBoundsException e2) {
				e2.printStackTrace();
				v = null;
			}
		}
		return v == null ? defaultValue : v;
	}

	@Override
	public E set(int i, E t) {
		try {
			// TODO: 未使用領域にアクセスする可能性があるのでoffsetチェックを行った方が良い
			cacheVals[i - cacheOffset] = t;
		} catch (NullPointerException e) {
			loadCacheOrNewSegment(i);
			cacheVals[i - cacheOffset] = t;
		} catch (ArrayIndexOutOfBoundsException e) {
			loadCacheOrNewSegment(i);
			try {
				cacheVals[i - cacheOffset] = t;
			} catch (ArrayIndexOutOfBoundsException e2) {
				e2.printStackTrace();
			}
		}
		isUpdatedCache = true;
		return t;
	}

	private IndexIterator<E> createIndexIterator(int i) {
		Map<Integer, E[]> cacheSegment = new TreeMap<>();
		if (this.cacheVals != null) {
			cacheSegment.put(this.cacheSegmentNum, this.cacheVals);
		}
		if (this.secondCacheVals != null) {
			cacheSegment.put(this.secondSegmentNum, this.secondCacheVals);
		}
		return new IndexIterator<>(target, index, defaultValue, i, cacheSegment);
	}

	@Override
	public Iterator<E> iterator() {
		return createIndexIterator(0);
	}

	private E[] getSegmentBySegmentNumber(int segmentNum) {
		if (this.cacheSegmentNum == segmentNum) {
			return this.cacheVals;
		} else {
			loadCacheOrNewSegmentBySegmentNum(segmentNum);
			return this.cacheVals;
		}
	}

	@SuppressWarnings("unchecked")
	private void updateCacheValSize() {
		E[] tmpVal;
		int newSize;
		int copyLength;
		if (this.cacheVals != null) {
			// update cache size
			newSize = index.getItemPerSegmentSize(cacheSegmentNum);
			if (newSize != cacheVals.length) {
				tmpVal = (E[]) Array.newInstance(target, newSize);
				copyLength = Math.min(newSize, cacheVals.length);
				if (copyLength > 0) {
					System.arraycopy(cacheVals, 0, tmpVal, 0, copyLength);
				}
				this.cacheVals = tmpVal;
			}
		}
		if (this.secondCacheVals != null) {
			newSize = index.getItemPerSegmentSize(secondSegmentNum);
			if (newSize != secondCacheVals.length) {
				tmpVal = (E[]) Array.newInstance(target, newSize);
				copyLength = Math.min(newSize, secondCacheVals.length);
				if (copyLength > 0) {
					System.arraycopy(secondCacheVals, 0, tmpVal, 0, Math.min(secondCacheVals.length, tmpVal.length));
				}
				this.secondCacheVals = tmpVal;
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void resize(int size) {
		if (size < 0) {
			throw new ArrayIndexOutOfBoundsException(size);
		} else if (this.size < size) {
			increaseSize(size);
			if (index.getAllocateSize() < size) {
				increaseAllocateSize(size);
			}
			updateCacheValSize();
		} else if (size < this.size) {
			int tmpSegmentNum = this.segmentSize - 1;

			decreaseSize(size);
			decreaseAllocateSize(size);

			// reduce size
			if (this.segmentSize <= this.secondSegmentNum) {
				this.secondSegmentNum = -1;
				this.secondCacheVals = null;
			}
			if (this.segmentSize <= this.cacheSegmentNum) {
				this.cacheSegmentNum = this.secondSegmentNum;
				this.cacheOffset = this.secondCacheOffset;
				this.cacheVals = this.secondCacheVals;
				this.isUpdatedCache = this.isUpdatedSecondCache;
			}

			// delete unuse segment
			while (tmpSegmentNum >= this.segmentSize) {
				index.deleteSegment(tmpSegmentNum--);
			}

			updateCacheValSize();

			// reduce size and fill null
			if (this.cacheVals != null && size < this.cacheOffset + this.cacheVals.length) {
				Arrays.fill(cacheVals, size - this.cacheOffset, cacheVals.length, null);
			}

			if (this.secondCacheVals != null && size < this.secondCacheOffset + this.secondCacheVals.length) {
				Arrays.fill(secondCacheVals, size - this.secondCacheOffset, secondCacheVals.length, null);
			}
        }
	}

	@Override
	public int compress() {
		int oldSize = size;
		int lastSegmentNum = this.segmentSize - 1;
		int lastOffset = index.getOffset(lastSegmentNum);
		E[] lastVal = getSegmentBySegmentNumber(lastSegmentNum);
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

				if (lastSegmentNum < this.cacheSegmentNum) {
					this.cacheSegmentNum = -1;
					this.cacheVals = null;
				}
				if (lastSegmentNum < this.secondSegmentNum) {
					this.secondSegmentNum = -1;
					this.secondCacheVals = null;
				}
			} else {
				break;
			}
		}
		i++;
		if (0 <= valIndex && valIndex < lastVal.length - 1 && i != size) {
			if (this.cacheSegmentNum == lastSegmentNum) {
				this.isUpdatedCache = true;
			} else if(this.secondSegmentNum == lastSegmentNum) {
				this.isUpdatedSecondCache = true;
			}
			decreaseSize(i);
			decreaseAllocateSize(i);
		}
		return oldSize - size;
	}

	@Override
	public int size() {
		return size;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void load(SeparableIndex<E> index) throws IOException, ClassNotFoundException {
		// 全部のindexが保存されていないのでバグあり

		this.secondSegmentNum = -1;
		this.secondCacheVals = null;
		this.isUpdatedSecondCache = false;
		this.cacheOffset = 0;
		this.cacheSegmentNum = 0;

		int size = index.getItemSize();
		if (this.size < size) {
			increaseSize(size);
		} else if(size < this.size) {
			decreaseSize(size);
		}

		IndexIterator<E> iterator = new IndexIterator<>(target, index, defaultValue);
		if (this.segmentSize > 1) {
			this.index.save(iterator, size, target);
			this.cacheVals = this.index.loadSegment(this.cacheSegmentNum, target);
			this.isUpdatedCache = false;
		} else {
			// load cache only, not save at time
			this.cacheVals = (E[]) Array.newInstance(target, index.getItemPerSegmentSize(cacheSegmentNum));
			while (iterator.hasNext()) {
				this.cacheVals[iterator.index()] = iterator.next();
			}
			this.isUpdatedCache = true;
		}
	}

	@Override
	public void save(SeparableIndex<E> index) throws IOException {
		index.save(this.iterator(), this.size(), target);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object[] toArray() {
		E[] obj = (E[]) Array.newInstance(target, size);
		Iterator<E> ite = this.iterator();
		int i = 0;
		while (ite.hasNext()) {
			obj[i++] = ite.next();
		}
		return obj;
	}

	private void prepareAddition() {
		int oldSize = size;
		int oldSegmentSize = segmentSize;
		// 仮でサイズを更新してsegmentSizeの変化を見る
		increaseSize(size + 1);
		if (oldSegmentSize == segmentSize) {
			E[] vals = getSegmentBySegmentNumber(segmentSize - 1);
			if (vals.length == oldSize - this.cacheOffset) {
				// 最後のoffsetの場合、効率化のために余分にサイズを拡大
				increaseAllocateSize(oldSize + vals.length);
				updateCacheValSize();
			}
		} else {
			if (index.getAllocateSize() == size) {
				// 新しいsegmentができた場合、1つしかサイズが確保できていない場合は作り直し
				decreaseSize(oldSize);
				decreaseAllocateSize(oldSize);
				increaseAllocateSize(oldSize + index.getItemPerSegmentSize(segmentSize - 1));
				increaseSize(oldSize + 1);
			}
			getSegmentBySegmentNumber(segmentSize - 1);
			updateCacheValSize();
		}
	}

	@Override
	public boolean add(E t) {
		int oldSize = size;
		prepareAddition();
		this.set(oldSize, t);
		return true;
	}

	@Override
	public void add(int i, E element) {
		if (i == size) {
			add(element);
		} else if(i < 0 && size < i) {
			throw new ArrayIndexOutOfBoundsException(i);
		} else {
			try {
				prepareAddition();
				// lastVal
				E[] tmpVals = this.cacheVals;
				int tmpSegmentNum = this.cacheSegmentNum;

				int tmpi;
				while ((tmpi = i - this.cacheOffset) < 0) {
					E[] targetVals = tmpVals;
					System.arraycopy(tmpVals, 0, tmpVals, 1, tmpVals.length - 1);
					tmpSegmentNum--;
					tmpVals = getSegmentBySegmentNumber(tmpSegmentNum);
					targetVals[0] = tmpVals[tmpVals.length - 1];
					// targetVals is secondCache
					saveSecondCacheSegment();
				}
				System.arraycopy(tmpVals, tmpi, tmpVals, tmpi + 1, tmpVals.length - tmpi - 1);
				tmpVals[tmpi] = element;
				this.isUpdatedCache = true;
			} catch (IOException e) {
				throw new DiskArrayListIOException(e);
			}
		}
	}

	@Override
	public E remove(int i) {
		E ret;
		if (i < 0 && size <= i) {
			throw new ArrayIndexOutOfBoundsException(i);
		} else {
			try {
				int tmpSegmentNum = this.segmentSize - 1;
				E[] tmpVal = getSegmentBySegmentNumber(tmpSegmentNum);
				E tmpItem = null;
				int lastIndex;
				int tmpi;
				while ((tmpi = i - this.cacheOffset) < 0) {
					lastIndex = tmpVal.length - 1;
					E firstItem = tmpVal[0];
					System.arraycopy(tmpVal, 1, tmpVal, 0, lastIndex);
					tmpVal[lastIndex] = tmpItem;
					tmpItem = firstItem;
					tmpSegmentNum--;
					tmpVal = getSegmentBySegmentNumber(tmpSegmentNum);
					// tmpVal is SecondCache in tmpVal[lastIndex]
					saveSecondCacheSegment();
				}
				ret = tmpVal[tmpi];
				System.arraycopy(tmpVal, tmpi + 1, tmpVal, tmpi, tmpVal.length - tmpi - 1);
				tmpVal[tmpVal.length - 1] = tmpItem;

				decreaseSize(size - 1);
				this.isUpdatedCache = true;
			} catch (IOException e) {
				throw new DiskArrayListIOException(e);
			}
		}
		return ret;
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		for (E item : c) {
			add(item);
		}
		return true;
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		int mvSize = size - index;
		int colSize = c.size();
		this.resize(size + colSize);
		for (int i = 0, j = size - 1; i < mvSize; i++, j--) {
			this.set(j, get(j - colSize));
		}
		for (E item : c) {
			this.set(index++, item);
		}
		return true;
	}

	@Override
	public boolean addAll(E[] val) {
		for (E item : val) {
			add(item);
		}
		return true;
	}

	@Override
	public boolean addAll(int index, E[] val) {
		int mvSize = size - index;
		int colSize = val.length;
		this.resize(size + colSize);
		for (int i = 0, j = size - 1; i < mvSize; i++, j--) {
			this.set(j, get(j - colSize));
		}
		for (E item : val) {
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
			E val = this.get(i);
			if (c.contains(val) == complement) {
				this.set(w++, val);
			}
		}
		if (w != size) {
			size = w;
			decreaseSize(size);
			decreaseAllocateSize(size);
			modified = true;
		}
		return modified;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void clear() {
		this.secondCacheOffset = -1;
		this.secondCacheVals = null;
		this.isUpdatedSecondCache = false;
		this.isUpdatedCache = true;
		this.isUpdatedInfo = true;

		if (this.segmentSize == 1) {
			Arrays.fill(cacheVals, null);
		} else {
			try {
				this.cacheSegmentNum = 0;
				this.cacheOffset = 0;
				int valSize = index.getItemPerSegmentSize(cacheSegmentNum);
				this.cacheVals = (E[]) Array.newInstance(target, valSize);
				index.saveSegment(cacheSegmentNum, cacheVals, 0);
				for (int i = 1; i < segmentSize; i++) {
					valSize = index.getItemPerSegmentSize(i);
					E[] val = (E[]) Array.newInstance(target, valSize);
					index.saveSegment(i, val, 0);
				}
			} catch (IOException e) {
				throw new DiskArrayListIOException(e);
			}
		}
	}

	@Override
	public int indexOf(Object o) {
		return super.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		return super.lastIndexOf(o);
	}

	@Override
	public ListIterator<E> listIterator() {
		return createIndexIterator(0);
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		return createIndexIterator(index);
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		return super.subList(fromIndex, toIndex);
	}

}
