package jp.ksgwr.array;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class TextIntegerIndexer implements InfoSegmentIndexer<Integer, BufferedWriter, BufferedReader> {

	private static final String NULL = "null";
	
	public TextIntegerIndexer() {
		
	}
	
	@Override
	public void closeSerializer(BufferedWriter serializer) throws IOException {
		serializer.close();
	}

	@Override
	public BufferedWriter openSerializer(File file, boolean isSegment) throws IOException {
		return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "utf-8"));
	}

	@Override
	public void closeDeserializer(BufferedReader deserializer) throws IOException {
		deserializer.close();
	}

	@Override
	public BufferedReader openDeserializer(File file, boolean isSegment) throws IOException {
		return new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf-8"));
	}

	@Override
	public void serializeSegment(BufferedWriter serializer, Integer[] val) throws IOException {
		for (Integer v : val) {
			if (v == null) {
				serializer.write(NULL);
			} else {
				serializer.write(v.toString());
			}
			serializer.newLine();
		}
	}

	@Override
	public void serializeIntProp(BufferedWriter serializer, int val) throws IOException {
		serializer.write(Integer.toString(val));
		serializer.newLine();
	}

	@Override
	public void deserializeSegment(BufferedReader deserializer, Integer[] val) throws IOException, ClassNotFoundException {
		for (int i=0;i<val.length;i++) {
			String line = deserializer.readLine();
			if (line == null) {
				return;
			}
			if (!NULL.equals(line)) {
				val[i] = Integer.valueOf(line);
			}
		}
	}

	@Override
	public int deserializeIntProp(BufferedReader deserializer) throws IOException {
		return Integer.valueOf(deserializer.readLine());
	}

}
