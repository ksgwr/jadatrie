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

/**
 * index directory
 * info file include indexSize and splitSize.
 * seg file include item value.
 * This class implemented by Java standard object serialize stream.
 * it can implemented by other method (e.g. MessagePack)
 *
 * @author ksgwr
 *
 * @param <T> item class
 */
public class Index<T extends Serializable> {

	/** info file name */
	private static final String INFO_FILENAME = "info";

	/** seg prefix file name */
	private static final String SEG_PREFIX = "seg";

	/** index directory */
	private final File directory;

	/** common prefix */
	private final String prefix;

	/** info file name */
	private final String infoFilename;

	/** seg file prefix name */
	private final String segPrefix;

	/** split size */
	private int splitSize;

	/** index size */
	private int size;

	/**
	 * constructor
	 * @param directory index directory
	 */
	public Index(File directory) {
		this(directory, "");
	}

	/**
	 * constructor
	 * @param directory index directory
	 * @param prefix common prefix file name
	 */
	public Index(File directory, String prefix) {
		this(directory, prefix, 0);
	}

	/**
	 * constructor
	 * @param directory index directory
	 * @param splitSize split size
	 */
	public Index(File directory, int splitSize) {
		this(directory, "", splitSize);
	}

	/**
	 * constructor
	 * @param directory index directory
	 * @param prefix common prefix file name
	 * @param splitSize split size
	 */
	public Index(File directory, String prefix, int splitSize) {
		this.directory = directory;
		this.prefix = prefix;
		this.splitSize = splitSize;
		this.infoFilename = prefix + INFO_FILENAME;
		this.segPrefix = prefix + SEG_PREFIX;
	}

	/**
	 * set index size
	 * @param size index size
	 */
	public void setSize(int size) {
		this.size = size;
	}

	/**
	 * set split size
	 * @param splitSize
	 */
	public void setSplitSize(int splitSize) {
		this.splitSize = splitSize;
	}

	/**
	 * get size
	 * @return index size
	 */
	public int getSize() {
		return size;
	}

	/**
	 * get split size
	 * @return split size
	 */
	public int getSplitSize() {
		return splitSize;
	}

	/**
	 * get common prefix
	 * @return common prefix
	 */
	public String getPrefix() {
		return prefix;
	}

	/**
	 * delete all segment file.
	 * @throws IOException
	 */
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

	/**
	 * save all data
	 * @param iterator data iterator
	 * @throws IOException file error
	 */
	public void save(Iterator<T> iterator) throws IOException {
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
			int j;
			if (splitSize != Integer.MAX_VALUE && (j = size % splitSize) != 0) {
				for (; j < splitSize; j++) {
					oos.writeObject(null);
				}
			}
		} finally {
			if (oos != null) {
				oos.close();
			}
		}
	}

	/**
	 * save info file
	 * @throws IOException file error
	 */
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

	/**
	 * save segment
	 * @param offset file offset
	 * @param target target class
	 * @param vals segment values
	 * @throws IOException file error
	 */
	public void saveSegment(int offset, Class<T> target, T[] vals) throws IOException {
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(new BufferedOutputStream(
					new FileOutputStream(new File(directory, segPrefix + offset))));
			for (int i = 0; i < vals.length; i++) {
				oos.writeObject(vals[i]);
			}
		} finally {
			if (oos != null) {
				oos.close();
			}
		}

	}

	/**
	 * load info and set size and splitSize.
	 * @throws IOException file error
	 */
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

	/**
	 * load segment
	 * @param offset file offset
	 * @param target target class
	 * @return segment values
	 * @throws IOException file error
	 * @throws ClassNotFoundException target class error
	 */
	@SuppressWarnings("unchecked")
	public T[] loadSegment(int offset, Class<T> target) throws IOException, ClassNotFoundException {
		T[] vals = null;
		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(new BufferedInputStream(
					new FileInputStream(new File(directory, segPrefix + offset))));

			int valSize;
			if (splitSize == Integer.MAX_VALUE) {
				// not split, allocate minimum size
				valSize = size;
			} else {
				valSize = splitSize;
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

	/**
	 * check segment file
	 * @param offset offset
	 * @return if true exist file
	 */
	public boolean isExistSegment(int offset) {
		return new File(directory, segPrefix + offset).exists();
	}

	/**
	 * delete segment file
	 * @param offset offset
	 * @return if true delete file
	 */
	public boolean deleteSegment(int offset) {
		return new File(directory, segPrefix + offset).delete();
	}
}
