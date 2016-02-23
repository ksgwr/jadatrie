package jp.ksgwr.jadatrie;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

public class DoubleListIterator<T> implements Iterator<Entry<String, T>>{

	Iterator<String> key;
	Iterator<T> val;

	public DoubleListIterator(List<String> key, List<T> val) {
		this.key = key.iterator();
		this.val = val.iterator();
	}

	@Override
	public boolean hasNext() {
		return key.hasNext() && val.hasNext();
	}

	@Override
	public Entry<String, T> next() {
		return new KeyValue<T>(key.next(), val.next());
	}

	@Override
	public void remove() {
		key.remove();
		val.remove();
	}

}
