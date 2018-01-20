package jp.ksgwr.jadatrie;

import java.util.Iterator;
import java.util.Map.Entry;

import jp.ksgwr.jadatrie.core.KeyValue;

public class KeyArrayIterator implements Iterator<Entry<String, Boolean>>{

	private String[] key;
	private int i;

	public KeyArrayIterator(String[] key) {
		this.key = key;
		this.i = 0;
	}

	@Override
	public boolean hasNext() {
		return i < key.length;
	}

	@Override
	public Entry<String, Boolean> next() {
		return new KeyValue<Boolean>(key[i++], Boolean.TRUE);
	}

	@Override
	public void remove() {
		i--;
		if (i < 0) {
			return;
		}
		String[] newKey = new String[key.length - 1];
		if (i > 0) {
			System.arraycopy(key, 0, newKey, 0, i);
		}
		if (i < key.length - 1) {
			System.arraycopy(key, i + 1, newKey, i + 1, key.length - i);
		}
		this.key = newKey;
	}

}
