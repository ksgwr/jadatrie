package jp.ksgwr.array;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
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

	public DiskArrayList(Class<T> target) {
		super(target);
		doOnExit();
	}

	@SuppressWarnings("unchecked")
	public DiskArrayList(Class<T> target, File directory, int size) throws IOException {
		super(target);
		this.cacheOffset = -1;
		this.offset = 0;
		this.size = size;
		this.index = new Index<T>(directory);
		this.vals = (T[]) Array.newInstance(target, size);
		directory.mkdirs();
		index.setSize(size);
		index.setSplitSize(splitSize);
		index.saveInfo();
		index.saveSegment(offset, target, vals, size);
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
		index.setSize(size);
		index.setSplitSize(splitSize);
		index.saveInfo();
		this.offset = ( size / splitSize ) * splitSize;
		this.vals = (T[]) Array.newInstance(target, size - offset);
		index.saveSegment(offset, target, vals, size - offset);
		for (int seg = size / splitSize; seg > 0; seg--) {
			this.offset -= splitSize;
			this.vals = (T[]) Array.newInstance(target, splitSize);
			index.saveSegment(offset, target, this.vals, splitSize);
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
						index.saveInfo();
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}

	public void saveInfo() throws IOException {
		index.saveInfo();
		isUpdatedInfo = false;
	}

	public void saveIfUpdated() throws IOException {
		if (isUpdated) {
			index.saveSegment(offset, target, vals, vals.length);
			isUpdated = false;
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
		if (splitSize > 0) {
			try {
				saveIfUpdated();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
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
			int valSize = size % splitSize;
			int offset = (size / splitSize) * splitSize;
			T[] newVal;
			T[] oldVal;
			if (this.size < size) {
				// 領域拡大
				if (!index.isExistSegment(offset)) {
					// 末尾
					newVal = (T[]) Array.newInstance(target, valSize);
					index.saveSegment(offset, target, newVal, newVal.length);

					// 中間ファイル
					offset -= splitSize;
					while (!index.isExistSegment(offset)) {
						newVal = (T[]) Array.newInstance(target, splitSize);
						index.saveSegment(offset, target, newVal, newVal.length);
						offset -= splitSize;
					}

					valSize = splitSize;
				}
				oldVal = getSegment(offset);
				newVal = (T[]) Array.newInstance(target, splitSize);
				System.arraycopy(oldVal, 0, newVal, 0, oldVal.length);
				index.saveSegment(offset, target, newVal, newVal.length);
				updateCache(offset, newVal);
			} else {
				// 領域縮小
				oldVal = getSegment(offset);
				newVal = (T[]) Array.newInstance(target, valSize);
				System.arraycopy(oldVal, 0, newVal, 0, newVal.length);
				index.saveSegment(offset, target, newVal, newVal.length);
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
			int lastOffset = (size / splitSize) * splitSize;
			T[] lastVal = getSegment(lastOffset);
			int i = size - 1;
			while (i >= 0) {
				int valIndex = i - lastOffset;
				while(valIndex > 0 && lastVal[valIndex] == null) {
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
			int lastOffset = (size / splitSize) * splitSize;
			int valSize = (size + 1) % splitSize;
			T[] newVal;
			size++;
			if (valSize == 0) {
				// 新領域の拡大
				newVal = (T[]) Array.newInstance(target, splitSize);
				newVal[0] = t;

				lastOffset += splitSize;

				saveInfo();
			} else {
				newVal = getSegment(lastOffset);
				newVal[valSize] = t;

				// 効率性のためsaveInfoしない
				isUpdatedInfo = true;
			}
			saveIfUpdated();
			this.cacheOffset = this.offset;
			this.cacheVals = this.vals;

			this.offset = lastOffset;
			this.vals = newVal;

			isUpdated = true;
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return true;
	}

	@Override
	public boolean remove(Object o) {
		// TODO 自動生成されたメソッド・スタブ
		return false;
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
		return true;
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

	@Override
	public void clear() {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	public void add(int index, T element) {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	public T remove(int index) {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	@Override
	public int indexOf(Object o) {
		// TODO 自動生成されたメソッド・スタブ
		return 0;
	}

	@Override
	public int lastIndexOf(Object o) {
		// TODO 自動生成されたメソッド・スタブ
		return 0;
	}

	@Override
	public ListIterator<T> listIterator() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	@Override
	public ListIterator<T> listIterator(int index) {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	@Override
	public List<T> subList(int fromIndex, int toIndex) {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

}