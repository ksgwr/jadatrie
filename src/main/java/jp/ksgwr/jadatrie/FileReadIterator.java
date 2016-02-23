package jp.ksgwr.jadatrie;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

public abstract class FileReadIterator<T> implements Iterator<Entry<String,T>> {

	BufferedReader br;
	String line;
	boolean init;

	public FileReadIterator(BufferedReader reader) {
		this.br = reader;
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
		return readString(line);
	}

	public abstract KeyValue<T> readString(String line);

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

}
