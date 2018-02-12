package jp.ksgwr.jadatrie;

import jp.ksgwr.jadatrie.core.Node;

import java.util.List;

/**
 * DoubleArrayTrieのビルド中のデバッグを可能にするためのリスナー
 *
 * @author ksgwr
 *
 */
public interface DoubleArrayTrieBuildListener {

    void initializeSize(int codeLength, int dataSize, List<String> keys, int unitSize);

    void resize(List<Node> siblings, int depth, int currentPos, int maxCode,int currentSize, int expandSize);

    void findBase(List<Node> siblings, int depth, int base);

    void compressSize(int compressSize);

}
