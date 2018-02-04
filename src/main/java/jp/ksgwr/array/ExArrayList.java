package jp.ksgwr.array;

import java.io.IOException;
import java.util.List;

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
