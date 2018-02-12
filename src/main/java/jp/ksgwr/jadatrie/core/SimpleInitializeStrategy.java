package jp.ksgwr.jadatrie.core;

import java.util.List;

public class SimpleInitializeStrategy implements InitializeStrategy {

    @Override
    public int initializeSize(int codeLength, int dataSize, List<String> keys) {
        int unitSize = codeLength * dataSize;
        if (unitSize < 0) {
            //桁あふれ時
            unitSize = dataSize * 2;
            if (unitSize < 0) {
                unitSize = dataSize;
            }
        }
        return unitSize;
    }
}
