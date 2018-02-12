package jp.ksgwr.jadatrie.core;

import java.util.List;

@FunctionalInterface
public interface ResizeStrategy {

    int resize(int currentSize, int currentPos, int maxCode, List<Unit> units, List<String> keys, List<Node> siblings);
}
