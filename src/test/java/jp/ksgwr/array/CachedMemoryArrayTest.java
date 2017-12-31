package jp.ksgwr.array;


public class CachedMemoryArrayTest extends MemoryArrayTest {

	@Override
	protected ExArrayList<Integer> initExArrayList() throws Exception {
		return new CachedMemoryArrayList<Integer>(Integer.class, super.size);
	}
}
