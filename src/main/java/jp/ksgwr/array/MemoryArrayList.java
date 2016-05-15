package jp.ksgwr.array;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;


public class MemoryArrayList<T extends Serializable> extends ExArrayList<T> {

	private ArrayList<T[]> vals;

	private int valIndex;

	private int offset;

	private int size;

	private int allocateSize;

	private int lastOffset;

	private T[] val;

	public MemoryArrayList(Class<T> target) {
		super(target);
	}

	public MemoryArrayList(Class<T> target, int size) {
		this(target, size, 10);
	}

	@SuppressWarnings("unchecked")
	public MemoryArrayList(Class<T> target, int size, int resizeCapacity) {
		super(target);
		this.offset = 0;
		this.valIndex = 0;
		this.size = size;
		this.allocateSize = size;
		this.lastOffset = 0;
		this.val = (T[]) Array.newInstance(target, size);
		this.vals = new ArrayList<T[]>(resizeCapacity);
		vals.add(this.val);
	}

	private int searchRelativeIndex(int relativePosition, int absolutePosition) {
		if (relativePosition < 0) {
			for (valIndex--; valIndex > 0; valIndex--) {
				this.val = vals.get(valIndex);
				this.offset -= val.length;
				relativePosition = absolutePosition - offset;
				if (relativePosition < val.length) {
					break;
				}
			}
		} else {
			for (valIndex++; valIndex < vals.size(); valIndex++) {
				this.offset += val.length;
				this.val = vals.get(valIndex);
				relativePosition = absolutePosition - offset;
				if (relativePosition < val.length) {
					break;
				}
			}
		}
		return relativePosition;
	}

	@Override
	public T get(int i) {
		int tmpi = i - offset;
		try {
			// 未使用領域にアクセスする可能性はある
			return this.val[tmpi];
		} catch (ArrayIndexOutOfBoundsException e) {
			tmpi = searchRelativeIndex(tmpi, i);
			return this.val[tmpi];
		}
	}

	@Override
	public T set(int i, T t) {
		int tmpi = i - offset;
		try {
			return this.val[tmpi] = t;
		} catch (ArrayIndexOutOfBoundsException e) {
			tmpi = searchRelativeIndex(tmpi, i);
			return this.val[tmpi] = t;
		}
	}

	@Override
	public Iterator<T> iterator() {
		return this.listIterator();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void resize(int size) {
		if (this.size < size) {
			// 領域を拡大する場合
			T[] lastVal = vals.get(vals.size() - 1);
			// 未使用領域の初期化
			if (this.size < this.allocateSize) {
				for (int i = this.size; i < this.allocateSize; i++) {
					lastVal[i - lastOffset] = null;
				}
			}
			// 新領域の確保
			if (this.allocateSize < size) {
				T[] val = (T[]) Array.newInstance(target, size - this.allocateSize);
				this.vals.add(val);
				this.allocateSize = size;
				this.lastOffset = lastOffset + lastVal.length;
			}
		} else {
			// 領域を縮小する場合
			// lastOffsetの更新,未使用リストの削除
			while (size < lastOffset) {
				int lastIndex = vals.size() - 1;
				T[] delVal = vals.remove(lastIndex--);
				T[] lastVal = vals.get(lastIndex);
				this.lastOffset -= lastVal.length;
				this.allocateSize -=  delVal.length;
				if (this.val == delVal) {
					this.val = lastVal;
					this.offset = this.lastOffset;
					this.valIndex = lastIndex;
				}
			}
			// size縮小の場合は縮小領域は初期化しない
		}
		this.size = size;
	}

	@Override
	public void compress() {
		int lastIndex = vals.size() - 1;
		T[] lastVal = vals.get(lastIndex);
		while (lastIndex >= 0) {
			int valIndex;
			if (this.size == this.allocateSize) {
				valIndex = lastVal.length - 1;
			} else {
				valIndex = size - lastOffset;
			}
			while (valIndex >= 0 && lastVal[valIndex] == null) {
				valIndex--;
				size--;
			}
			// 不要なリストがある場合、かつ0番目以外
			if (valIndex < 0 && lastIndex > 0) {
				T[] delVal = vals.remove(lastIndex--);
				lastVal = vals.get(lastIndex);
				this.lastOffset -= lastVal.length;
				this.allocateSize -= delVal.length;
				if (this.val == delVal) {
					this.val = lastVal;
					this.offset = this.lastOffset;
					this.valIndex = lastIndex;
				}
			} else {
				break;
			}
		}
	}

	@Override
	public int size() {
		return size;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void load(File directory) throws IOException, ClassNotFoundException {
		Index<T> index = new Index<T>(directory);
		index.loadInfo();

		this.val = (T[]) Array.newInstance(target, index.getSize());
		IndexIterator<T> iterator = new IndexIterator<T>(target, index, val);
		while(iterator.hasNext()) {
			val[iterator.index()] = iterator.next();
		}
		this.valIndex = 0;
		this.offset = 0;
		this.size = val.length;
		this.allocateSize = val.length;
		this.lastOffset = 0;
		this.vals = new ArrayList<T[]>();
		this.vals.add(val);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object[] toArray() {
		if (vals.size() == 1 && allocateSize == size) {
			return vals.get(0);
		}
		T[] obj = (T[]) Array.newInstance(target, size);
		int offset = 0;
		for (int i=0;i<vals.size();i++) {
			T[] val = vals.get(i);
			int copySize = i == vals.size() - 1 ? size - offset : val.length;
			System.arraycopy(val, 0, obj, offset, copySize);
			offset += val.length;
		}
		return obj;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean add(T e) {
		if (size == allocateSize) {
			// 最後の配列と同じサイズだけ拡張する
			T[] lastVal = vals.get(vals.size() - 1);
			T[] val = (T[]) Array.newInstance(target, lastVal.length);
			val[0] = e;
			this.vals.add(val);
			size++;
			this.allocateSize += lastVal.length;
			this.lastOffset += lastVal.length;
		} else {
			T[] lastVal = vals.get(vals.size() - 1);
			lastVal[size++] = e;
		}
		return true;
	}

	@Override
	public boolean remove(Object o) {
		boolean flag = false;
		Iterator<T> ite = this.iterator();
		while (ite.hasNext()) {
			T t = ite.next();
			if (t == o) {
				ite.remove();
				flag = true;
			}
		}
		return flag;
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		T[] lastVal = vals.get(vals.size() - 1);
		T[] val = c.toArray(lastVal);
		return addAll(val);
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {

		return false;
	}


	@SuppressWarnings("unchecked")
	@Override
	public boolean addAll(T[] val) {
		T[] lastVal = vals.get(vals.size() - 1);
		if (size == allocateSize) {
			this.vals.add(val);
			this.size += val.length;
			this.allocateSize += val.length;
			this.lastOffset += lastVal.length;
		} else {
			int copySize;
			if (allocateSize - size < val.length) {
				copySize = allocateSize - size;
				int newSize = val.length - copySize;
				T[] newVal = (T[]) Array.newInstance(target, newSize);
				System.arraycopy(val, copySize, newVal, 0, newSize);
				this.vals.add(newVal);
				this.allocateSize += newSize;
				this.lastOffset += lastVal.length;
			} else {
				copySize = val.length;
			}
			System.arraycopy(val, 0, lastVal, size - lastOffset, copySize);
			this.size += this.val.length;
		}
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
		return new ListArrayIterator<T>(vals, size);
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
