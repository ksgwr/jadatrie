package jp.ksgwr.jadatrie.core;

import java.util.List;

public class SimpleResizeStrategy implements ResizeStrategy {

    public SimpleResizeStrategy() {

    }

    @Override
    public int resize(int currentSize, int currentPos, int maxCode, List<Unit> units, List<String> keys, List<Node> siblings) {
        // right expression : expect require size from current progress
        // min 2x for increasing gradually size
        double rate = Math.min(2.0, (double)keys.size() / siblings.get(0).left);
        return (int) Math.floor((double)currentSize * rate);
    }
}
