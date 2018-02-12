package jp.ksgwr.jadatrie.core;

import java.util.List;

@FunctionalInterface
public interface InitializeStrategy {

    int initializeSize(int codeLength, int dataSize, List<String> keys);
}
