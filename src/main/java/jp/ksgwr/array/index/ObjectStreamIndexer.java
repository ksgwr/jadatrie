package jp.ksgwr.array.index;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ObjectStreamIndexer<T> implements InfoSegmentIndexer<T, ObjectOutputStream, ObjectInputStream> {

	public ObjectStreamIndexer() {
		
	}
	
	@Override
	public void closeSerializer(ObjectOutputStream serializer) throws IOException {
		serializer.close();
	}

	@Override
	public ObjectOutputStream openSerializer(File file, boolean isSegment) throws IOException {
		return new ObjectOutputStream(new BufferedOutputStream(
				new FileOutputStream(file)));
	}

	@Override
	public void closeDeserializer(ObjectInputStream deserializer) throws IOException {
		deserializer.close();
	}

	@Override
	public ObjectInputStream openDeserializer(File file, boolean isSegment) throws IOException {
		return new ObjectInputStream(new BufferedInputStream(
				new FileInputStream(file)));
	}

	@Override
	public void serializeSegment(ObjectOutputStream serializer, T[] val) throws IOException {
		for (T v:val) {
			serializer.writeObject(v);
		}
	}

	@Override
	public void serializeIntProp(ObjectOutputStream serializer, int val) throws IOException {
		serializer.writeInt(val);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void deserializeSegment(ObjectInputStream deserializer, T[] val) throws IOException, ClassNotFoundException {
		for (int i=0;i<val.length;i++) {
			if (deserializer.available() <= 0) {
				break;
			}
			val[i] = (T) deserializer.readObject();
		}
	}

	@Override
	public int deserializeIntProp(ObjectInputStream deserializer) throws IOException {
		return deserializer.readInt();
	}

}
