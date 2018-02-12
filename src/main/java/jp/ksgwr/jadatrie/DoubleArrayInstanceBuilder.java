package jp.ksgwr.jadatrie;

import jp.ksgwr.array.DiskArrayList;
import jp.ksgwr.array.WritableCachedMemoryArrayList;
import jp.ksgwr.array.WritableExArrayList;
import jp.ksgwr.array.index.InfoDynamicSegmentIndex;
import jp.ksgwr.array.index.InfoSegmentIndexer;
import jp.ksgwr.array.index.ObjectStreamIndexer;
import jp.ksgwr.jadatrie.core.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class DoubleArrayInstanceBuilder<VALUE> {

    private WritableExArrayList<Unit> units;

    private WritableExArrayList<Integer> codes;

    private DoubleArrayTrieBuildListener listener;

    private InitializeStrategy initializeStrategy;

    private PositionStrategy positionStrategy;

    private ResizeStrategy resizeStrategy;

    private List<String> keys;

    private List<VALUE> vals;

    private File indexDirectory;

    private InfoSegmentIndexer indexer;

    public DoubleArrayInstanceBuilder() {
    }

    public DoubleArrayTrie<VALUE> createInstance() throws IOException {
        initializeUnsetValue();;

        DoubleArrayTrie<VALUE> datrie = new DoubleArrayTrie<>(this);
        datrie.setKeyValue(keys, vals);

        if (indexDirectory != null) {
            if (indexer == null) {
                datrie.load(indexDirectory);
            } else {
                datrie.load(indexDirectory, indexer);
            }
        }

        return datrie;
    }

    protected DoubleArrayInstanceBuilder<VALUE> initializeUnsetValue() {
        if (indexDirectory == null) {
            if (units == null) {
                units = new WritableCachedMemoryArrayList<>(Unit.class, 0);
            }
            if (codes == null) {
                codes = new WritableCachedMemoryArrayList<>(Integer.class, (Integer) 0, Character.MAX_CODE_POINT);
            }
        }
        if (initializeStrategy == null) {
            initializeStrategy = new SimpleInitializeStrategy();
        }
        if (positionStrategy == null) {
            positionStrategy = new SimplePositionStrategy();
        }
        if (resizeStrategy == null) {
            resizeStrategy = new SimpleResizeStrategy();
        }
        return this;
    }

    protected WritableExArrayList<Unit> getUnits() {
        return units;
    }

    protected WritableExArrayList<Integer> getCodes() {
        return codes;
    }

    protected DoubleArrayTrieBuildListener getListener() {
        return listener;
    }

    protected InitializeStrategy getInitializeStrategy() {
        return initializeStrategy;
    }

    protected PositionStrategy getPositionStrategy() {
        return positionStrategy;
    }

    protected ResizeStrategy getResizeStrategy() {
        return resizeStrategy;
    }

    public DoubleArrayInstanceBuilder<VALUE> setCodes(WritableExArrayList<Integer> codes) {
        this.codes = codes;
        return this;
    }

    public DoubleArrayInstanceBuilder<VALUE> setUnitsExArray(WritableExArrayList<Unit> units) {
        this.units = units;
        return this;
    }

    public DoubleArrayInstanceBuilder<VALUE> setUnitsFixDiskArray(File directory, int separateSize) {
        this.units = new DiskArrayList<>(Unit.class, directory, 0, separateSize);
        return this;
    }

    public DoubleArrayInstanceBuilder<VALUE> setUnitsDynamicDiskArray(File directory) {
        this.units = new DiskArrayList<>(Unit.class, new InfoDynamicSegmentIndex<>(directory, "", new ObjectStreamIndexer<>()), 0);
        return this;
    }

    public DoubleArrayInstanceBuilder<VALUE> setBuildListener(DoubleArrayTrieBuildListener listener) {
        this.listener = listener;
        return this;
    }

    public DoubleArrayInstanceBuilder<VALUE> setInitializeStrategy(InitializeStrategy initializeStrategy) {
        this.initializeStrategy = initializeStrategy;
        return this;
    }

    public DoubleArrayInstanceBuilder<VALUE> setPositionStrategy(PositionStrategy positionStrategy) {
        this.positionStrategy = positionStrategy;
        return this;
    }

    public DoubleArrayInstanceBuilder<VALUE> setResizeStrategy(ResizeStrategy resizeStrategy) {
        this.resizeStrategy = resizeStrategy;
        return this;
    }

    public DoubleArrayInstanceBuilder<VALUE> loadIndex(File indexDirectory) {
        this.indexDirectory = indexDirectory;
        return this;
    }

    public DoubleArrayInstanceBuilder<VALUE> loadIndex(File indexDirectory, InfoSegmentIndexer indexer) {
        this.indexDirectory = indexDirectory;
        this.indexer = indexer;
        return this;
    }

    public DoubleArrayInstanceBuilder<VALUE> setKeys(List<String> keys) {
        this.keys = keys;
        return this;
    }

    public DoubleArrayInstanceBuilder<VALUE> setValues(List<VALUE> vals) {
        this.vals = vals;
        return this;
    }
}
