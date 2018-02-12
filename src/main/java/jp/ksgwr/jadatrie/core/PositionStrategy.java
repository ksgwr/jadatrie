package jp.ksgwr.jadatrie.core;

import java.util.List;

@FunctionalInterface
public interface PositionStrategy {

    int startPosition(int currentPos, int maxCode, List<Unit> units, List<String> keys, List<Node> siblings);
}
