package jp.ksgwr.array.index;

import jp.ksgwr.array.IndexIterator;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Objects;

public class InfoSegmentIndex<T extends Serializable, Serializer, Deserializer> implements SeparableIndex<T> {

	/** info file name */
	private static final String INFO_FILENAME = "info";

	/** seg prefix file name */
	private static final String SEG_PREFIX = "seg";

	/** index directory */
	protected final File directory;

	/** info file name */
	protected final File infoFile;

	/** seg file prefix name */
	protected final String segPrefix;

	/** index size */
	protected int itemSize;

	/** allocate index size */
	protected int allocateSize;

	/** segmentg size */
	protected int segmentSize;

	/** indexer */
	protected final InfoSegmentIndexer<T, Serializer, Deserializer> indexer;

	public InfoSegmentIndex(File directory, String prefix, InfoSegmentIndexer<T, Serializer, Deserializer> indexer) {
		this.directory = directory;
		this.infoFile = new File(directory, prefix + INFO_FILENAME);
		this.segPrefix = prefix + SEG_PREFIX;
		this.segmentSize = 1;
		this.indexer = indexer;
	}

	protected File getSegmentFile(int segmentNum) {
		return new File(directory, segPrefix);
	}

	protected void loadInfo(InfoSegmentIndexer<T,Serializer,Deserializer> indexer, Deserializer deserializer) throws IOException {
		this.itemSize = indexer.deserializeIntProp(deserializer);
		this.allocateSize = indexer.deserializeIntProp(deserializer);
	}

	protected void saveInfo(InfoSegmentIndexer<T,Serializer,Deserializer> indexer, Serializer serializer) throws IOException {
		indexer.serializeIntProp(serializer, this.itemSize);
		indexer.serializeIntProp(serializer, this.allocateSize);
	}

	@Override
	public void loadInfo() throws IOException {
		Deserializer deserializer = null;
		try {
			deserializer = indexer.openDeserializer(infoFile, false);
			loadInfo(indexer, deserializer);
		} finally {
			if (deserializer != null) {
				indexer.closeDeserializer(deserializer);
			}
		}
	}

	@Override
	public int getItemSize() {
		return itemSize;
	}

	@Override
	public int getAllocateSize() {
		return allocateSize;
	}

	@Override
	public int getSegmentSize() {
		return segmentSize;
	}

	@Override
	public int getSegmentNumber(int offset) {
		return 0;
	}

	@Override
	public int getOffset(int segmentNum) {
		return 0;
	}

	@Override
	public int getItemPerSegmentSize(int segmentNum) {
		return itemSize <= allocateSize ? allocateSize : itemSize;
	}

	@Override
	public void deleteSegment(int segmentNum) {
		this.getSegmentFile(segmentNum).delete();
	}

	@Override
	public void increaseItemSize(int size) {
		this.itemSize = size;
		int lastSegmentNum = getSegmentNumber(size - 1);
		int newAllocateSize = getOffset(lastSegmentNum) + getItemPerSegmentSize(lastSegmentNum);
		increaseAllocateSize(newAllocateSize);
	}

	@Override
	public void decreaseItemSize(int size) {
		this.itemSize = size;
	}

	@Override
	public void increaseAllocateSize(int size) {
		this.allocateSize = size;
		this.segmentSize = getSegmentNumber(allocateSize - 1) + 1;
	}

	@Override
	public void decreaseAllocateSize(int size) {
		this.allocateSize = size;
		this.segmentSize = size == 0 ? 1 : getSegmentNumber(allocateSize - 1) + 1;
	}

	@Override
	public void saveInfo() throws IOException {
		Serializer serializer = null;
		try {
			serializer = indexer.openSerializer(infoFile, false);
			saveInfo(indexer, serializer);
		} finally {
			if (serializer != null) {
				indexer.closeSerializer(serializer);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void save(Iterator<T> ite, int size, Class<T> target) throws IOException {
		cleanup();
		increaseItemSize(size);
		int i = 0;
		Serializer serializer = null;
		try {
			int segmentNum = -1;
			T[] vals = null;
			int tmpi = 0;
			while (ite.hasNext()) {
				int tmpSegmentNum = this.getSegmentNumber(i);
				if (segmentNum < tmpSegmentNum) {
					if (serializer != null) {
						indexer.serializeSegment(serializer, vals, tmpi);
						indexer.closeSerializer(serializer);
					}
					segmentNum = tmpSegmentNum;
					int valSize = this.getItemPerSegmentSize(segmentNum);
					vals = (T[]) Array.newInstance(target, valSize);
					tmpi = 0;
					File segmentFile = getSegmentFile(segmentNum);
					serializer = indexer.openSerializer(segmentFile, true);

				}
				vals[tmpi++] = ite.next();
				i++;
			}
			if (serializer != null) {
				indexer.serializeSegment(serializer, vals, tmpi);
				indexer.closeSerializer(serializer);
				serializer = null;
			}
			saveInfo();
		} finally {
			if (serializer != null) {
				indexer.closeSerializer(serializer);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public T[] loadSegment(int segmentNum, Class<T> target) throws IOException, ClassNotFoundException {
		int valSize = this.getItemPerSegmentSize(segmentNum);
		T[] vals = (T[]) Array.newInstance(target, valSize);
		File segmentFile = getSegmentFile(segmentNum);
		if (!segmentFile.exists()) {
			return vals;
		}

		// 読み込みの初期化を効率化する
		int length;
		if (segmentNum == segmentSize - 1) {
			length = this.getItemSize() - this.getOffset(segmentNum);
		} else {
			length = valSize;
		}
		Deserializer deserializer = null;
		try {
			deserializer = indexer.openDeserializer(segmentFile, true);
			indexer.deserializeSegment(deserializer, vals, length);
		} finally {
			if (deserializer != null) {
				indexer.closeDeserializer(deserializer);
			}
		}

		return vals;
	}

	@Override
	public void saveSegment(int segmentNum, T[] vals, int length) throws IOException {
		Serializer serializer = null;
		try {
			File segFile = getSegmentFile(segmentNum);
			serializer = indexer.openSerializer(segFile, true);
			indexer.serializeSegment(serializer, vals, length);
		} finally {
			if (serializer != null) {
				indexer.closeSerializer(serializer);
			}
		}

	}

	@Override
	public void cleanup() {
		infoFile.delete();
		File [] segList = directory.listFiles((dir, name) -> name.startsWith(segPrefix));
		if (segList != null) {
			for (File segFile : segList) {
				segFile.delete();
			}
		}
	}

	@Override
	public boolean isExist() {
		return infoFile.exists();
	}

}
