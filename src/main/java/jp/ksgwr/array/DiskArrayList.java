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
	
	private int allocateSize;

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
		this.cacheOffset = -1;
		
		this.offset = 0;
		this.segmentNum = 0;
		this.size = size;
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
		this.cacheOffset = -1;
		
		this.offset = 0;
		this.segmentNum = 0;
		this.vals = index.loadSegment(segmentNum, target);
				
		this.index = index;
		this.size = index.getItemSize();
		this.segmentSize = index.getSegmentSize();
	}

	private final void updateSize(int size) throws IOException {
		this.allocateSize = size;
		index.updateItemSize(size);
		this.segmentSize = index.getSegmentSize();
	}
	
	@Override
	public void close() throws IOException {
		saveIfUpdated();
		if (isUpdatedInfo) {
			updateSize(size);
		}
	}

	public void saveIfUpdated() throws IOException {
		if (isUpdated) {
			index.saveSegment(segmentNum, vals);
			isUpdated = false;
		}
	}

	private final void initForAllScan() {
		if (this.segmentSize > 1) {
			try {
				saveIfUpdated();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private void loadCacheOrNewSegment(int i) {
		try {
			int tmpOffset = this.offset;
			int tmpSegmentNum = this.segmentNum;
			saveIfUpdated();
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

	private T[] getSegment(int offset) throws ClassNotFoundException, IOException {
		if (this.offset == offset) {
			return this.vals;
		} else if(this.cacheOffset == offset) {
			return this.cacheVals;
		} else {
			int tmpSegmentNum = index.getSegmentNumber(offset);
			return index.loadSegment(tmpSegmentNum, target);
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
	
	private void updateCache(int offset, int segmentNum, T[] newVal) {
		if (this.offset == offset) {
			this.vals = newVal;
			this.segmentNum = segmentNum;
		} else if(this.cacheOffset == offset) {
			this.cacheVals = newVal;
			this.cacheSegmentNum = segmentNum;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void resize(int size) {
		try {
			int valSize;
			T[] newVal;
			T[] oldVal;
			
			int oldSegmentSize = this.segmentSize;
			// may be change getItemPerSegmentSize
			updateSize(size);
			int tmpSegmentNum = this.segmentSize - 1;
			
			if (this.size < size) {
				// 領域拡大
				if (oldSegmentSize <= tmpSegmentNum) {
					// 末尾
					valSize = index.getItemPerSegmentSize(tmpSegmentNum);
					newVal = (T[]) Array.newInstance(target, valSize);
					index.saveSegment(tmpSegmentNum, newVal);

					// 中間ファイル
					tmpSegmentNum--;
					while (oldSegmentSize <= tmpSegmentNum) {
						valSize = index.getItemPerSegmentSize(tmpSegmentNum);
						newVal = (T[]) Array.newInstance(target, valSize);
						index.saveSegment(tmpSegmentNum, newVal);
						tmpSegmentNum--;
					}

				}
				oldVal = getSegmentBySegmentNumber(tmpSegmentNum);
				valSize = index.getItemPerSegmentSize(tmpSegmentNum);
				newVal = (T[]) Array.newInstance(target, valSize);
				System.arraycopy(oldVal, 0, newVal, 0, oldVal.length);
				index.saveSegment(tmpSegmentNum, newVal);
				updateCache(offset, tmpSegmentNum, newVal);
			} else {
				// 領域縮小
				oldVal = getSegmentBySegmentNumber(oldSegmentSize - 1);
				valSize = index.getItemPerSegmentSize(tmpSegmentNum);
				newVal = (T[]) Array.newInstance(target, valSize);
				System.arraycopy(oldVal, 0, newVal, 0, newVal.length);
				index.saveSegment(tmpSegmentNum, newVal);
				updateCache(offset, tmpSegmentNum, newVal);
				int tmpOffset = index.getOffset(tmpSegmentNum);
				if (tmpSegmentNum < this.segmentNum) {
					this.offset = tmpOffset;
					this.segmentNum = tmpSegmentNum;
					this.vals = newVal;
				}
				if (tmpSegmentNum < this.cacheSegmentNum) {
					this.cacheOffset = tmpOffset;
					this.cacheSegmentNum = tmpSegmentNum;
					this.cacheVals = newVal;
				}

				tmpSegmentNum++;
				while (tmpSegmentNum < oldSegmentSize) {
					index.deleteSegment(tmpSegmentNum);
					tmpSegmentNum++;
				}
			}
			this.size = size; 
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
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

			while (i >= 0) {
				int valIndex = i - lastOffset;
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
			if (this.size != i) {
				this.size = i;
				this.segmentSize = lastSegmentNum + 1;
				this.isUpdatedInfo = true;
			}
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
		
		this.size = index.getItemSize();
		this.allocateSize = this.size;
		this.segmentSize = index.getSegmentSize();
		this.isUpdatedInfo = true;
		IndexIterator<T> iterator = new IndexIterator<>(target, index);
		if (this.segmentSize > 1) {
			this.index.save(iterator, size, target);
			this.vals = this.index.loadSegment(this.segmentNum, target);
			this.isUpdated = false;			
		} else {
			// load cache only, not save at time
			this.vals = (T[]) Array.newInstance(target, size);
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

	@SuppressWarnings("unchecked")
	@Override
	public boolean add(T t) {
		try {
			if (allocateSize <= size) {
				int oldSegmentSize = this.segmentSize;
				int lastValSize = index.getItemPerSegmentSize(oldSegmentSize - 1);
				updateSize(size + lastValSize);
				if (oldSegmentSize == segmentSize) {
					// valsのサイズ拡大の場合、効率化のためキャッシュを使いまわす
					T[] newVal;
					int newOffset = -1;
					int newSegmentNum = this.segmentSize - 1;
					T[] tmpVal = null;
					if (newSegmentNum == this.segmentNum) {
						tmpVal = vals;
						newOffset = offset;
					} else if (newSegmentNum == this.cacheSegmentNum) {
						tmpVal = cacheVals;
						newOffset = cacheOffset;
					}
					if (tmpVal != null) {
						// Indexから読み込むのでなく既存キャッシュを使って高速化
						int newValSize = index.getItemPerSegmentSize(newSegmentNum);
						newVal = (T[]) Array.newInstance(target, newValSize);
						System.arraycopy(tmpVal, 0, newVal, 0, tmpVal.length);
						updateCache(newOffset, newSegmentNum, newVal);
					}
				}
			}
			this.set(size, t);
			size++;
			isUpdatedInfo = true;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		// TODO 自動生成されたメソッド・スタブ
		return false;
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		// TODO 自動生成されたメソッド・スタブ
		return false;
	}

	@Override
	public boolean addAll(T[] val) {
		return false;
	}

	@Override
	public boolean addAll(int index, T[] val) {
		return false;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		// TODO 自動生成されたメソッド・スタブ
		return false;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		// TODO 自動生成されたメソッド・スタブ
		return false;
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

	@SuppressWarnings("unchecked")
	@Override
	public void add(int i, T element) {
		if (i == size) {
			add(element);
		} else if(i < 0 && size < i) {
			throw new ArrayIndexOutOfBoundsException(i);
		} else {
			
		}
		/*
		if (i == size) {
			add(element);
		} else if(i < 0 && size < i) {
			throw new ArrayIndexOutOfBoundsException(i);
		} else {
			try {
				if (splitSize == Integer.MAX_VALUE) {
					T[] newVal;
					if (size == vals.length) {
						newVal = (T[]) Array.newInstance(target, vals.length + vals.length);
						System.arraycopy(vals, 0, newVal, 0, i);
					} else {
						newVal = this.vals;
					}
					// TODO: 未使用領域のコピーは省略可すれば効率化できる
					System.arraycopy(vals, i, newVal, i + 1, vals.length - i);
					newVal[i] = element;
					vals = newVal;

				} else {
					this.saveIfUpdated();
					// TODO: tmpでなくcacheとして更新した方が効率が良い？
					// cacheの中身も変更を反映させる必要があり、 cacheが使いまわせない
					int tmpOffset = ( (size - 1) / splitSize ) * splitSize;
					T[] tmpVal = null;
					int offset;
					T[] val;

					// tmpi計算

					if ( size % splitSize == 0) {
						// size拡張判断
						tmpOffset += splitSize;
						tmpVal = (T[]) Array.newInstance(target, splitSize);
						index.saveSegment(tmpOffset, target, tmpVal);
					} else {
						tmpVal = index.loadSegment(tmpOffset, target);
					}

					int tmpi = i - tmpOffset;
					while (tmpi < 0) {
						offset = tmpOffset;
						val = tmpVal;
						System.arraycopy(val, 0, val, 1, val.length - 1);
						tmpOffset -= splitSize;
						tmpVal = index.loadSegment(tmpOffset, target);
						val[0] = tmpVal[tmpVal.length - 1];
						index.saveSegment(offset, target, val);
						tmpi = i - tmpOffset;
					}
					System.arraycopy(tmpVal, tmpi, tmpVal, tmpi + 1, tmpVal.length - tmpi - 1);
					tmpVal[tmpi] = element;
					this.cacheOffset = -1;
					this.cacheVals = null;
					this.offset = tmpOffset;
					this.vals = tmpVal;
				}

				this.index.setSize(++size);
				isUpdatedInfo = true;
				isUpdated = true;

			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}

		}

	  */
	}

	@Override
	public T remove(int i) {
		/*
		T ret = null;
		if(i < 0 && size <= i) {
			throw new ArrayIndexOutOfBoundsException(i);
		} else if (splitSize == Integer.MAX_VALUE){
			ret = vals[i];
			System.arraycopy(vals, i + 1, vals, i, vals.length - i - 1);
		} else {
			try {
				this.saveIfUpdated();
				int tmpOffset = ( (size - 1) / splitSize ) * splitSize;
				T[] tmpVal = index.loadSegment(tmpOffset, target);
				int tmpi = i - tmpOffset;
				T tmpItem = null;
				int lastIndex;
				while (tmpi < 0) {
					// TODO: tmpVal.length - 1は一定値でsplitSizeに置換できる
					lastIndex = tmpVal.length - 1;
					T firstItem = tmpVal[0];
					System.arraycopy(tmpVal, 1, tmpVal, 0, lastIndex);
					tmpVal[lastIndex] = tmpItem;
					tmpItem = firstItem;
					index.saveSegment(tmpOffset, target, tmpVal);
					tmpOffset -= splitSize;
					tmpVal = index.loadSegment(tmpOffset, target);
					tmpi += tmpVal.length;
				}
				ret = tmpVal[tmpi];
				System.arraycopy(tmpVal, tmpi + 1, tmpVal, tmpi, tmpVal.length - tmpi - 1);
				tmpVal[tmpVal.length - 1] = tmpItem;
				this.cacheOffset = -1;
				this.cacheVals = null;
				this.offset = tmpOffset;
				this.vals = tmpVal;
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}

		}
		this.index.setSize(--size);
		this.isUpdatedInfo = true;
		this.isUpdated = true;
		return ret;
		*/
		return null;
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
		return new IndexIterator<T>(target, this.index, vals, index);
	}

	@Override
	public List<T> subList(int fromIndex, int toIndex) {
		initForAllScan();
		return super.subList(fromIndex, toIndex);
	}

}
