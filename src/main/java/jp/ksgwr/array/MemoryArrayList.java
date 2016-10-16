package jp.ksgwr.array;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;

/**
 * Memory Array List
 *
 * @author ksgwr
 *
 * @param <T> item class
 */
public class MemoryArrayList<T extends Serializable> extends ExArrayList<T> {

	/** array list */
	private ArrayList<T[]> vals;

	/** array index */
	private int valIndex;

	/** offset index */
	private int offset;

	/** data size */
	private int size;

	/** capacity size */
	private int allocateSize;

	/** last value offset (for efficiency "add" ) */
	private int lastOffset;

	/** current value */
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
			for (valIndex--; valIndex >= 0; valIndex--) {
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
			// TODO: 未使用領域にアクセスする可能性はある
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
			// size縮小の場合は縮小領域は初期化しない (領域が消える訳でなく意味が薄いため)
		}
		this.size = size;
	}

	/**
	 * compressすることでT[] valのみ使いMTセーフになる
	 */
	@SuppressWarnings("unchecked")
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
			} else {
				break;
			}
		}

		if (vals.size() > 1 || this.allocateSize != this.size) {
			T[] newVal = (T[]) Array.newInstance(target, size);
			int offset = 0;
			for (int i=0,valsSize=vals.size();i<valsSize;i++) {
				T[] v = vals.get(i);
				if (i == valsSize - 1) {
					System.arraycopy(val, 0, newVal, offset, size - offset);
				} else {
					System.arraycopy(val, 0, newVal, offset, v.length);
					offset += v.length;
				}
			}
			this.val = newVal;
			this.vals.clear();
			this.vals.add(newVal);
			this.allocateSize = size;
		} else {
			this.val = lastVal;
		}
		this.offset = 0;
		this.lastOffset = 0;
		this.valIndex = 0;
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
			this.allocateSize += val.length;
			this.lastOffset += lastVal.length;
		} else {
			T[] lastVal = vals.get(vals.size() - 1);
			int lastIndex = size - lastOffset;
			size++;
			lastVal[lastIndex] = e;
		}
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		T[] val = c.toArray(this.val);
		return addAll(val);
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		T[] val = c.toArray(this.val);
		int tmpi;
		do {
			tmpi = index - this.lastOffset;
		} while (tmpi >= 0);
		
		//境界条件、そのまま追加か分割が発生するか
		if (tmpi == 0) {
			// 違う?
		} else {
			
		}
		
		this.size += this.val.length;
		return true;
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
		this.val = vals.get(0);
		if (vals.size() > 1) {
			this.vals.clear();
			this.vals.add(val);
		}
		this.allocateSize = val.length;
		this.size = val.length;
		this.offset = 0;
		this.lastOffset = 0;
		Arrays.fill(val, null);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void add(int index, T element) {
		int tmpValIndex = vals.size() - 1;
		T[] tmpVal = vals.get(valIndex);
		T[] val;
		if (this.size == this.allocateSize) {
			// 最後の配列と同じサイズだけ拡張する
			val = (T[]) Array.newInstance(target, tmpVal.length);
			this.vals.add(val);
			this.allocateSize += val.length;
			this.lastOffset += tmpVal.length;
			tmpVal = val;
			tmpValIndex++;
		}

		if(index == size) {
			tmpVal[index - lastOffset] = element;
		} else if (index < 0 && size < index) {
			throw new ArrayIndexOutOfBoundsException(index);
		} else {
			int tmpi = index - lastOffset;
			while(0 <= tmpValIndex) {
				if (tmpi < 0) {
					val = tmpVal;
					System.arraycopy(val, 0, val, 1, val.length - 1);
					tmpVal = vals.get(--tmpValIndex);
					val[0] = tmpVal[tmpVal.length - 1];
					tmpi += tmpVal.length;
				} else {
					System.arraycopy(tmpVal, tmpi, tmpVal, tmpi + 1,
							tmpVal.length - tmpi - 1);
					tmpVal[tmpi] = element;
					break;
				}
			}
		}
		this.size++;
	}

	@Override
	public T remove(int index) {
		int tmpValIndex = vals.size() - 1;
		T[] tmpVal = vals.get(valIndex);
		T tmpItem = null;
		T ret = null;
		if (index < 0 && size <= index) {
			throw new ArrayIndexOutOfBoundsException(index);
		} else {
			int tmpi = index - lastOffset;
			while(0 <= tmpValIndex) {
				int lastIndex = tmpVal.length - 1;
				if (tmpi < 0) {
					T firstItem = tmpVal[0];
					System.arraycopy(tmpVal, 1, tmpVal, 0, lastIndex);
					tmpVal[lastIndex] = tmpItem;
					tmpItem = firstItem;
					tmpi += tmpVal.length;
					tmpVal = vals.get(--tmpValIndex);
				} else {
					ret = tmpVal[tmpi];
					System.arraycopy(tmpVal, tmpi + 1, tmpVal, tmpi,
							tmpVal.length - tmpi - 1);
					tmpVal[lastIndex] = tmpItem;
					break;
				}
			}
		}
		this.size--;
		return ret;
	}

	@Override
	public ListIterator<T> listIterator() {
		return new ListArrayIterator<T>(vals, size);
	}

	@Override
	public ListIterator<T> listIterator(int index) {
		return new ListArrayIterator<T>(vals, size, index);
	}




}
