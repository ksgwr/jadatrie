package jp.ksgwr.jadatrie;

import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

public class DoubleArrayTrie<T> {

	//Suppose that there are n nodes in the trie, and the alphabet is of size m. The size of the double-array structure would be n + cm, where c is a coefficient which is dependent on the characteristic of the trie. And the time complexity of the brute force algorithm would be O(nm + cm2).
	private static class Unit {
		int base;
		int check;

		public Unit(int base,int check) {
			this.base = base;
			this.check = check;
		}

		public String toString() {
			return "Unit(" + base + "," + check + ")";
		}
	}

	private static class CharFreq {

		int code;
		int count;

		public CharFreq(int code) {
			this.code = code;
		}

		public String toString() {
			return "c:" + code + "(" + (char) code + ")," + count;
		}
	}

	private Class<T> target;

	private Unit units[];

	// UTF-16だと圧縮率が悪い、頻度順にソートして高い頻度に低い番号をつけると圧縮率が良い
	private int[] codes;

	private int codeLength;

    private String keys[];
    private T vals[];

    // baseの値は衝突を考えないようにするために、常にincrementsしていく
    private int currentPos;

    // dummy node
    private final Node tmpnode = new Node(0,0,0);

    public DoubleArrayTrie(Class<T> target) {
    	this.target = target;
    }

    public void build(TreeMap<String, T> entries) {
    	build(entries.entrySet().iterator(), entries.size());
    }

    public void build(String[] key, T[] val) {
    	setKeyValueArray(key, val);
    	build(new DoubleArrayIterator<T>(key, val), key.length);
    }

    public void build(List<String> key, List<T> val) {
    	build(new DoubleListIterator<T>(key, val), key.size());
    }

    protected void setKeyValueArray(String[] key, T[] val) {
    	this.keys = key;
    	this.vals = val;
    }

    @SuppressWarnings("unchecked")
	protected void build(Iterator<Entry<String,T>> entries, int size) {
    	if (this.keys == null) {
    		this.keys = new String[size];
        	this.vals = (T[]) Array.newInstance(target, size);
    	}
    	CharFreq[] cfs = new CharFreq[Character.MAX_CODE_POINT];
    	List<Node> siblings = new ArrayList<Node>(size);
    	if (entries.hasNext()) {
    		Entry<String, T> entry = entries.next();
    		String key = entry.getKey();
    		this.keys[0] = key;
			if (this.vals != null) {
				this.vals[0] = entry.getValue();
			}

    		int i = 1;
    		int cur = key.charAt(0);
    		int prevc = cur;
        	int previ = 0;

        	// code count
			for (int j = 0; j < key.length(); j++) {
				int c = key.charAt(j);
				if (cfs[c] == null) {
					cfs[c] = new CharFreq(c);
				} else {
					cfs[c].count++;
				}
			}

    		while(entries.hasNext()) {
        		entry = entries.next();
        		key = entry.getKey();
        		this.keys[i] = key;
        		if (this.vals != null) {
        			this.vals[i] = entry.getValue();
        		}

        		cur = key.charAt(0);
        		for (int j = 0; j < key.length(); j++) {
    				int c = key.charAt(j);
    				if (cfs[c] == null) {
    					cfs[c] = new CharFreq(c);
    				} else {
    					cfs[c].count++;
    				}
    			}

        		if (prevc != cur) {
        			siblings.add(new Node(prevc, previ, i));
        			prevc = cur;
        			previ = i;
        		}
        		i++;
        	}
    		siblings.add(new Node(cur, previ, i));

    	}
    	Arrays.sort(cfs, new Comparator<CharFreq>() {
			@Override
			public int compare(CharFreq o1, CharFreq o2) {
				if (o1 == null && o2 == null) {
					return 0;
				} else if (o1 == null) {
					return 1;
				} else if (o2 == null) {
					return -1;
				}
				return o2.count - o1.count;
			}
		});
    	this.codes = new int[Character.MAX_CODE_POINT];
    	int num = 1;
		for (CharFreq cf : cfs) {
			if (cf == null) {
				break;
			}
			this.codes[cf.code] = num++;
		}
		this.codeLength = num;
		cfs = null;
		for (Node node : siblings) {
			node.code = codes[node.code];
		}

    	// List<Node>を範囲(潜在ノード数)でソートすると効率が良い
    	// 文字列長にも影響するので完璧ではない
    	this.units = new Unit[codeLength * size];
    	units[0] = new Unit(1, 0);

    	this.currentPos = 0;
    	insert(siblings, 0);

    }

    private void fetch(EfficientNodeList newSiblings, Node root, int depth) {
    	newSiblings.clear(root.right - root.left);

    	int i = root.left;
    	int cur;
    	int prevc;
    	int previ;
    	if (i < root.right) {
    		String key = keys[i];
    		int len = key.length();
    		if (depth < len) {
    			cur = codes[key.charAt(depth)];
    		} else if (len == depth){
    			// end string
    			cur = 0;
    		} else {
    			return;
    		}
    		prevc = cur;
			previ = i;
    		i++;

    		while(i < root.right) {
        		key = keys[i];
        		cur = codes[key.charAt(depth)];

        		if(prevc != cur) {
        			tmpnode.code = prevc;
        			tmpnode.left = previ;
        			tmpnode.right = i;
        			newSiblings.add(tmpnode);
        			prevc = cur;
        			previ = i;
        		}
        		i++;
        	}

			tmpnode.code = cur;
			tmpnode.left = previ;
			tmpnode.right = i;
			newSiblings.add(tmpnode);

    	}
    }

    private int insert(List<Node> siblings, int depth) {
    	loop: while(true) {
    		currentPos++;
    		/*if (units[begin] != null) {
    			continue loop;
    		}*/
    		for (Node node:siblings) {
    			Unit unit = units[currentPos + node.code];
    			if (unit != null && unit.check != 0) {
    				continue loop;
    			}
    		}
    		break;
    	}
    	int base = currentPos;
    	for (Node node:siblings) {
    		units[base + node.code] = new Unit(0, base);
    	}

    	int newDepth = depth + 1;
    	EfficientNodeList newSiblings = new EfficientNodeList();
    	for (Node node:siblings) {
    		// 子のリストのオブジェクトは可能な限り使い回す
    		fetch(newSiblings, node, newDepth);
    		if (newSiblings.size()==0) {
    			// 終端のindexの値をマイナス値に変換
    			units[base + node.code].base = -node.left - 1;
    		} else {
    			units[base + node.code].base = insert(newSiblings, newDepth);
    		}
    	}

    	return base;
    }

    public SearchResult commonPrefixSearch(String target) {
    	return commonPrefixSearch(target, 0, target.length(), 0);
    }

    public SearchResult commonPrefixSearch(String target, int start, int end, int nodePos) {
    	List<Integer> results = new ArrayList<Integer>();

    	Unit unit;
    	int base = units[nodePos].base;
    	int next;
    	int i = start;
		for (i = start; i < end; i++) {
			unit = units[base];
    		// check end of string
    		if (unit != null && (next = unit.base) < 0) {
    			results.add(-next - 1);
    		}
    		int c = codes[target.charAt(i)];
    		if (c == 0) {
    			return new SearchResult(results, base, i);
    		}
    		next = base + c;
    		unit = units[next];
    		if (unit != null && base == unit.check) {
    			base = unit.base;
    		} else {
    			return new SearchResult(results, base, i);
    		}
    	}
		// 最後の文字の終端チェック
    	if ((unit = units[base]) != null && (next = unit.base) < 0) {
    		results.add(-next - 1);
    	}

		return new SearchResult(results, base, i);
    }

    public SearchResult exactMatch(String target) {
    	return exactMatch(target, 0, target.length(), 0);
    }

    public SearchResult exactMatch(String target, int start, int end, int nodePos) {
    	List<Integer> results = new ArrayList<Integer>();

    	Unit unit;
    	int base = units[nodePos].base;
    	int next;
    	int i;
    	for (i = start; i < end; i++) {
    		int c = codes[target.charAt(i)];
    		if (c == 0) {
    			return new SearchResult(results, base, i);
    		}
    		next = base + c;
    		unit = units[next];
    		if (unit != null && base == unit.check) {
    			base = unit.base;
    		} else {
    			return new SearchResult(results, base, i);
    		}
    	}
    	if ((unit = units[base]) != null && (next = unit.base) < 0) {
    		results.add(-next - 1);
    	}

    	return new SearchResult(results, base, i);
    }

    public SearchResult exactSpellerMatch(String target) {
    	return exactSpellerMatch(target, 1);
    }

    public SearchResult exactSpellerMatch(String target, int maxDist) {
    	return exactSpellerMatch(target, maxDist, true, true, true);
    }

    public SearchResult exactSpellerMatch(String target, int maxDist, boolean canAdd, boolean canDelete, boolean canReplace) {
    	return exactSpellerMatch(target, maxDist, canAdd, canDelete, canReplace, 0, target.length(), 0);
    }

    /**
     * 必要な結果はIDと編集距離、またadd,deleteなどのフラグがあると良いが編集距離1でないとあまり意味が無い
     *
     * @param target
     * @param maxDist
     * @param canAdd
     * @param canDelete
     * @param canReplace
     * @param start
     * @param end
     * @param nodePos
     * @return
     */
    public SearchResult exactSpellerMatch(String target, int maxDist, boolean canAdd, boolean canDelete, boolean canReplace, int start, int end, int nodePos) {
    	List<Integer> results = new ArrayList<Integer>();

    	boolean isCheck = true;
    	Unit unit, tmpUnit = null;
    	int base = units[nodePos].base, tmpBase;
    	int next, tmpNext;
    	int i, c, tmpc;
    	for (i = start; i < end; i++) {

    		c = codes[target.charAt(i)];

    		if (c != 0 && maxDist > 0 && canDelete) {
				for (int j = 0; j < codeLength; j++) {
    				if (j == c) {
    					// 通常遷移はスキップ
    					continue;
    				}
    				tmpNext = base + j;
    				tmpUnit = units[tmpNext];
    				if (tmpUnit != null && base == tmpUnit.check) {
    					// 次の遷移をチェックすることで無駄な関数呼び出しを防ぐ
    					tmpBase = tmpUnit.base;
    					tmpNext = tmpBase + c;
    					tmpUnit = units[tmpNext];
    					if (tmpUnit != null && tmpBase == tmpUnit.check) {
    						// 任意の文字を削除した場合の次の遷移先が見つかった
    						SearchResult tmpResult = exactSpellerMatch(target,maxDist - 1, canAdd, canDelete, canReplace,i + 1, end, tmpNext);
    						results.addAll(tmpResult.ids);
    					}
    				}
    			}
			}


    		next = base + c;
    		unit = units[next];

    		isCheck = false;
    		if(i + 1 == end && maxDist > 0 && canAdd) {
    			//last char
				tmpUnit = units[base];
				if (tmpUnit != null && (tmpNext = tmpUnit.base) < 0) {
					results.add(-tmpNext -1);
				}
    		}


    		if (maxDist > 0 && canReplace) {
    			// get prev children
				// check next c == children.c
				// 後でcanAddと重複部分をまとめる
				if (i + 1 < end) {
					tmpc = codes[target.charAt(i + 1)];
					if (tmpc != 0) {
						// 次の遷移先候補をcode分、線形探索して出す
						// その候補の次がnext cと一致するものに対して関数を呼び出す
						for (int j = 0; j < codeLength; j++) {
							if (j == c) {
								// 通常遷移はスキップ
								continue;
							}
							tmpNext = base + j;
							tmpUnit = units[tmpNext];
							if (tmpUnit != null && base == tmpUnit.check) {
								tmpBase = tmpUnit.base;
								tmpNext = tmpBase + tmpc;
								tmpUnit = units[tmpNext];
								if (tmpUnit != null && tmpBase == tmpUnit.check) {
									// ひとつ先の遷移先が見つかった
									SearchResult tmpResult = exactSpellerMatch(target,
											maxDist - 1, canAdd, canDelete, canReplace,
											i + 2, end, tmpNext);
									results.addAll(tmpResult.ids);
								}
							}
						}

					}
				} else {
					// last char
					for (int j = 0; j < codeLength; j++) {
						if (j == c) {
							// 通常遷移はスキップ
							continue;
						}
						tmpNext = base + j;
						tmpUnit = units[tmpNext];
						if (tmpUnit != null && base == tmpUnit.check && (tmpBase = tmpUnit.base) >= 0 &&
	    						(tmpUnit = units[tmpBase]) != null && (tmpNext = tmpUnit.base) < 0) {
							results.add(-tmpNext - 1);
						}
					}
				}
    		}

    		if (c != 0 && unit != null && base == unit.check) {
    			// 遷移成功の場合
    			base = unit.base;
    			isCheck = true;
    		} else if (maxDist > 0) {
    			// 遷移失敗の場合
    			// 1個先を見て繋がる候補を出す(ことによって計算量を大幅に削減する)
    			// その代わり「aa」「axxa」の様なパターンに対応できない
    			// 繋がったら後は通常のメソッドに渡して残りはO(1)で出す
    			// 全体の計算量は木の分岐数 3m << n(データ数)
    			if (canAdd) {
    				// 最後の文字は遷移成功判定にかかわらず行う必要があり
    				// それ以外は遷移失敗時のみ行えば良い。（少なくともcanAddの場合）
    				// get children
    				// check next c == children.c
					if (i + 1 < end) {
						c = codes[target.charAt(i + 1)];
						if (c != 0) {
							tmpNext = base + c;
							tmpUnit = units[tmpNext];
							if (tmpUnit != null && base == tmpUnit.check) {
								SearchResult tmpResult = exactSpellerMatch(target,
										maxDist - 1, canAdd, canDelete,
										canReplace, i + 2, end, tmpNext);
								results.addAll(tmpResult.ids);
							}
						}
					}
    			}
    			return new SearchResult(results, base, i);
    		} else {
    			return new SearchResult(results, base, i);
    		}
    	}
    	if (isCheck) {
    		if (maxDist > 0 && canDelete) {
    			// 最後の文字を消す場合（終端にたどり着くかをチェック)
				for (int j = 0; j < codeLength; j++) {
					tmpNext = base + j;
    				tmpUnit = units[tmpNext];
    				if (tmpUnit != null && base == tmpUnit.check && (tmpBase = tmpUnit.base) >= 0 &&
    						(tmpUnit = units[tmpBase]) != null && (tmpNext = tmpUnit.base) < 0) {
    					results.add(-tmpNext - 1);
    				}
				}
    		}

    		if ((unit = units[base]) != null && (next = unit.base) < 0) {
    			results.add(-next - 1);
    		}
    	}
    	return new SearchResult(results, base, i);
    }

	protected final void collectChildren(ArrayDeque<Integer> queue, int base) {
		Unit unit;
		int next;
		for (int i = 1; i < codeLength; i++) {
			next = base + i;
			unit = units[next];
			if (unit != null && base == unit.check) {
				queue.add(units[next].base);
			}
		}
	}

    public List<Integer> predictiveSearch(String target) {
    	return predictiveSearch(target, 0, target.length(), 0);
    }

    public List<Integer> predictiveSearch(String target, int start, int end, int nodePos) {
    	SearchResult result = exactMatch(target, start, end, nodePos);
    	List<Integer> ids = new ArrayList<Integer>();
    	if (result.i != end) {
    		// 末端ノードに到達していない
    		return ids;
    	}
    	ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
    	queue.add(result.base);

		while (!queue.isEmpty()) {
			int base = queue.poll();
			collectId(ids, base);
			collectChildren(queue, base);
		}

    	return ids;
    }

    /**
     * baseがマイナス値のものを収集する
     * @param ids
     * @param base
     */
	protected final void collectId(List<Integer> ids, int nodePos) {
		Unit unit = units[nodePos];
		int next;
		if (unit != null && (next = unit.base) < 0) {
			ids.add(-next - 1);
		}
	}

    public List<Entry<String,T>> indexOf(String target, int[] pos, boolean startStrict, boolean endStrict, boolean longestStrict) {

    	return null;
    }

    public int[] createCodeMap() {
    	int[] map = new int[codeLength];
		for (int i = 0; i < codes.length; i++) {
			if (codes[i] > 0) {
				map[codes[i]] = i;
			}
		}
    	return map;
    }


    /**
     * for debug
     * @param target
     * @return
     */
    public int walkNode(String target) {
    	return walkNode(target, 0, target.length(), 0);
    }

    public int walkNode(String target, int start, int end, int nodePos) {
    	Unit unit;
    	int base = units[nodePos].base;
    	int next;
    	int i;
    	for (i = start; i < end; i++) {
    		int c = codes[target.charAt(i)];
    		if (c == 0) {
    			return -1;
    		}
    		next = base + c;
    		unit = units[next];
    		if (unit != null && base == unit.check) {
    			base = unit.base;
    		} else {
    			return -1;
    		}
    	}
    	if ((unit = units[base]) != null && (next = unit.base) < 0) {
    		return base;
    	}
    	return i;
    }

    /**
     * for debug
     * @param nodePos
     * @return
     */
    public String traverseNode(int nodePos) {
    	return traverseNode(nodePos, createCodeMap());
    }

    public String traverseNode(int nodePos, int[] codeMap) {
    	StringBuilder sb = new StringBuilder();
    	traveseNodeResult(sb, nodePos, codeMap);
    	return sb.reverse().toString();
    }

    protected void traveseNodeResult(StringBuilder sb, int nodePos, int[] codeMap) {
		if (nodePos == 0) {
			return;
		}
		int startNodePos = nodePos;
    	int check = units[nodePos].check;
    	Unit unit;
		for (; 0 <= nodePos; nodePos--) {
			if ((unit = units[nodePos]) != null && unit.base == check) {
				break;
			}
		}

    	int c = startNodePos - units[nodePos].base;
    	if (c == 0) {
    		sb.append('#');
    	} else {
    		sb.append((char)codeMap[c]);
    	}
    	traveseNodeResult(sb, nodePos, codeMap);
    }
}
