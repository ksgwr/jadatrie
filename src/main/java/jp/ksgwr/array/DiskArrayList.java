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

public class DiskArrayList<T extends Serializable> extends ExArrayList<T> {

	private Index<T> index;

	private T[] vals;

	// 境界値付近の場合を備えて1つ前をcacheする
	private T[] cacheVals;

	private int cacheOffset;

	private int offset;

	private int splitSize;

	private int size;

	private boolean isUpdated;

	private boolean isUpdatedInfo;

	@SuppressWarnings("unchecked")
	public DiskArrayList(Class<T> target, File directory, int size) throws IOException {
		super(target);
		this.cacheOffset = -1;
		this.offset = 0;
		this.size = size;
		this.splitSize = Integer.MAX_VALUE;
		this.index = new Index<T>(directory);
		this.vals = (T[]) Array.newInstance(target, size);
		directory.mkdirs();
		index.cleanupSegment();
		index.setSize(size);
		index.setSplitSize(splitSize);
		index.saveInfo();
		index.saveSegment(offset, target, vals);
		doOnExit();
	}

	@SuppressWarnings("unchecked")
	public DiskArrayList(Class<T> target, File directory, int size, int splitSize) throws IOException {
		super(target);
		this.cacheOffset = -1;
		this.size = size;
		this.splitSize = splitSize;
		this.index = new Index<T>(directory);
		directory.mkdirs();
		index.cleanupSegment();
		index.setSize(size);
		index.setSplitSize(splitSize);
		index.saveInfo();
		this.offset = ( ( size - 1 ) / splitSize ) * splitSize;
		this.vals = (T[]) Array.newInstance(target, splitSize);
		index.saveSegment(offset, target, vals);
		for (int seg = (size - 1) / splitSize; seg > 0; seg--) {
			this.offset -= splitSize;
			index.saveSegment(offset, target, this.vals);
		}
		doOnExit();
	}

	private void doOnExit() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					saveIfUpdated();
					if (isUpdatedInfo) {
						index.setSize(size);
						index.saveInfo();
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}

	public void saveInfo() throws IOException {
		index.setSize(size);
		index.saveInfo();
		isUpdatedInfo = false;
	}

	public void saveIfUpdated() throws IOException {
		if (isUpdated) {
			index.saveSegment(offset, target, vals);
			isUpdated = false;
		}
	}

	private final void initForAllScan() {
		if (splitSize != Integer.MAX_VALUE) {
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
			saveIfUpdated();
			this.offset = (i / splitSize) * splitSize;
			if (this.offset == this.cacheOffset) {
				// swap cache and current value
				T[] tmpVals = this.cacheVals;
				this.cacheVals = this.vals;

				this.vals = tmpVals;
			} else {
				this.cacheVals = this.vals;

				this.vals = index.loadSegment(offset, target);
			}
			this.cacheOffset = tmpOffset;
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
		isUpdated = true;
		try {
			return vals[i - offset] = t;
		} catch (NullPointerException e) {
			loadCacheOrNewSegment(i);
			return vals[i - offset] = t;
		} catch (ArrayIndexOutOfBoundsException e) {
			loadCacheOrNewSegment(i);
			return vals[i - offset] = t;
		}
	}

	@Override
	public Iterator<T> iterator() {
		initForAllScan();
		return new IndexIterator<T>(target, index, vals);
	}

	private T[] getSegment(int offset) throws ClassNotFoundException, IOException {
		if (this.offset == offset) {
			return this.vals;
		} else if(this.cacheOffset == offset) {
			return this.cacheVals;
		} else {
			return index.loadSegment(offset, target);
		}
	}

	private void updateCache(int offset, T[] newVal) {
		if (this.offset == offset) {
			this.vals = newVal;
		} else if(this.cacheOffset == offset) {
			this.cacheVals = newVal;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void resize(int size) {
		try {
			int valSize;
			int offset = ( (size - 1) / splitSize) * splitSize;
			T[] newVal;
			T[] oldVal;
			if (this.size < size) {
				// 領域拡大
				if (!index.isExistSegment(offset)) {
					// 末尾
					newVal = (T[]) Array.newInstance(target, splitSize);
					index.saveSegment(offset, target, newVal);

					// 中間ファイル
					offset -= splitSize;
					while (!index.isExistSegment(offset)) {
						newVal = (T[]) Array.newInstance(target, splitSize);
						index.saveSegment(offset, target, newVal);
						offset -= splitSize;
					}

					valSize = splitSize;
				}
				valSize = splitSize == Integer.MAX_VALUE ? size : splitSize;
				oldVal = getSegment(offset);
				newVal = (T[]) Array.newInstance(target, valSize);
				System.arraycopy(oldVal, 0, newVal, 0, oldVal.length);
				index.saveSegment(offset, target, newVal);
				updateCache(offset, newVal);
			} else {
				// 領域縮小
				oldVal = getSegment(offset);
				valSize = splitSize == Integer.MAX_VALUE ? size : splitSize;
				newVal = (T[]) Array.newInstance(target, valSize);
				System.arraycopy(oldVal, 0, newVal, 0, newVal.length);
				index.saveSegment(offset, target, newVal);
				updateCache(offset, newVal);
				if (offset < this.offset) {
					this.offset = offset;
					this.vals = newVal;
				}
				if (offset < this.cacheOffset) {
					this.cacheOffset = offset;
					this.vals = newVal;
				}

				offset += splitSize;
				while (index.isExistSegment(offset)) {
					index.deleteSegment(offset);
					offset += splitSize;
				}
			}
			this.index.setSize(size);
			this.size = size;
			saveInfo();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void compress() {
		try {
			int lastOffset = ((size - 1) / splitSize) * splitSize;
			T[] lastVal = getSegment(lastOffset);
			int i = size - 1;
			while (i >= 0) {
				int valIndex = i - lastOffset;
				while(valIndex >= 0 && lastVal[valIndex] == null) {
					valIndex--;
					size--;
					i--;
				}
				if (valIndex < 0) {
					index.deleteSegment(lastOffset);
					lastOffset -= splitSize;
					lastVal = getSegment(lastOffset);
					if (lastOffset < this.offset) {
						this.offset = lastOffset;
						this.vals = lastVal;
					}
					if (lastOffset < this.cacheOffset) {
						this.cacheOffset = lastOffset;
						this.vals = lastVal;
					}
				} else {
					break;
				}
			}
			this.index.setSize(size);
			saveInfo();
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

	@Override
	public void load(File directory) throws IOException {
		this.index = new Index<T>(directory);
		index.loadInfo();

		this.vals = null;
		this.offset = -1;
		this.splitSize = index.getSplitSize();
		this.size = index.getSize();
		this.isUpdated = false;
		this.isUpdatedInfo = false;

		this.cacheOffset = -1;
		this.cacheVals = null;
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
			if (splitSize == Integer.MAX_VALUE) {
				if (vals.length == size) {
					T[] newVal = (T[]) Array.newInstance(target, vals.length + vals.length);
					System.arraycopy(vals, 0, newVal, 0, vals.length);
					vals = newVal;
				}
				vals[size] = t;
				size++;
			} else {
				int lastOffset = ((size - 1) / splitSize) * splitSize;
				int valSize = size % splitSize;
				T[] newVal;
				size++;
				if (valSize == 0) {
					// 新領域の拡大
					newVal = (T[]) Array.newInstance(target, splitSize);
					newVal[0] = t;

					lastOffset += splitSize;

					index.saveSegment(lastOffset, target, newVal);
				} else {
					newVal = getSegment(lastOffset);
					newVal[valSize] = t;
				}

				if (lastOffset != this.offset) {
					// cacheにヒットしない場合
					saveIfUpdated();
					this.cacheOffset = this.offset;
					this.cacheVals = this.vals;

					this.offset = lastOffset;
					this.vals = newVal;
				}
			}
			this.index.setSize(size);
			isUpdatedInfo = true;
			isUpdated = true;
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
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

		if (splitSize == Integer.MAX_VALUE) {
			this.offset = 0;
			Arrays.fill(vals, null);
		} else {
			try {
				index.cleanupSegment();
				this.offset = ( ( size - 1 ) / splitSize ) * splitSize;
				this.vals = (T[]) Array.newInstance(target, splitSize);
				index.saveSegment(offset, target, vals);
				for (int seg = (size - 1) / splitSize; seg > 0; seg--) {
					this.offset -= splitSize;
					index.saveSegment(offset, target, this.vals);
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


	}

	@Override
	public T remove(int i) {
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
		return new IndexIterator<T>(target, index, vals);
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
