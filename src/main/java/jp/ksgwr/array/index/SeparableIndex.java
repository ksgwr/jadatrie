package jp.ksgwr.array.index;

import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;

/**
 * separatable index interface
 *
 * @author ksgwr
 *
 * @param <T> item class
 */
public interface SeparableIndex<T extends Serializable> {

	/**
	 * get item size
	 * @return item size
	 */
	int getItemSize();

	/**
	 * get alocate size
	 * @return allocate size;
	 */
	int getAllocateSize();

	/**
	 * get segment size
	 * @return segment size
	 */
	int getSegmentSize();

	/**
	 * get segment number
	 * @param offset index offset
	 * @return segment number
	 */
	int getSegmentNumber(int offset);

	/**
	 * get offset by segment number
	 * @param segmentNum segment number
	 * @return offset
	 */
	int getOffset(int segmentNum);

	/**
	 * get item/segment size
	 * @param segmentNum segment number
	 * @return item/segment size
	 */
	int getItemPerSegmentSize(int segmentNum);

	/**
	 * load segment
	 * @param segmentNum segment number
	 * @param target target class
	 * @return segment items
	 * @throws IOException file error
	 * @throws ClassNotFoundException failed new array creation
	 */
	T[] loadSegment(int segmentNum, Class<T> target) throws IOException, ClassNotFoundException;

	/**
	 * save segment
	 * @param segmentNum segment number
	 * @param val target items
	 * @param length save items max size
	 * @throws IOException file error
	 */
	void saveSegment(int segmentNum, T[] val, int length) throws IOException;

	/**
	 * delete segment
	 * @param segmentNum segment number
	 */
	void deleteSegment(int segmentNum);

	/**
	 * increase item size and update allocate size and segment size if needed
	 * decide segment number, offset, item per segment size
	 * @param size new item size
	 */
	void increaseItemSize(int size);

	/**
	 * decrease item size
	 * decide segment number, offset, item per segment size
	 * @param size new item size
	 */
	void decreaseItemSize(int size);

	/**
	 * increase allocate size and update segment size if needed
	 * @param size new allocate size
	 */
	void increaseAllocateSize(int size);

	/**
	 * decrease allocate size and update segment size if needed
	 * @param size new allocate size
	 */
	void decreaseAllocateSize(int size);

	/**
	 * save info (ex. itemSize, segmentSize)
	 * @throws IOException file error
	 */
	void saveInfo() throws IOException;

	/**
	 * load info, must call this or cleanup before use
	 * @throws IOException file error
	 */
	void loadInfo() throws IOException;

	/**
	 * is index exist
	 * @return if true index already exist
	 */
	boolean isExist();

	/**
	 * cleanup all data
	 */
	void cleanup();

	/**
	 * save all data
	 * @param iterator iterator
	 * @param size size
	 * @param target target class
	 * @throws IOException file error
	 */
	void save(Iterator<T> iterator, int size, Class<T> target) throws IOException;

}
