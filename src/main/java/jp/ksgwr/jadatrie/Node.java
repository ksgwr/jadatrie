package jp.ksgwr.jadatrie;

public class Node {

	int code; //0が終端、それ以外は文字コード+1
	int left;
	int right;

	public Node(int code,int left,int right) {
		this.code = code;
		this.left = left;
		this.right = right;
	}

	public Node() {

	}

	public void set(Node node) {
		this.code = node.code;
		this.left = node.left;
		this.right = node.right;
	}

	public String toString() {
		return "Node(c:"+code+"("+(char)(code-1)+"),l:"+left+",r:"+right+")";
	}
}
