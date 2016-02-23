package jp.ksgwr.jadatrie;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

public class KeyListIterator implements Iterator<Entry<String, Boolean>>{

	private Iterator<String> ite;

	public KeyListIterator(List<String> key) {
		this.ite = key.iterator();
	}

	@Override
	public boolean hasNext() {
		return ite.hasNext();
	}

	@Override
	public Entry<String, Boolean> next() {
		return new KeyValue<Boolean>(ite.next(), Boolean.TRUE);
	}

	@Override
	public void remove() {
		ite.remove();
	}
}
