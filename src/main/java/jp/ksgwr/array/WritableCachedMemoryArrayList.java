package jp.ksgwr.array;

import jp.ksgwr.array.index.SeparableIndex;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class WritableCachedMemoryArrayList<E extends Serializable> extends CachedMemoryArrayList<E> implements WritableExArrayList<E> {

    /**
     * constructor
     * @param target target class
     * @param size initial array size
     */
    public WritableCachedMemoryArrayList(Class<E> target, int size) {
        super(target, size);
    }

    /**
     * constructor
     * @param target target class
     * @param size initial array size
     */
    public WritableCachedMemoryArrayList(Class<E> target, E defaultValue, int size) {
        super(target, defaultValue, size);
    }

    public WritableCachedMemoryArrayList(Class<E> target, int size, int resizeCapacity) {
        super(target, size, resizeCapacity);
    }

    /**
     * constructor
     * @param target target class
     * @param size initial array size
     * @param resizeCapacity resize capacity size
     */
    @SuppressWarnings("unchecked")
    public WritableCachedMemoryArrayList(Class<E> target, E defaultValue, int size, int resizeCapacity) {
        super(target, defaultValue, size, resizeCapacity);
    }

    public WritableCachedMemoryArrayList(Class<E> target, E[] val, int resizeCapacity) {
        super(target, val, resizeCapacity);
    }

    /**
     * constructor
     * @param target target class
     * @param val initial val array
     * @param resizeCapacity resize capacity size
     */
    public WritableCachedMemoryArrayList(Class<E> target, E defaultValue, E[] val, int resizeCapacity) {
        super(target, defaultValue, val, resizeCapacity);
    }

    @Override
    public void save(SeparableIndex<E> index) throws IOException {
        index.save(this.iterator(), this.size(), target);
    }

    @Override
    public void load(SeparableIndex<E> index) {
        // copy all values
        E[] val = (E[]) Array.newInstance(target, index.getItemSize());
        IndexIterator<E> iterator = new IndexIterator<>(target, index, defaultValue);
        while(iterator.hasNext()) {
            val[iterator.index()] = iterator.next();
        }

        this.size = val.length;
        this.allocateSize = val.length;
        this.lastOffset = 0;
        this.vals = new ArrayList<>();
        this.vals.add(val);
        this.valIndex = 0;
        this.offset = 0;
        this.val = vals.get(0);
    }
}
