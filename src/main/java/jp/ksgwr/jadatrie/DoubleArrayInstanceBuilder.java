package jp.ksgwr.jadatrie;

import javafx.geometry.Pos;
import jp.ksgwr.array.DiskArrayList;
import jp.ksgwr.array.ExArrayList;
import jp.ksgwr.array.IndexableCachedMemoryArrayList;
import jp.ksgwr.array.IndexableExArrayList;
import jp.ksgwr.array.index.InfoDynamicSegmentIndex;
import jp.ksgwr.array.index.ObjectStreamIndexer;
import jp.ksgwr.jadatrie.core.*;

import java.io.File;
import java.util.List;

public class DoubleArrayInstanceBuilder<VALUE> {

    private IndexableExArrayList<Unit> units;

    private IndexableExArrayList<Integer> codes;

    private DoubleArrayTrieBuildListener listener;

    private InitializeStrategy initializeStrategy;

    private PositionStrategy positionStrategy;

    private ResizeStrategy resizeStrategy;

    private List<String> keys;

    private Class<VALUE> target;

    private List<VALUE> vals;

    public DoubleArrayInstanceBuilder() {
    }

    public DoubleArrayTrie<VALUE> createInstance() {
        initializeUnsetValue();;

        DoubleArrayTrie<VALUE> datrie = new DoubleArrayTrie<>(this);
        datrie.debugger = listener;
        return datrie;
    }

    protected DoubleArrayInstanceBuilder<VALUE> initializeUnsetValue() {
        if (units == null) {
            units = new IndexableCachedMemoryArrayList<>(Unit.class, 0);
        }
        if (codes == null) {
            codes = new IndexableCachedMemoryArrayList<>(Integer.class, (Integer)0, Character.MAX_CODE_POINT);
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

    protected IndexableExArrayList<Unit> getUnits() {
        return units;
    }

    protected IndexableExArrayList<Integer> getCodes() {
        return codes;
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

    public DoubleArrayInstanceBuilder<VALUE> setUnitsExArray(IndexableExArrayList<Unit> units) {
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
}
