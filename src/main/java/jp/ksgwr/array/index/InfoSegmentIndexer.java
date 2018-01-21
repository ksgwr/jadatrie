package jp.ksgwr.array.index;

import java.io.File;
import java.io.IOException;

public interface InfoSegmentIndexer<T, Serializer, Deserializer> {

	void closeSerializer(Serializer serializer) throws IOException;

	Serializer openSerializer(File file, boolean isSegment) throws IOException;

	void closeDeserializer(Deserializer deserializer) throws IOException;

	Deserializer openDeserializer(File file, boolean isSegment) throws IOException;

	void serializeSegment(Serializer serializer, T[] val, int length) throws IOException;

	void serializeIntProp(Serializer serializer, int val) throws IOException;

	void deserializeSegment(Deserializer deserializer, T[] val, int length) throws IOException, ClassNotFoundException;

	int deserializeIntProp(Deserializer deserializer) throws IOException;

}
