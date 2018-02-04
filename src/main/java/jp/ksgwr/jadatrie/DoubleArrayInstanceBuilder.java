package jp.ksgwr.jadatrie;

import jp.ksgwr.array.ExArrayList;
import jp.ksgwr.array.IndexableExArrayList;
import jp.ksgwr.jadatrie.core.Unit;

import java.util.List;

public class DoubleArrayInstanceBuilder<VALUE> {

    private IndexableExArrayList<Unit> units;

    private IndexableExArrayList<Integer> codes;

    private DoubleArrayTrieBuildListener listener;

    private List<String> keys;

    private Class<VALUE> target;

    private List<VALUE> vals;

    public DoubleArrayInstanceBuilder(Class<VALUE> target) {
        this.target = target;
    }

    public DoubleArrayTrie<VALUE> createInstance() {
        DoubleArrayTrie<VALUE> datrie = new DoubleArrayTrie<>(target, null, null);
        datrie.units = units;
        datrie.codes = codes;

        return datrie;
    }

    public DoubleArrayInstanceBuilder setUnitsExArray(IndexableExArrayList<Unit> units) {
        this.units = units;
        return this;
    }

    public DoubleArrayInstanceBuilder setBuildListener(DoubleArrayTrieBuildListener listener) {
        this.listener = listener;
        return this;
    }
}
