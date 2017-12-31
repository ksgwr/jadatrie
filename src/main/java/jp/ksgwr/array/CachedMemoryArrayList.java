package jp.ksgwr.array;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/**
 * Cached Memory Array List (more speedy get and set)
 *
 * @author ksgwr
 *
 * @param <T> item class
 */
public class CachedMemoryArrayList<T extends Serializable> extends MemoryArrayList<T> {

	/** array index */
	private int valIndex;

	/** offset index */
	private int offset;

	/** current value */
	private T[] val;

	/**
	 * constructor
	 * @param target target class
	 */
	protected CachedMemoryArrayList(Class<T> target) {
		super(target);
	}

	/**
	 * constructor
	 * @param target target class
	 * @param size initial array size
	 */
	public CachedMemoryArrayList(Class<T> target, int size) {
		this(target, size, 10);
	}

	/**
	 * constructor
	 * @param target target class
	 * @param size initial array size
	 * @param resizeCapacity resize capacity size
	 */
	public CachedMemoryArrayList(Class<T> target, int size, int resizeCapacity) {
		super(target, size, resizeCapacity);
		this.valIndex = 0;
		this.val = vals.get(0);
		this.offset = 0;
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
	public void compress() {
		super.compress();
		this.initCache();
	}

	@Override
	public void load(File directory) throws IOException, ClassNotFoundException {
		super.load(directory);
		this.initCache();
	}

	@Override
	public void clear() {
		super.clear();
		this.initCache();
	}

}
