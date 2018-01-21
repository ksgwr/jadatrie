package jp.ksgwr.array.util;

import java.util.List;

public class ArraySearchUtil {

    public static <T> int infimumBinarySearch(List<? extends Comparable<? super T>> list, T key) {
        int low = 0;
        int high = list.size() - 1;
        int preMid = 0;
        int preCmp = 0;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            Comparable<? super T> midVal = list.get(mid);
            // cmp < 0 = mid < key
            int cmp = midVal.compareTo(key);
            if (cmp == 0) {
                return mid;
            }
            if (cmp < 0) {
                low = mid + 1;
                if (low == preMid && preCmp > 0) {
                    return mid;
                }
            } else {
                high = mid - 1;
                if (high == preMid && preCmp < 0) {
                    return mid - 1;
                }
            }
            preMid = mid;
            preCmp = cmp;
        }
        return preMid;
    }
}
