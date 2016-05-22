package jp.ksgwr.array;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Iterator;

public class Index<T extends Serializable> {

	private static final String INFO_FILENAME = "info";

	private static final String SEG_PREFIX = "seg";

	private final File directory;

	private final String prefix;

	private final String infoFilename;

	private final String segPrefix;

	private int splitSize;

	private int size;

	private Iterator<T> iterator;

	public Index(File directory) {
		this(directory, "");
	}

	public Index(File directory, String prefix) {
		this(directory, prefix, 0);
	}

	public Index(File directory, int splitSize) {
		this(directory, "", splitSize);
	}

	public Index(File directory, String prefix, int splitSize) {
		this.directory = directory;
		this.prefix = prefix;
		this.splitSize = splitSize;
		this.infoFilename = prefix + INFO_FILENAME;
		this.segPrefix = prefix + SEG_PREFIX;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public void setIterator(Iterator<T> iterator) {
		this.iterator = iterator;
	}

	public void setSplitSize(int splitSize) {
		this.splitSize = splitSize;
	}

	public int getSize() {
		return size;
	}

	public int getSplitSize() {
		return splitSize;
	}

	public String getPrefix() {
		return prefix;
	}

	public void cleanupSegment() throws IOException {
		for(File file:directory.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.indexOf(segPrefix) >= 0;
			}
		})) {
			file.delete();
		}
	}

	public void save() throws IOException {
		saveInfo();
		cleanupSegment();
		int i = 0;
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(new BufferedOutputStream(
					new FileOutputStream(new File(directory, segPrefix + i))));
			while (iterator.hasNext()) {
				if (splitSize > 0 && i % splitSize == 0) {
					if (oos != null) {
						oos.close();
						oos = null;
					}
					oos = new ObjectOutputStream(new BufferedOutputStream(
							new FileOutputStream(new File(directory, segPrefix + i))));
				}
				T val = iterator.next();
				oos.writeObject(val);
				i++;
			}
		} finally {
			if (oos != null) {
				oos.close();
			}
		}
	}

	public void saveInfo() throws IOException {
		DataOutputStream out = null;
		try {
			out = new DataOutputStream(new BufferedOutputStream(
				new FileOutputStream(new File(directory, infoFilename))));
			out.writeInt(size);
			out.writeInt(splitSize);
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	public void saveSegment(int offset, Class<T> target, T[] vals, int size) throws IOException {
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(new BufferedOutputStream(
					new FileOutputStream(new File(directory, segPrefix + offset))));
			for (int i = 0; i < size; i++) {
				oos.writeObject(vals[i]);
			}
		} finally {
			if (oos != null) {
				oos.close();
			}
		}

	}

	public void loadInfo() throws IOException {
		DataInputStream is = null;
		try {
			is = new DataInputStream(new BufferedInputStream(
					new FileInputStream(new File(directory, infoFilename))));
			this.size = is.readInt();
			this.splitSize = is.readInt();
		} finally {
			if (is != null) {
				is.close();
			}
		}
	}

	@SuppressWarnings("unchecked")
	public T[] loadSegment(int offset, Class<T> target) throws IOException, ClassNotFoundException {
		T[] vals = null;
		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(new BufferedInputStream(
					new FileInputStream(new File(directory, segPrefix + offset))));

			int valSize;
			if (splitSize == Integer.MAX_VALUE) {
				valSize = size;
			} else if (offset + splitSize < size) {
				valSize = splitSize;
			} else {
				valSize = size - offset;
			}
			vals = (T[]) Array.newInstance(target, valSize);

			for (int i=0;i<valSize;i++) {
				vals[i] = (T) ois.readObject();
			}

		} finally {
			if (ois != null) {
				ois.close();
			}
		}

		return vals;
	}

	public boolean isExistSegment(int offset) {
		return new File(directory, segPrefix + offset).exists();
	}

	public boolean deleteSegment(int offset) {
		return new File(directory, segPrefix + offset).delete();
	}
}
