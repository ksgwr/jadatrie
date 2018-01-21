package jp.ksgwr.jadatrie;

import jp.ksgwr.jadatrie.core.KeyValue;
import jp.ksgwr.jadatrie.core.KeyValueStringDeserializer;

public class SimpleKeyValueDeserializer implements KeyValueStringDeserializer<Boolean> {

    @Override
    public KeyValue<Boolean> createKeyValue(String str) {
        return new KeyValue<>(str, Boolean.TRUE);
    }
}
