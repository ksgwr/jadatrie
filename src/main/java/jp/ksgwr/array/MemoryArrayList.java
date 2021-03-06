package jp.ksgwr.array;

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
 * @param <E> item class
 */
public class MemoryArrayList<E> extends AbstractExArrayList<E> {

	/** array list */
	protected ArrayList<E[]> vals;

	/** data size */
	protected int size;

	/** capacity size */
	protected int allocateSize;

	/** last value offset (for efficiency "add" ) */
	protected int lastOffset;

	public MemoryArrayList(Class<E> target, int size) {
		this(target, null, size, 10);
	}

	/**
	 * constructor
	 * @param target target class
	 * @param size initial array size
	 */
	public MemoryArrayList(Class<E> target, E defaultValue, int size) {
		this(target, defaultValue, size, 10);
	}

	@SuppressWarnings("unchecked")
	public MemoryArrayList(Class<E> target, int size, int resizeCapacity) {
		this(target, null, (E[])Array.newInstance(target, size), resizeCapacity);
	}

	/**
	 * constructor
	 * @param target target class
	 * @param size initial array size
	 * @param resizeCapacity resize capacity size
	 */
	@SuppressWarnings("unchecked")
	public MemoryArrayList(Class<E> target, E defaultValue, int size, int resizeCapacity) {
		this(target, defaultValue, (E[])Array.newInstance(target, size), resizeCapacity);
	}

	public MemoryArrayList(Class<E> target, E[] val, int resizeCapacity) {
		this(target, null, val, resizeCapacity);
	}

	/**
	 * constructor
	 * @param target target class
	 * @param val initial val array
	 * @param resizeCapacity resize capacity size
	 */
	public MemoryArrayList(Class<E> target, E defaultValue, E[] val, int resizeCapacity) {
		super(target, defaultValue);
		this.size = val.length;
		this.allocateSize = val.length;
		this.lastOffset = 0;
		this.vals = new ArrayList<>(resizeCapacity);
		vals.add(val);
	}

	@Override
	public E get(int i) {
		int tmpi = i;
		for (E[] val: vals) {
			int offset = tmpi - val.length;
			if (offset < 0) {
				E v = val[tmpi];
				return v == null ? defaultValue : v;
			} else {
				tmpi = offset;
			}
		}
		throw new ArrayIndexOutOfBoundsException(i);
	}

	@Override
	public E set(int i, E e) {
		int tmpi = i;
		for (E[] val: vals) {
			int offset = tmpi - val.length;
			if (offset < 0) {
				return val[tmpi] = e;
			} else {
				tmpi = offset;
			}
		}
		throw new ArrayIndexOutOfBoundsException(i);
	}

	@Override
	public Iterator<E> iterator() {
		return this.listIterator();
	}

	private void removeUnuseArray() {
		// lastOffsetの更新,未使用リストの削除
		while (size < lastOffset) {
			int lastIndex = vals.size() - 1;
			E[] delVal = vals.remove(lastIndex--);
			E[] lastVal = vals.get(lastIndex);
			this.lastOffset -= lastVal.length;
			this.allocateSize -= delVal.length;
		}
		// size縮小の場合は縮小領域は初期化しない (領域が消える訳でなく意味が薄いため)
	}

	@SuppressWarnings("unchecked")
	@Override
	public void resize(int size) {
		if (this.size < size) {
			// 領域を拡大する場合
			E[] lastVal = vals.get(vals.size() - 1);
			// 未使用領域の初期化
			if (this.size < this.allocateSize) {
				for (int i = this.size; i < this.allocateSize; i++) {
					lastVal[i - lastOffset] = null;
				}
			}
			// sizeが0の場合は不要な配列を削除
			if (this.allocateSize == 0) {
				this.vals.remove(0);
			}

			// 新領域の確保
			if (this.allocateSize < size) {
				E[] val = (E[]) Array.newInstance(target, size - this.allocateSize);
				this.vals.add(val);
				this.allocateSize = size;
				this.lastOffset = lastOffset + lastVal.length;
			}
		} else {
			// 領域を縮小する場合
			removeUnuseArray();
		}
		this.size = size;
	}

	/**
	 * compressすることでT[] valのみ使いMTセーフになる
	 */
	@SuppressWarnings("unchecked")
	@Override
	public int compress() {
		// 後ろからnull値が続くまでsize,lastOffset,allocateSizeを計算する
		int oldSize = size;

		int lastIndex = vals.size() - 1;
		E[] lastVal = vals.get(lastIndex);
		do {
			int valIndex = size - lastOffset - 1;
			while (valIndex >= 0 && lastVal[valIndex] == null) {
				valIndex--;
				size--;
			}
			// 不要なリストがある場合、かつ0番目以外
			if (valIndex < 0 && lastIndex > 0) {
				E[] delVal = vals.remove(lastIndex--);
				lastVal = vals.get(lastIndex);
				this.lastOffset -= lastVal.length;
				this.allocateSize -= delVal.length;
			} else {
				break;
			}
		} while(true);

		if (vals.size() > 1 || this.allocateSize != this.size) {
			// valsが複数あれば統合する
			E[] newVal = (E[]) Array.newInstance(target, size);
			int offset = 0;
			for (int i = 0, valsSize = vals.size(); i < valsSize; i++) {
				E[] v = vals.get(i);
				if (i == valsSize - 1) {
					System.arraycopy(v, 0, newVal, offset, size - offset);
				} else {
					System.arraycopy(v, 0, newVal, offset, v.length);
					offset += v.length;
				}
			}
			this.vals.clear();
			this.vals.add(newVal);
			this.allocateSize = size;
		}

		this.lastOffset = 0;

		return oldSize - size;
	}

	@Override
	public int size() {
		return size;
	}



	@SuppressWarnings("unchecked")
	@Override
	public Object[] toArray() {
		if (vals.size() == 1 && allocateSize == size) {
			return vals.get(0);
		}
		E[] obj = (E[]) Array.newInstance(target, size);
		int offset = 0;
		for (int i=0;i<vals.size();i++) {
			E[] val = vals.get(i);
			int copySize = i == vals.size() - 1 ? size - offset : val.length;
			System.arraycopy(val, 0, obj, offset, copySize);
			offset += val.length;
		}
		return obj;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean add(E e) {
		if (size == allocateSize) {
			// 最後の配列と同じサイズだけ拡張する
			E[] lastVal = vals.get(vals.size() - 1);
			E[] val = (E[]) Array.newInstance(target, lastVal.length);
			val[0] = e;
			this.vals.add(val);
			size++;
			this.allocateSize += val.length;
			this.lastOffset += lastVal.length;
		} else {
			E[] lastVal = vals.get(vals.size() - 1);
			int lastIndex = size - lastOffset;
			size++;
			lastVal[lastIndex] = e;
		}
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		@SuppressWarnings("unchecked")
		E[] val = c.toArray((E[]) Array.newInstance(target, 0));
		return addAll(val);
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		@SuppressWarnings("unchecked")
		E[] val = c.toArray((E[]) Array.newInstance(target, 0));
		return addAll(index, val);
	}


	@SuppressWarnings("unchecked")
	@Override
	public boolean addAll(E[] val) {
		E[] lastVal = vals.get(vals.size() - 1);
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
				E[] newVal = (E[]) Array.newInstance(target, newSize);
				System.arraycopy(val, copySize, newVal, 0, newSize);
				this.vals.add(newVal);
				this.allocateSize += newSize;
				this.lastOffset += lastVal.length;
			} else {
				copySize = val.length;
			}
			System.arraycopy(val, 0, lastVal, size - lastOffset, copySize);
			this.size += val.length;
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean addAll(int index, E[] val) {
		int tmpValIndex = vals.size() - 1;
		E[] tmpVal = vals.get(tmpValIndex);

		if (index == size) {
			this.addAll(val);
		} else if (index < 0 && size < index) {
			throw new ArrayIndexOutOfBoundsException(index);
		} else {
			int tmpi = index -lastOffset;
			while (0 <= tmpValIndex) {
				if (tmpi < 0) {
					tmpi += tmpVal.length;
					tmpVal = vals.get(--tmpValIndex);
				} else if (tmpi == 0) {
					this.vals.add(tmpValIndex, val);
					break;
				} else {
					// split array
					int postLen = tmpVal.length - tmpi;
					E[] preVal = (E[]) Array.newInstance(target, tmpi);
					E[] postVal = (E[]) Array.newInstance(target, postLen);

					System.arraycopy(tmpVal, 0, preVal, 0, tmpi);
					System.arraycopy(tmpVal, tmpi, postVal, 0, postLen);

					vals.remove(tmpValIndex);
					vals.add(tmpValIndex, postVal);
					vals.add(tmpValIndex, val);
					vals.add(tmpValIndex, preVal);

					break;
				}
			}
			this.size += val.length;
			this.allocateSize += val.length;
			this.lastOffset += val.length;
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
			modified = true;
			removeUnuseArray();
		}
		return modified;
	}

	@Override
	public void clear() {
		E[] val = vals.get(0);
		if (vals.size() > 1) {
			this.vals.clear();
			this.vals.add(val);
		}
		this.allocateSize = val.length;
		this.size = val.length;
		this.lastOffset = 0;
		Arrays.fill(val, null);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void add(int index, E element) {
		int tmpValIndex = vals.size() - 1;
		E[] tmpVal = vals.get(tmpValIndex);
		E[] val;
		if (this.size == this.allocateSize) {
			// 最後の配列と同じサイズだけ拡張する
			val = (E[]) Array.newInstance(target, tmpVal.length);
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
	public E remove(int index) {
		int tmpValIndex = vals.size() - 1;
		E[] tmpVal = vals.get(tmpValIndex);
		E tmpItem = null;
		E ret = null;
		if (index < 0 && size <= index) {
			throw new ArrayIndexOutOfBoundsException(index);
		} else {
			int tmpi = index - lastOffset;
			while(0 <= tmpValIndex) {
				int lastIndex = tmpVal.length - 1;
				if (tmpi < 0) {
					E firstItem = tmpVal[0];
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
	public ListIterator<E> listIterator() {
		return new ListArrayIterator<>(vals, size, defaultValue);
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		return new ListArrayIterator<>(vals, size, index, defaultValue);
	}

}
