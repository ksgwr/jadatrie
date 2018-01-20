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
public interface SeparatableIndex<T extends Serializable> {

	/**
	 * get item size
	 * @return item size
	 */
	public int getItemSize();

	/**
	 * get alocate size
	 * @return allocate size;
	 */
	public int getAllocateSize();

	/**
	 * get segment size
	 * @return segment size
	 */
	public int getSegmentSize();

	/**
	 * get segment number
	 * @param offset index offset
	 * @return segment number
	 */
	public int getSegmentNumber(int offset);

	/**
	 * get offset by segment number
	 * @param segmentNum segment number
	 * @return offset
	 */
	public int getOffset(int segmentNum);

	/**
	 * get item/segment size
	 * @param segmentNum segment number
	 * @return item/segment size
	 */
	public int getItemPerSegmentSize(int segmentNum);

	/**
	 * load segment
	 * @param segmentNum segment number
	 * @param target target class
	 * @return segment items
	 * @throws IOException file error
	 * @throws ClassNotFoundException failed new array creation
	 */
	public T[] loadSegment(int segmentNum, Class<T> target) throws IOException, ClassNotFoundException;

	/**
	 * save segment
	 * @param segmentNum segment number
	 * @param val target items
	 * @param length save items max size
	 * @throws IOException file error
	 */
	public void saveSegment(int segmentNum, T[] val, int length) throws IOException;

	/**
	 * delete segment
	 * @param segmentNum segment number
	 * @throws IOException file error
	 */
	public void deleteSegment(int segmentNum) throws IOException;

	/**
	 * update item size
	 * @param size new sizeW
	 */
	public void updateItemSize(int size);

	/**
	 * reserve allocate size
	 * @param size allocate new size (must larger than item size)
	 */
	public void reserveAllocateSize(int size);

	/**
	 * save info (ex. itemSize, segmentSize)
	 * @throws IOException
	 */
	public void saveInfo() throws IOException;

	/**
	 * is index exist
	 * @return if true index already exist
	 */
	public boolean isExist();

	/**
	 * cleanup all data
	 * @throws IOException file error
	 */
	public void cleanup() throws IOException;

	/**
	 * save all data
	 * @param iterator iterator
	 * @param size size
	 * @param target target class
	 * @throws IOException file error
	 */
	public void save(Iterator<T> iterator, int size, Class<T> target) throws IOException;

}
