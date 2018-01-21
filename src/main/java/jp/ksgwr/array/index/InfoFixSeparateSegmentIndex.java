package jp.ksgwr.array.index;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

public class InfoFixSeparateSegmentIndex<T extends Serializable, Serializer, Deserializer> extends InfoSegmentIndex<T, Serializer, Deserializer> {

	protected int separateSize;

	public InfoFixSeparateSegmentIndex(File directory, String prefix, int separateSize, InfoSegmentIndexer<T, Serializer, Deserializer> indexer) {
		super(directory, prefix, indexer);
		this.separateSize = separateSize;
	}

	@Override
	protected File getSegmentFile(int segmentNum) {
		return new File(directory, segPrefix + segmentNum);
	}

	@Override
	protected void loadInfo(InfoSegmentIndexer<T,Serializer,Deserializer> indexer, Deserializer deserializer) throws IOException {
		this.itemSize = indexer.deserializeIntProp(deserializer);
		this.segmentSize = indexer.deserializeIntProp(deserializer);
		this.separateSize = indexer.deserializeIntProp(deserializer);
	}

	@Override
	protected void saveInfo(InfoSegmentIndexer<T,Serializer,Deserializer> indexer, Serializer serializer) throws IOException {
		indexer.serializeIntProp(serializer, this.itemSize);
		indexer.serializeIntProp(serializer, this.segmentSize);
		indexer.serializeIntProp(serializer, this.separateSize);
	}

	@Override
	public int getSegmentNumber(int offset) {
		return offset / separateSize;
	}

	@Override
	public int getOffset(int segmentNum) {
		return segmentNum * separateSize;
	}

	@Override
	public int getItemPerSegmentSize(int segmentNum) {
		return separateSize;
	}
}
