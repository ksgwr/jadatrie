package jp.ksgwr.jadatrie.core;

import jp.ksgwr.jadatrie.core.KeyValue;
import jp.ksgwr.jadatrie.core.KeyValueStringDeserializer;

public class SimpleKeyValueDeserializer implements KeyValueStringDeserializer<Boolean> {

    public SimpleKeyValueDeserializer() {

    }

    @Override
    public KeyValue<Boolean> createKeyValue(String str) {
        return new KeyValue<>(str, Boolean.TRUE);
    }
}
