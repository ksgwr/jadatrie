package jp.ksgwr.jadatrie.core;

@FunctionalInterface
public interface KeyValueStringDeserializer<T> {

    KeyValue<T> createKeyValue(String str);
}
