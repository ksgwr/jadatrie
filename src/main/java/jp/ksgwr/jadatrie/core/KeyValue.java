package jp.ksgwr.jadatrie.core;

import java.util.Map.Entry;

public class KeyValue<T> implements Entry<String, T> {

	private final String key;
	private T val;

	public KeyValue(String key) {
		this.key = key;
	}

	public KeyValue(String key, T val) {
		this.key = key;
		this.val = val;
	}

	@Override
	public String getKey() {
		return key;
	}

	@Override
	public T getValue() {
		return val;
	}

	@Override
	public T setValue(T value) {
		return this.val = value;
	}

}
