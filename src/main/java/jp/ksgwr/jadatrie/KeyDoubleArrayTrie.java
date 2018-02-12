package jp.ksgwr.jadatrie;

import java.util.List;

public class KeyDoubleArrayTrie extends DoubleArrayTrie<Boolean> {

	public KeyDoubleArrayTrie() {
		super();
	}

	public void build(String[] key) {
		super.setKeyValue(key, null, Boolean.class);
		super.build(new KeyArrayIterator(key), key.length, false);
	}

	public void build(List<String> key) {
		super.setKeyValue(key, null);
		super.build(new KeyListIterator(key), key.size(), false);
	}

}
