package jp.ksgwr.jadatrie;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

import jp.ksgwr.jadatrie.core.KeyValue;
import jp.ksgwr.jadatrie.core.KeyValueStringDeserializer;

public class FileReadIterator<T> implements Iterator<Entry<String,T>> {

	private final BufferedReader br;
	private final KeyValueStringDeserializer<T> deserializer;
	private String line;
	private boolean init;

	public FileReadIterator(BufferedReader reader, KeyValueStringDeserializer<T> deserializer) {
		this.br = reader;
		this.deserializer = deserializer;
		this.init = false;
	}

	@Override
	public boolean hasNext() {
		try {
			init = true;
			line = br.readLine();
			if (line != null) {
				return true;
			} else {
				br.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return false;
	}

	@Override
	public Entry<String, T> next() {
		if (!init) {
			// not call hasNext
			try {
				line = br.readLine();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		KeyValue<T> entry = deserializer.createKeyValue(line);
		init = false;
		while (entry == null && this.hasNext()) {
			entry = (KeyValue<T>) this.next();
		}
		return entry;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	public int readRestLines() {
		int count = 0;
		while (this.hasNext()) {
			this.next();
			count++;
		}
		return count;
	}

}
