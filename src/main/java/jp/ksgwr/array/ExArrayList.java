package jp.ksgwr.array;

import java.io.IOException;
import java.util.List;

/**
 * Extra Array List
 * this list is constant capacity size.
 * it expects to call "get" and "set" than "add", so it can access null items.
 * it implemented by distributed internal array.
 * it can allocate internal array efficiently and write external file partially.
 *
 * @author ksgwr
 *
 * @param <E> item class
 */
public interface ExArrayList<E> extends List<E>, AutoCloseable {

    /**
     * resize capacity
     * @param size capacity size
     */
    void resize(int size);

    /**
     * compress capacity, free unuse space
     * should call on only get use case.
     * @return compress size
     */
    int compress();

    /**
     * add all item array
     * @param val item array
     * @return if true success.
     */
    boolean addAll(E[] val);

    /**
     * add all item array
     * @param index index
     * @param val item array
     * @return if true success.
     */
    boolean addAll(int index, E[] val);

    @Override
    void close() throws IOException;
}
