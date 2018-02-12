package jp.ksgwr.array.index;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Iterator;

public class ImmutableStreamIndex<T extends Serializable, Serializer, Deserializer> implements SeparableIndex<T> {

    private Serializer serializer;

    private Deserializer deserializer;

    private InfoSegmentIndexer<T,Serializer,Deserializer> indexer;

    private int size;

    public ImmutableStreamIndex(Serializer serializer, Deserializer deserializer, InfoSegmentIndexer<T,Serializer,Deserializer> indexer) {
        this.serializer = serializer;
        this.deserializer = deserializer;
        this.indexer = indexer;
    }

    @Override
    public int getItemSize() {
        return size;
    }

    @Override
    public int getAllocateSize() {
        return size;
    }

    @Override
    public int getSegmentSize() {
        return 1;
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
        return size;
    }

    @Override
    public T[] loadSegment(int segmentNum, Class<T> target) throws IOException, ClassNotFoundException {
        T[] vals = (T[]) Array.newInstance(target, size);
        indexer.deserializeSegment(deserializer, vals, vals.length);
        return vals;
    }

    @Override
    public void saveSegment(int segmentNum, T[] val, int length) throws IOException {
        indexer.serializeSegment(serializer, val, length);
    }

    @Override
    public void deleteSegment(int segmentNum) {

    }

    @Override
    public void increaseItemSize(int size) {
        this.size = size;
    }

    @Override
    public void decreaseItemSize(int size) {
        this.size = size;
    }

    @Override
    public void increaseAllocateSize(int size) {
        this.size = size;
    }

    @Override
    public void decreaseAllocateSize(int size) {
        this.size = size;
    }

    @Override
    public void saveInfo() throws IOException {
        indexer.serializeIntProp(serializer, size);
    }

    @Override
    public void loadInfo() throws IOException {
        size = indexer.deserializeIntProp(deserializer);
    }

    @Override
    public boolean isExist() {
        return true;
    }

    @Override
    public void cleanup() {

    }

    @Override
    public void save(Iterator<T> iterator, int size, Class<T> target) throws IOException {
        this.size = size;
        T[] vals = (T[]) Array.newInstance(target, size);
        int i = 0;
        while (iterator.hasNext()) {
            vals[i++] = iterator.next();
        }
        saveInfo();
        indexer.serializeSegment(serializer, vals, vals.length);
    }
}
