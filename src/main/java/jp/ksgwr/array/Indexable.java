package jp.ksgwr.array;

import jp.ksgwr.array.index.SeparableIndex;

import java.io.IOException;
import java.io.Serializable;

public interface Indexable<E extends Serializable> {

    /**
     * load external index
     * @param index index
     * @throws IOException file error
     * @throws ClassNotFoundException target class error
     */
    void load(SeparableIndex<E> index) throws IOException, ClassNotFoundException;

    /**
     * save index
     * @param index index
     * @throws IOException file error
     */
    void save(SeparableIndex<E> index) throws IOException;
}
