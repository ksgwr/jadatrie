# Java Double Array Trie Implementation [![Build Status](https://travis-ci.org/ksgwr/jadatrie.svg?branch=master)](https://travis-ci.org/ksgwr/jadatrie)
## 特徴

** ※本ライブラリは未完成です。現状では参考資料の実装を利用するのをお勧めします。 **

JavaのDoubleArrayTrieの実装です。既に世の中に同様のライブラリが多くありますが、
本ライブラリでは後述する高速化Tipsを取り入れた検索速度重視の実装にしています。
メモリ使用量などを重視する場合はDoubleArrayTrieでなく(trie4jにあるような)LODOSなどの使用を検討すると良いでしょう。
DoubleArrayTrieの最大の特徴としては、commonPrefixSearchが高速に行えることが挙げられます。長文中からの特徴語抽出などに活躍するでしょう。
そこで本ライブラリではこれら自然言語処理タスクに役立つメソッドを一部特化し提供します。例えばそこそこ効率の良い１文字違いのエントリの探索機能や形態素の区切りを考慮した探索機能を提供します。１文字違いのエントリを探索するにはSimStringなど転置索引を利用したより高速な手法がありますが、本ライブラリでは木構造という特徴を生かしそこそこ高速に検索する機能を提供します。ただし、特徴として追加・削除・置換をそれぞれ有効か無効にするかを選択することができるようにしています。
また、多くのライブラリで提供していない(そのためにラッパーを書くことが多い)キーに対するバリューを総称型で指定し取得する機能を標準で提供します。デメリットとして、単純な利用例でもバリュー分メモリを無駄に使ってしまうことがあるのは注意が必要です。

### 主要メソッド

* void build(TreeMap<String, T> entries)
  * インデックスを構築します。TreeMapを利用するためキーは自動でソートされます。
* commonPrefixSearch(String target)
  * targetのprefixが一致する全てのエントリを探索します。最後の要素は最長共通接頭辞となります。特徴後抽出などに利用できます。
* exactMatch(String target)
  * 完全一致するエントリを探索します。キーの存在確認などに利用します。
* predictiveSearch(String target)
  * targetをprefixに持つすべてのエントリを探索します。入力補助などに利用できます。
* exactSpellerMatch(String target)
  * 1文字違いのエントリを探索します。

## Build Performance

[Wikipedia](https://dumps.wikimedia.org/jawiki/latest/)のタイトル一覧(jawiki-latest-all-titles.gz)データでインデックスを作成してみます。なお、このデータは整列済みです。測定時は約290万件(圧縮時17MB)のデータでした。

下記はインデックスを作成するためにsizeを読み込む共通処理です。

```java
File file = new File("data", "jawiki-latest-all-titles.gz");
BufferedReader br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file)), "UTF-8"));
FileReadIterator<Boolean> ite = new FileReadIterator<>(br, new SimpleKeyValueDeserializer());
ite.next(); // skip header
int size = ite.readRestLines();
// 再度読み込み
br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file)), "UTF-8"));
ite = new FileReadIterator<>(br, new SimpleKeyValueDeserializer());
ite.next(); // skip header
```

まずは速度優先で全てをメモリに展開してインデックスを作成します。

```java
DoubleArrayTrie<Boolean> datrie = new DoubleArrayTrie<Boolean>();
// メモリ展開用リスト(指定サイズが用意される配列型リスト)
ExArrayList<String> key = new CachedMemoryArrayList<>(String.class, size);

datrie.build(ite, size, key, null); // build time : 10 [sec]
Runtime.getRuntime().totalMemory() // 1600 MB
datrie.getDoubleArraySize() // 約2800万
datrie.calcFillingRate() // DoubleArray充填率 0.6
```

ディスクを利用しメモリ使用量を優先して作成することもできます。

```java
// 要素を指定した分割数でディスクに書き出します。小さいほど省メモリですが遅くなります。
File unitDirectory = new File("units");
File keyDirectory = new File("key");
unitDirectory.mkdir();
keyDirectory.mkdir();
int separateSize = 1024 * 64;
DoubleArrayTrie<Boolean> datrie = new DoubleArrayInstanceBuilder<Boolean>()
                                .setUnitsFixDiskArray(unitDirectory, separateSize)
                                .createInstance();
// keyもディスクに書き出します
ExArrayList<String> key = new DiskArrayList<String>(String.class, keyDirectory, size, separateSize);

datrie.build(ite, size, key, null); // build time : 1 min 47 sec
Runtime.getRuntime().totalMemory() // 340 MB
// TODO: ディスクを利用した方法は検索時もスレッドセーフでなくなるため今後修正します
```

充填率を高める（サイズ縮小、検索時の速度を向上させる）ためにはFillingRatePriorityPositionStrategyを利用してください。インデックス作成は遅くなります。

```java
DoubleArrayTrie<Boolean> datrie =  new DoubleArrayInstanceBuilder<Boolean>()
                                .setPositionStrategy(new FillingRatePriorityPositionStrategy())
                                .createInstance();
// メモリ展開用リスト(指定サイズが用意される配列型リスト)
ExArrayList<String> key = new CachedMemoryArrayList<>(String.class, size);

datrie.build(ite, size, key, null); // build time : 14 [sec]
datrie.getDoubleArraySize() // 約2000万
datrie.calcFillingRate() // DoubleArray充填率 0.8
```

また、標準のDiskArrayListはJDK付属のObjectInputStream,ObjectOutputStreamで実装されています。これらをMessagePackなどで置き換えればより高速化する可能性があります。その場合、[ObjectStreamIndexer](src/main/java/jp/ksgwr/array/index/ObjectStreamIndexer.java)の実装を参考に独自のIndexerを実装してください。このライブラリでは外部ライブラリの依存を発生させないため定義していません。例えば以下のように利用します。

```java
ExArrayList<String> key = new DiskArrayList<String>(String.class, new InfoSegmentIndex(keyDirectory, "", new YourCustomIndexer()), size);

DoubleArrayInstanceBuilder<Boolean>()
.setUnitsExArray(new DiskArrayList<String>(...))
.createInstance();
```

## 高速化実装Tips

* UTF-16(unicode)をそのままマッピングすると隙間が大量にできるため、自前でマッピングするか1byte毎扱った方が良い
  * Character.MAX_CODE_POINT==1114111(int)
  * 本ライブラリでは出現頻度順に文字をマッピングし充填率を高める
* Node = {Base,CHECK}, Node[]構成にしメモリ局所性を高める
  * しかし、多くの実装はBASE[],CHECK[]になっているため実測値は要調査
* 文字種の探索時にnew ArrayList<>();など配列を毎回作成するとインスタンス生成コストが高いためListを使いまわす(EfficientNodeListを実装)
* 隙間探しの効率化として、常にbase使用済みノードを保持しincrementし後半のみを探す
  * base+c以降のみを使うと衝突は完全に回避できるが、さすがに効率が悪いので避ける
* ノードを深さ優先で構築すると探索時のキャッシュミスが起きにくい
* 子ノードが多い順で追加すると充填率が上がり構築・探索で効率が良い
  * 本ライブラリでは未実装

## 実装Tips

* keyはsortされていることを前提とすると構築・探索とも高速になる
* 最初の探索でn文字目の文字種(分岐数)を数え、全ての文字種に対しbase+cが未使用なbaseを決定する(使っていない文字のノードは使用済みでもcheckがあるため問題がない)
* base値にマイナス値のindexの順位を入れることで終端であることと、そのindexの番号を表現する
* 終端ラベルはc=0とする。baseを1以上にすればbase+c>0となる

## ToDo

* BitVectorの活用(TAILなどを入れる)
* spellerMatchのリファクタリング、他のメソッドのspeller実装の追加
* id(debug用)でなく、key,valueを返すためのメソッド追加
* MAMatchの実装
* javadocなどの記述、テストケースの作成
* おそらくsortしていないものを入れられた時にバグがある

## License

Apache License 2.0予定

## 参考資料

* dary
    * http://d.hatena.ne.jp/tkng/20061225/1167038986
* darts-clone
    * http://www.slideshare.net/s5yata/dsirnlp-04s5yata
    * https://github.com/s-yata/darts-clone
    * https://github.com/hiroshi-manabe/darts-clone-java
* darts-java
    * https://github.com/komiya-atsushi/darts-java
    * https://gist.github.com/komiya-atsushi/3765693
* trie4j
    * https://github.com/takawitter/trie4j
* jada
    * https://github.com/sile/jada
* BitVector
    * http://codezine.jp/article/detail/260
