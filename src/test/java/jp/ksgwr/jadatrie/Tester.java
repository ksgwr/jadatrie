package jp.ksgwr.jadatrie;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPInputStream;

public class Tester {

	public static void main(String[] args) throws Exception {
		File file = new File("data", "jawiki-latest-all-titles.gz");

		String line;
		BufferedReader br = TestReader.createReader(file);
		final int size = new TestReader(br).readRestLines();

		br = TestReader.createReader(file);
		final FileReadIterator<Boolean> ite = new TestReader(br);

		/*
		while(ite.hasNext()) {
			Entry<String, Boolean> entry = ite.next();
			String key = entry.getKey();
			if (key.indexOf("バベルの塔") >= 0) {
				System.out.println(key);
			}
		}
		*/


		final DoubleArrayTrie<Boolean> datrie = new DoubleArrayTrie<Boolean>(Boolean.class);

		System.out.println("load start");
		Thread t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					datrie.load(new File("data", "jawiki.idx"));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		Thread t2 = new Thread(new Runnable() {
			@Override
			public void run() {
				datrie.setKeyValue(ite, size);
			}
		});
		t1.start();
		t2.start();
		t1.join();
		t2.join();
		System.out.println("load finish");

		//datrie.build(ite, size);
		//datrie.save(new File("data","jawiki.idx"));

		System.out.println(datrie.getDoubleArraySize());
		System.out.println(datrie.exactMatch("バベルの塔").ids);



	}

	public static class TestReader extends FileReadIterator<Boolean> {

		boolean init;

		public TestReader(BufferedReader reader) {
			super(reader);
			this.init = true;
		}

		@Override
		public KeyValue<Boolean> readString(String line) {
			if (init) {
				this.init = false;
				return null;
			}
			return new KeyValue<Boolean>(line, Boolean.TRUE);
		}

		public static BufferedReader createReader(File file) throws UnsupportedEncodingException, FileNotFoundException, IOException {
			return new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file)), "UTF-8"));
		}

	}
}
