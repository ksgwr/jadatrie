package jp.ksgwr.jadatrie;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import jp.ksgwr.array.CachedMemoryArrayList;
import jp.ksgwr.array.ExArrayList;
import jp.ksgwr.jadatrie.core.CharFreq;
import jp.ksgwr.jadatrie.core.EfficientNodeList;
import jp.ksgwr.jadatrie.core.Node;
import jp.ksgwr.jadatrie.core.SearchResult;
import jp.ksgwr.jadatrie.core.Unit;

public class DoubleArrayTrie<T extends Serializable> {

	protected final Class<T> target;

	protected ExArrayList<Unit> units;

	// UTF-16だと圧縮率が悪い、頻度順にソートして高い頻度に低い番号をつけると圧縮率が良い
	protected ExArrayList<Integer> codes;

	protected int codeLength;

    protected List<String> keys;
    protected List<T> vals;

    protected DoubleArrayTrieBuildListener debugger;

    // baseの値は衝突を考えないようにするために、常にincrementsしていく
    private int currentPos;

    // dummy node
    private final Node tmpnode = new Node(0,0,0);

    public DoubleArrayTrie(Class<T> target) {
    	this(target, new CachedMemoryArrayList<Unit>(Unit.class, 0));
    }

    public DoubleArrayTrie(Class<T> target, ExArrayList<Unit> units) {
    	this(target, units, new CachedMemoryArrayList<Integer>(Integer.class, 0, Character.MAX_CODE_POINT, 1));
    }

    public DoubleArrayTrie(Class<T> target, ExArrayList<Unit> units, ExArrayList<Integer> codes) {
    	this.target = target;
    	this.units = units;
    	this.codes = codes;
    }

    public void setBuildListener(DoubleArrayTrieBuildListener listener) {
    	this.debugger = listener;
    }

    public Class<T> getValueClass() {
    	return target;
    }

    public int getDoubleArraySize() {
    	return units.size();
    }

    public int getKeySize() {
    	return keys.size();
    }

    public void build(TreeMap<String, T> entries) {
    	int size = entries.size();
    	this.keys = new CachedMemoryArrayList<String>(String.class, size, 1);
		this.vals = new CachedMemoryArrayList<T>(target, size, 1);
    	build(entries.entrySet().iterator(), size, true);
    }

    public void build(Iterator<Entry<String,T>> entries, int size, ExArrayList<String> emptyKey, ExArrayList<T> emptyVal) {
    	this.keys = emptyKey;
    	this.vals = emptyVal;
    	this.build(entries, size, true);
    }

    public void build(String[] key, T[] val) {
    	this.keys = new CachedMemoryArrayList<String>(String.class, key, 1);
    	if (val != null) {
    		this.vals = new CachedMemoryArrayList<T>(target, val, 1);
    	}
    	build(new DoubleArrayIterator<T>(key, val), key.length, false);
    }

    public void build(List<String> key, List<T> val) {
    	this.setKeyValue(key, val);
    	build(new DoubleListIterator<T>(key, val), key.size(), false);
    }

	protected void build(Iterator<Entry<String,T>> entries, int size, boolean requireInitKeyValue) {
    	// 文字列カウントと最初の文字が同じものでNodeの木構造を作成
    	CharFreq[] cfs = new CharFreq[Character.MAX_CODE_POINT];
    	List<Node> siblings = new ArrayList<Node>(size);
    	if (entries.hasNext()) {
    		Entry<String, T> entry = entries.next();
    		String key = entry.getKey();
			if (requireInitKeyValue) {
				this.keys.set(0, key);
				if (this.vals != null) {
					this.vals.set(0, entry.getValue());
				}
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
				if (requireInitKeyValue) {
					keys.set(i, key);
					if (this.vals != null) {
						vals.set(i, entry.getValue());
					}
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

    	// 文字とcodeの対応表を頻度によって作成する
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
    	this.codes.clear();
    	int num = 1;
		for (CharFreq cf : cfs) {
			if (cf == null) {
				break;
			}
			this.codes.set(cf.code, num++);
		}
		this.codeLength = num;
		cfs = null;
		for (Node node : siblings) {
			node.code = codes.get(node.code);
		}

    	// List<Node>を範囲(潜在ノード数)でソートすると効率が良い
    	// 文字列長にも影響するので完璧ではない
		int unitSize = codeLength * size;
		if (unitSize < 0) {
			unitSize = size;
		}
		this.units.clear();
		this.units.resize(unitSize);
    	units.set(0,  new Unit(1, 0));

    	this.currentPos = 0;
    	insert(siblings, 0);

    	units.compress();
    }

    private void fetch(EfficientNodeList newSiblings, Node root, int depth) {
    	newSiblings.clear(root.right - root.left);

    	int i = root.left;
    	int cur;
    	int prevc;
    	int previ;
    	if (i < root.right) {
    		String key = keys.get(i);
    		int len = key.length();
    		if (depth < len) {
    			cur = codes.get(key.charAt(depth));
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
        		key = keys.get(i);
        		cur = codes.get(key.charAt(depth));

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
    	int maxCode = 0;
    	for (Node node:siblings) {
    		if (maxCode < node.code) {
    			maxCode = node.code;
    		}
    	}
    	loop: while(true) {
    		currentPos++;
    		if (units.size() <= currentPos + maxCode) {
    			units.resize(units.size() + currentPos + maxCode + keys.size() - siblings.get(siblings.size() - 1).right);
    		}
    		for (Node node:siblings) {
    			Unit unit = units.get(currentPos + node.code);
    			if (unit != null && unit.check != 0) {
    				continue loop;
    			}
    		}
    		break;
    	}
    	int base = currentPos;
    	if (base == 18897452) {
    		System.out.println("test");
    	}
    	for (Node node:siblings) {
    		units.set(base + node.code, new Unit(0, base));
    	}

    	int count = 0;
    	int newDepth = depth + 1;
    	EfficientNodeList newSiblings = new EfficientNodeList();
    	for (Node node:siblings) {
    		if (newDepth == 1) {
    			System.out.println(count++ + ":" + System.currentTimeMillis());
    		}
    		// 子のリストのオブジェクトは可能な限り使い回す
    		fetch(newSiblings, node, newDepth);
    		int i = base + node.code;
    		Unit unit = units.get(i);
			if (newSiblings.size() == 0) {
    			// 終端のindexの値をマイナス値に変換
    			unit.base = -node.left - 1;
    		} else {
    			unit.base = insert(newSiblings, newDepth);
    		}
    		// 更新情報を伝えるため明示的にsetを呼び出す
			units.set(i, unit);
    	}

    	return base;
    }

    public SearchResult commonPrefixSearch(String target) {
    	return commonPrefixSearch(target, 0, target.length(), 0);
    }

    public SearchResult commonPrefixSearch(String target, int start, int end, int nodePos) {
    	List<Integer> results = new ArrayList<Integer>();

    	Unit unit;
    	int base = units.get(nodePos).base;
    	int next;
    	int i = start;
		for (i = start; i < end; i++) {
			unit = units.get(base);
    		// check end of string
    		if (unit != null && (next = unit.base) < 0) {
    			results.add(-next - 1);
    		}
    		int c = codes.get(target.charAt(i));
    		if (c == 0) {
    			return new SearchResult(results, base, i);
    		}
    		next = base + c;
    		unit = units.get(next);
    		if (unit != null && base == unit.check) {
    			base = unit.base;
    		} else {
    			return new SearchResult(results, base, i);
    		}
    	}
		// 最後の文字の終端チェック
    	if ((unit = units.get(base)) != null && (next = unit.base) < 0) {
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
    	int base = units.get(nodePos).base;
    	int next;
    	int i;
    	for (i = start; i < end; i++) {
    		int c = codes.get(target.charAt(i));
    		if (c == 0) {
    			return new SearchResult(results, base, i);
    		}
    		next = base + c;
    		unit = units.get(next);
    		if (unit != null && base == unit.check) {
    			base = unit.base;
    		} else {
    			return new SearchResult(results, base, i);
    		}
    	}
    	if ((unit = units.get(base)) != null && (next = unit.base) < 0) {
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
    	int base = units.get(nodePos).base, tmpBase;
    	int next, tmpNext;
    	int i, c, tmpc;
    	for (i = start; i < end; i++) {

    		c = codes.get(target.charAt(i));

    		if (c != 0 && maxDist > 0 && canDelete) {
				for (int j = 0; j < codeLength; j++) {
    				if (j == c) {
    					// 通常遷移はスキップ
    					continue;
    				}
    				tmpNext = base + j;
    				if (units.size() <= tmpNext) {
						continue;
					}
    				tmpUnit = units.get(tmpNext);
    				if (tmpUnit != null && base == tmpUnit.check) {
    					// 次の遷移をチェックすることで無駄な関数呼び出しを防ぐ
    					tmpBase = tmpUnit.base;
    					tmpNext = tmpBase + c;
    					if (units.size() <= tmpNext) {
    						continue;
    					}
    					tmpUnit = units.get(tmpNext);
    					if (tmpUnit != null && tmpBase == tmpUnit.check) {
    						// 任意の文字を削除した場合の次の遷移先が見つかった
    						SearchResult tmpResult = exactSpellerMatch(target,maxDist - 1, canAdd, canDelete, canReplace,i + 1, end, tmpNext);
    						results.addAll(tmpResult.ids);
    					}
    				}
    			}
			}


    		next = base + c;
    		if (units.size() <= next) {
				continue;
			}
    		unit = units.get(next);

    		isCheck = false;
    		if(i + 1 == end && maxDist > 0 && canAdd) {
    			//last char
				tmpUnit = units.get(base);
				if (tmpUnit != null && (tmpNext = tmpUnit.base) < 0) {
					results.add(-tmpNext -1);
				}
    		}


    		if (maxDist > 0 && canReplace) {
    			// get prev children
				// check next c == children.c
				// 後でcanAddと重複部分をまとめる
				if (i + 1 < end) {
					tmpc = codes.get(target.charAt(i + 1));
					if (tmpc != 0) {
						// 次の遷移先候補をcode分、線形探索して出す
						// その候補の次がnext cと一致するものに対して関数を呼び出す
						for (int j = 0; j < codeLength; j++) {
							if (j == c) {
								// 通常遷移はスキップ
								continue;
							}
							tmpNext = base + j;
							if (units.size() <= tmpNext) {
	    						continue;
	    					}
							tmpUnit = units.get(tmpNext);
							if (tmpUnit != null && base == tmpUnit.check) {
								tmpBase = tmpUnit.base;
								tmpNext = tmpBase + tmpc;
								if (units.size() <= tmpNext) {
		    						continue;
		    					}
								tmpUnit = units.get(tmpNext);
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
						if (units.size() <= tmpNext) {
    						continue;
    					}
						tmpUnit = units.get(tmpNext);
						if (tmpUnit != null && base == tmpUnit.check && (tmpBase = tmpUnit.base) >= 0 &&
	    						(tmpUnit = units.get(tmpBase)) != null && (tmpNext = tmpUnit.base) < 0) {
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
						c = codes.get(target.charAt(i + 1));
						if (c != 0) {
							tmpNext = base + c;
							if (units.size() <= tmpNext) {
	    						continue;
	    					}
							tmpUnit = units.get(tmpNext);
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
					if (units.size() <= tmpNext) {
						continue;
					}
    				tmpUnit = units.get(tmpNext);
    				if (tmpUnit != null && base == tmpUnit.check && (tmpBase = tmpUnit.base) >= 0 &&
    						(tmpUnit = units.get(tmpBase)) != null && (tmpNext = tmpUnit.base) < 0) {
    					results.add(-tmpNext - 1);
    				}
				}
    		}

    		if ((unit = units.get(base)) != null && (next = unit.base) < 0) {
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
			unit = units.get(next);
			if (unit != null && base == unit.check) {
				queue.add(units.get(next).base);
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
     * @param nodePos
     */
	protected final void collectId(List<Integer> ids, int nodePos) {
		Unit unit = units.get(nodePos);
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
		for (int i = 0; i < codes.size(); i++) {
			if (codes.get(i) > 0) {
				map[codes.get(i)] = i;
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
    	int base = units.get(nodePos).base;
    	int next;
    	int i;
    	for (i = start; i < end; i++) {
    		int c = codes.get(target.charAt(i));
    		if (c == 0) {
    			return -1;
    		}
    		next = base + c;
    		unit = units.get(next);
    		if (unit != null && base == unit.check) {
    			base = unit.base;
    		} else {
    			return -1;
    		}
    	}
    	if ((unit = units.get(base)) != null && (next = unit.base) < 0) {
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
    	int check = units.get(nodePos).check;
    	Unit unit;
		for (; 0 <= nodePos; nodePos--) {
			if ((unit = units.get(nodePos)) != null && unit.base == check) {
				break;
			}
		}

    	int c = startNodePos - units.get(nodePos).base;
    	if (c == 0) {
    		sb.append('#');
    	} else {
    		sb.append((char)codeMap[c]);
    	}
    	traveseNodeResult(sb, nodePos, codeMap);
    }

    public void save(File file) throws IOException {
    	DataOutputStream out = null;
		try {
			out = new DataOutputStream(new BufferedOutputStream(
					new FileOutputStream(file)));
			out.writeInt(units.size());
			// TODO: 末尾の未使用領域を削除すると後の効率的に良い
			for(Unit unit:units) {
				boolean isNotNull = unit != null;
				out.writeBoolean(isNotNull);
				if (isNotNull) {
					out.writeInt(unit.base);
					out.writeInt(unit.check);
				}
			}
			out.writeInt(codes.size());
			out.writeInt(codeLength);
			for(int i=0;i<codes.size();i++) {
				int code = codes.get(i);
				if (code > 0) {
					out.writeInt(i);
					out.writeInt(code);
				}
			}
		} finally {
			if (out != null)
				out.close();
		}
    }

    public void load(File file) throws IOException {
    	DataInputStream is = null;
		try {
			is = new DataInputStream(new BufferedInputStream(
					new FileInputStream(file)));

			int len = is.readInt();
			this.units = new CachedMemoryArrayList<Unit>(Unit.class, len);
			for (int i=0;i<len;i++) {
				boolean b = is.readBoolean();
				if (b) {
					int base = is.readInt();
					int check = is.readInt();
					units.set(i, new Unit(base, check));
				}
			}
			len = is.readInt();
			this.codes = new CachedMemoryArrayList<Integer>(Integer.class, len);
			this.codeLength = is.readInt();
			len = this.codeLength - 1;
			for (int i=0;i<len;i++) {
				int idx = is.readInt();
				int code = is.readInt();
				this.codes.set(idx, code);
			}
		} finally {
			if (is != null)
				is.close();
		}
    }

    public void setKeyValue(Iterator<Entry<String,T>> entries, int size) {
    	this.setKeyValue(entries, size, new CachedMemoryArrayList<String>(String.class, size, 1), new CachedMemoryArrayList<T>(target, size, 1));
    }

	public void setKeyValue(Iterator<Entry<String,T>> entries, int size, ExArrayList<String> emptyKeys, ExArrayList<T> emptyVals) {
    	this.keys = emptyKeys;
    	this.vals = emptyVals;
    	int i = 0;
    	while(entries.hasNext()) {
    		Entry<String,T> entry = entries.next();
    		this.keys.set(i, entry.getKey());
    		this.vals.set(i, entry.getValue());
    		i++;
    	}
    }

	public void setKeyValue(List<String> keys, List<T> vals) {
		this.keys = keys;
		this.vals = vals;
	}

    public void setKeyValue(String[] keys, T[] vals) {
    	List<T> valList = vals == null ? null : new CachedMemoryArrayList<T>(target, vals, 1);
    	this.setKeyValue(new CachedMemoryArrayList<String>(String.class, keys, 1), valList);
    }

    public void close() throws IOException {
    	units.close();
    	codes.close();
    }
}
