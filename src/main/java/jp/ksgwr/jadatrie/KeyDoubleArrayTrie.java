package jp.ksgwr.jadatrie;

import java.util.List;

public class KeyDoubleArrayTrie extends DoubleArrayTrie<Boolean> {

	public KeyDoubleArrayTrie() {
		super(Boolean.class);
	}

	public void build(String[] key) {
		super.build(new KeyArrayIterator(key), key.length);
	}

	public void build(List<String> key) {
		super.build(new KeyListIterator(key), key.size());
	}

}
