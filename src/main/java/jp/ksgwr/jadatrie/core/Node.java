package jp.ksgwr.jadatrie.core;

public class Node {

	/** character code, 0=end of node, and incremental code */
	public int code;

	/** lower key index number */
	public int left;
	/** upper key index number */
	public int right;

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
