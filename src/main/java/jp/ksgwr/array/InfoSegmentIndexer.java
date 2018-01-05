package jp.ksgwr.array;

import java.io.File;
import java.io.IOException;

public interface InfoSegmentIndexer<T, Serializer, Deserializer> {

	public void closeSerializer(Serializer serializer) throws IOException;
	
	public Serializer openSerializer(File file, boolean isSegment) throws IOException;
	
	public void closeDeserializer(Deserializer deserializer) throws IOException;
	
	public Deserializer openDeserializer(File file, boolean isSegment) throws IOException;
	
	public void serializeSegment(Serializer serializer, T[] val) throws IOException;
	
	public void serializeIntProp(Serializer serializer, int val) throws IOException;
	
	public void deserializeSegment(Deserializer deserializer, T[] val) throws IOException, ClassNotFoundException;
	
	public int deserializeIntProp(Deserializer deserializer) throws IOException;
	
}
