package com.hyw.SDS;

import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;



public class ConsistentHash<T> {
	
    //Hash计算对象，用于自定义hash算法
	private final HashFunction hashFunction;
	// 节点的复制因子,实际节点个数 * numberOfReplicas = 虚拟节点个数
	private final int numberOfReplicas;
	// 存储虚拟节点的hash值到真实节点的映射
	private final SortedMap<Long, T> circle = new TreeMap<Long, T>();

	public ConsistentHash(HashFunction hashFunction, int numberOfReplicas, Collection<T> nodes) {
		this.hashFunction = hashFunction;
		this.numberOfReplicas = numberOfReplicas;
		for (T node : nodes)
			add(node);
	}

	/**
     * 增加节点<br>每增加一个节点，就会在闭环上增加给定复制节点数<br>
     * 例如复制节点数是2，则每调用此方法一次，增加两个虚拟节点，这两个
     * 节点指向同一Node,由于hash算法会调用node的toString方法，故按照toString去重
     * @param node 节点对象
     */
	public void add(T node) {
		for (int i = 0; i < numberOfReplicas; i++)
			//将此映射中的指定值与指定键关联
			circle.put(hashFunction.hash(node.toString() + i), node);
	}

	public void remove(T node) {
		for (int i = 0; i < numberOfReplicas; i++)
			circle.remove(hashFunction.hash(node.toString() + i));
	}

	/*
	 * 获得一个最近的顺时针节点,根据给定的key 取Hash 
	 * 然后再取得顺时针方向上最近的一个虚拟节点对应的实际节点 再从实际节点中取得 数据
	 */
	public T get(Object key) {
		if (circle.isEmpty())
			return null;
		//遍历环
		long hash = hashFunction.hash((String) key);// node 用String来表示,获得node在哈希环中的hashCode
		if (!circle.containsKey(hash)) {// 数据映射在两台虚拟机器所在环之间,就需要按顺时针方向寻找机器
			SortedMap<Long, T> tailMap = circle.tailMap(hash);
			hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
		}
		//正好命中
		return circle.get(hash);
	}

	public static void main(String[] args) {
		
		int a = 0;
		int b = 0;
		int c = 0;
		Set<String> nodes = new HashSet<String>();
		nodes.add("A");
		nodes.add("B");
		nodes.add("C");
		ConsistentHash<String> ha = new ConsistentHash<String>(new HashFunction(), 1000, nodes);
		for(int i = 0;i < 100;i++) {
			
			String s=String.valueOf((int)(1+Math.random()*(1000-1+1)));
			System.out.println(s);
			String key = ha.get(s);
			System.out.println(key);
			switch (key) {
			case "A":
				a++;
				break;
			case "B":
				b++;
				break;	
			case "C":
				c++;
				break;
			default:
				break;
			}
		}
		System.out.println("A:"+a+"  B:"+b+"  C:"+c);

	}

}