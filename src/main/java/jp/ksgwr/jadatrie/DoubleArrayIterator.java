package jp.ksgwr.jadatrie;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Map.Entry;

import jp.ksgwr.jadatrie.core.KeyValue;

public class DoubleArrayIterator<T> implements Iterator<Entry<String, T>>{

	private Class<T> target;
	private String[] key;
	private T[] val;
	private int i;

	public DoubleArrayIterator(String[] key, T[] val) {
		this(key, val, null);
	}

	public DoubleArrayIterator(String[] key, T[] val, Class<T> target) {
		this.key = key;
		this.val = val;
		this.target = target;
		this.i = 0;
		if (this.val != null && key.length != val.length) {
			throw new RuntimeException("key and val array must same size. key:"
					+ key.length + ",val:" + val.length);
		}
	}

	@Override
	public boolean hasNext() {
		return i < key.length;
	}

	@Override
	public Entry<String, T> next() {
		return new KeyValue<T>(key[i], val != null ? val[i++] : null);
	}

	@Override
	public void remove() {
		if (target == null) {
			throw new RuntimeException("target class must be set");
		}

		i--;
		if (i < 0) {
			return;
		}
		String[] newKey = new String[key.length - 1];
		@SuppressWarnings("unchecked")
		T[] newVal = (T[]) Array.newInstance(target, key.length - 1);

		if (i > 0) {
			System.arraycopy(key, 0, newKey, 0, i);
			System.arraycopy(val, 0, newVal, 0, i);
		}
		if (i < key.length - 1) {
			System.arraycopy(key, i + 1, newKey, i + 1, key.length - i);
			System.arraycopy(val, i + 1, newVal, i + 1, key.length - i);
		}
		this.key = newKey;
		this.val = newVal;
	}

}
