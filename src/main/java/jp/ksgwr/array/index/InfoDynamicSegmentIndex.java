package jp.ksgwr.array.index;

import jp.ksgwr.array.util.ArraySearchUtil;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class InfoDynamicSegmentIndex<T extends Serializable, Serializer, Deserializer> extends InfoSegmentIndex<T, Serializer, Deserializer> {

    protected List<Integer> offsets;

    public InfoDynamicSegmentIndex(File directory, String prefix, InfoSegmentIndexer<T, Serializer, Deserializer> indexer) {
        super(directory, prefix, indexer);
        offsets = new ArrayList<>();
        offsets.add(0);
    }

    @Override
    protected File getSegmentFile(int segmentNum) {
        return new File(directory, segPrefix + segmentNum);
    }

    @Override
    protected void loadInfo(InfoSegmentIndexer<T,Serializer,Deserializer> indexer, Deserializer deserializer) throws IOException {
        this.itemSize = indexer.deserializeIntProp(deserializer);
        this.segmentSize = indexer.deserializeIntProp(deserializer);
        int offsetSize = indexer.deserializeIntProp(deserializer);
        this.offsets = new ArrayList<>(offsetSize);
        for (int i = 0; i < offsetSize; i++) {
            offsets.add(indexer.deserializeIntProp(deserializer));
        }
    }

    @Override
    protected void saveInfo(InfoSegmentIndexer<T,Serializer,Deserializer> indexer, Serializer serializer) throws IOException {
        indexer.serializeIntProp(serializer, this.itemSize);
        indexer.serializeIntProp(serializer, this.segmentSize);
        indexer.serializeIntProp(serializer, this.offsets.size());
        for (Integer offset : offsets) {
            indexer.serializeIntProp(serializer, offset);
        }
    }

    @Override
    public int getSegmentNumber(int offset) {
        // offsetがでかければ次のsegmentを返す
        int segmentNum = ArraySearchUtil.infimumBinarySearch(offsets, offset);
        int lastOffsetNum = offsets.size() - 1;
        if (segmentNum == lastOffsetNum && offsets.get(lastOffsetNum) < offset) {
            segmentNum++;
        }
        return segmentNum;
    }

    @Override
    public int getOffset(int segmentNum) {
        if (segmentNum < offsets.size()) {
            return offsets.get(segmentNum);
        } else {
            return offsets.get(offsets.size() - 1);
        }
    }

    @Override
    public int getItemPerSegmentSize(int segmentNum) {
        if (segmentNum + 1 < offsets.size()) {
            return offsets.get(segmentNum + 1) - offsets.get(segmentNum);
        } else {
            return this.allocateSize - offsets.get(offsets.size() - 1) + 1;
        }
    }

    @Override
    public void increaseItemSize(int size) {
        this.itemSize = size;
        if (allocateSize < itemSize) {
            this.allocateSize = itemSize;
            offsets.add(size);
            this.segmentSize++;
        }
    }

    @Override
    public void decreaseItemSize(int size) {
        this.itemSize = size;
    }

    @Override
    public void increaseAllocateSize(int size) {
        this.allocateSize = size;
        if (offsets.get(offsets.size() - 1) < size) {
            this.offsets.add(size);
        }
        this.segmentSize = offsets.size() - 1;
    }

    @Override
    public void decreaseAllocateSize(int size) {
        this.allocateSize = size;
        for (int i = offsets.size() - 1; i >= 0; i--) {
            int offset = offsets.get(i);
            if (offset <= size) {
                break;
            } else if (i + 1 < offsets.size()) {
                offsets.remove(i + 1);
            }
        }
        this.segmentSize = offsets.size() - 1;
    }
}
