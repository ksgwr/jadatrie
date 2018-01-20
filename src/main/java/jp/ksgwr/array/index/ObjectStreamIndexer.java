package jp.ksgwr.array.index;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
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
	public void serializeSegment(ObjectOutputStream serializer, T[] val, int length) throws IOException {
		for (int i = 0; i < length; i++) {
			serializer.writeObject(val[i]);
		}
	}

	@Override
	public void serializeIntProp(ObjectOutputStream serializer, int val) throws IOException {
		serializer.writeInt(val);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void deserializeSegment(ObjectInputStream deserializer, T[] val, int length) throws IOException, ClassNotFoundException {
		for (int i=0;i<length;i++) {
			try {
				val[i] = (T) deserializer.readObject();
			} catch (EOFException e) {
				// end of file
			}
		}
	}

	@Override
	public int deserializeIntProp(ObjectInputStream deserializer) throws IOException {
		return deserializer.readInt();
	}

}
