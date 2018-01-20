package jp.ksgwr.jadatrie;

import java.io.BufferedReader;

import jp.ksgwr.jadatrie.core.KeyValue;

public class KeyFileReadIterator extends FileReadIterator<Boolean> {

	public KeyFileReadIterator(BufferedReader reader) {
		super(reader);
	}

	@Override
	public KeyValue<Boolean> readString(String line) {
		return new KeyValue<Boolean>(line, Boolean.TRUE);
	}

}