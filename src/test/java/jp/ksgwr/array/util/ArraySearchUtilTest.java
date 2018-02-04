package jp.ksgwr.array.util;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArraySearchUtilTest {

    @Test
    public void infimumBinarySearchTest() {
        List<Integer> ary = new ArrayList<>(Arrays.asList(1,
                10,
                20,
                30,
                40,
                50));

        int actual;

        actual = ArraySearchUtil.infimumBinarySearch(ary, 0);
        assertEquals(0, actual);

        actual = ArraySearchUtil.infimumBinarySearch(ary, 1);
        assertEquals(0, actual);

        actual = ArraySearchUtil.infimumBinarySearch(ary, 5);
        assertEquals(0, actual);

        actual = ArraySearchUtil.infimumBinarySearch(ary, 15);
        assertEquals(1, actual);

        actual = ArraySearchUtil.infimumBinarySearch(ary, 25);
        assertEquals(2, actual);

        actual = ArraySearchUtil.infimumBinarySearch(ary, 35);
        assertEquals(3, actual);

        actual = ArraySearchUtil.infimumBinarySearch(ary, 45);
        assertEquals(4, actual);

        actual = ArraySearchUtil.infimumBinarySearch(ary, 50);
        assertEquals(5, actual);

        actual = ArraySearchUtil.infimumBinarySearch(ary, 55);
        assertEquals(5, actual);
    }

}
