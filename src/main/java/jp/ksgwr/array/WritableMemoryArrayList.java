package jp.ksgwr.array;

import jp.ksgwr.array.index.SeparableIndex;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class WritableMemoryArrayList<E extends Serializable> extends MemoryArrayList<E> implements WritableExArrayList<E> {

    public WritableMemoryArrayList(Class<E> target, int size) {
        super(target, size);
    }

    public WritableMemoryArrayList(Class<E> target, E defaultValue, int size) {
        super(target, defaultValue, size);
    }

    public WritableMemoryArrayList(Class<E> target, int size, int resizeCapacity) {
        super(target, size, resizeCapacity);
    }

    public WritableMemoryArrayList(Class<E> target, E defaultValue, int size, int resizeCapacity) {
        super(target, defaultValue, size, resizeCapacity);
    }

    public WritableMemoryArrayList(Class<E> target, E[] val, int resizeCapacity) {
        super(target, null, val, resizeCapacity);
    }

    public WritableMemoryArrayList(Class<E> target, E defaultValue, E[] val, int resizeCapacity) {
        super(target, defaultValue, val, resizeCapacity);
    }

    @Override
    public void save(SeparableIndex<E> index) throws IOException {
        index.save(this.iterator(), this.size(), target);
    }

    @SuppressWarnings("unchecked")
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
    }
}
