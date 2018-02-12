package jp.ksgwr.jadatrie.core;

import java.util.List;

public class SimplePositionStrategy implements PositionStrategy {

    public SimplePositionStrategy() {

    }

    @Override
    public int startPosition(int currentPos, int maxCode, List<Unit> units, List<String> keys, List<Node> siblings) {
        return currentPos;
    }
}
