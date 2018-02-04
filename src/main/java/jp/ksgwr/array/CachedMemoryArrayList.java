package jp.ksgwr.array;

import java.lang.reflect.Array;
import java.util.Collection;

/**
 * Cached Memory Array List (more speedy get and set)
 *
 * @author ksgwr
 *
 * @param <E> item class
 */
public class CachedMemoryArrayList<E> extends MemoryArrayList<E> {

	/** array index */
	protected int valIndex;

	/** offset index */
	protected int offset;

	/** current value */
	protected E[] val;

	/**
	 * constructor
	 * @param target target class
	 * @param size initial array size
	 */
	public CachedMemoryArrayList(Class<E> target, int size) {
		this(target, null, size, 10);
	}

	/**
	 * constructor
	 * @param target target class
	 * @param size initial array size
	 */
	public CachedMemoryArrayList(Class<E> target, E defaultValue, int size) {
		this(target, defaultValue, size, 10);
	}

	@SuppressWarnings("unchecked")
	public CachedMemoryArrayList(Class<E> target, int size, int resizeCapacity) {
		this(target, null, (E[])Array.newInstance(target, size), resizeCapacity);
	}

	/**
	 * constructor
	 * @param target target class
	 * @param size initial array size
	 * @param resizeCapacity resize capacity size
	 */
	@SuppressWarnings("unchecked")
	public CachedMemoryArrayList(Class<E> target, E defaultValue, int size, int resizeCapacity) {
		this(target, defaultValue, (E[])Array.newInstance(target, size), resizeCapacity);
	}

	public CachedMemoryArrayList(Class<E> target, E[] val, int resizeCapacity) {
		super(target, null, val, resizeCapacity);
		initCache();
	}

	/**
	 * constructor
	 * @param target target class
	 * @param val initial val array
	 * @param resizeCapacity resize capacity size
	 */
	public CachedMemoryArrayList(Class<E> target, E defaultValue, E[] val, int resizeCapacity) {
		super(target, defaultValue, val, resizeCapacity);
		initCache();
	}

	/**
	 * search relative position
	 * @param relativePosition invalid relative position
	 * @param absolutePosition absolute position
	 * @return new updated relative position
	 */
	private int searchRelativeIndex(int relativePosition, int absolutePosition) {
		if (relativePosition < 0) {
			for (valIndex--; valIndex >= 0; valIndex--) {
				this.val = vals.get(valIndex);
				this.offset -= val.length;
				relativePosition = absolutePosition - offset;
				if (relativePosition < val.length && 0 <= relativePosition) {
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
	public E get(int i) {
		int tmpi = i - offset;
		E v;
		try {
			// TODO: 未使用領域にアクセスする可能性はある
			v = val[tmpi];
		} catch (ArrayIndexOutOfBoundsException e) {
			tmpi = searchRelativeIndex(tmpi, i);
			v = val[tmpi];
		}
		return v == null ? defaultValue : v;
	}

	@Override
	public E set(int i, E t) {
		int tmpi = i - offset;
		try {
			return this.val[tmpi] = t;
		} catch (ArrayIndexOutOfBoundsException e) {
			tmpi = searchRelativeIndex(tmpi, i);
			return this.val[tmpi] = t;
		}
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		boolean modified = super.addAll(index, c);
		this.initCache();
		return modified;
	}

	@Override
	public boolean addAll(int index, E[] val) {
		boolean modified = super.addAll(index, val);
		this.initCache();
		return modified;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		boolean modified = super.removeAll(c);
		if (modified) {
			this.initCache();
		}
		return modified;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		boolean modified = super.retainAll(c);
		if (modified) {
			this.initCache();
		}
		return modified;
	}

	/**
	 * initialize cache
	 */
	private void initCache() {
		this.valIndex = 0;
		this.offset = 0;
		this.val = vals.get(0);
	}

	@Override
	public void resize(int size) {
		super.resize(size);
		this.initCache();
	}

	@Override
	public int compress() {
		int compressSize = super.compress();
		this.initCache();
		return compressSize;
	}

	@Override
	public void clear() {
		super.clear();
		this.initCache();
	}

}
