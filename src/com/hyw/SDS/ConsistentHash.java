package com.hyw.SDS;

import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;



public class ConsistentHash<T> {
	
    //Hash������������Զ���hash�㷨
	private final HashFunction hashFunction;
	// �ڵ�ĸ�������,ʵ�ʽڵ���� * numberOfReplicas = ����ڵ����
	private final int numberOfReplicas;
	// �洢����ڵ��hashֵ����ʵ�ڵ��ӳ��
	private final SortedMap<Long, T> circle = new TreeMap<Long, T>();

	public ConsistentHash(HashFunction hashFunction, int numberOfReplicas, Collection<T> nodes) {
		this.hashFunction = hashFunction;
		this.numberOfReplicas = numberOfReplicas;
		for (T node : nodes)
			add(node);
	}

	/**
     * ���ӽڵ�<br>ÿ����һ���ڵ㣬�ͻ��ڱջ������Ӹ������ƽڵ���<br>
     * ���縴�ƽڵ�����2����ÿ���ô˷���һ�Σ�������������ڵ㣬������
     * �ڵ�ָ��ͬһNode,����hash�㷨�����node��toString�������ʰ���toStringȥ��
     * @param node �ڵ����
     */
	public void add(T node) {
		for (int i = 0; i < numberOfReplicas; i++)
			//����ӳ���е�ָ��ֵ��ָ��������
			circle.put(hashFunction.hash(node.toString() + i), node);
	}

	public void remove(T node) {
		for (int i = 0; i < numberOfReplicas; i++)
			circle.remove(hashFunction.hash(node.toString() + i));
	}

	/*
	 * ���һ�������˳ʱ��ڵ�,���ݸ�����key ȡHash 
	 * Ȼ����ȡ��˳ʱ�뷽���������һ������ڵ��Ӧ��ʵ�ʽڵ� �ٴ�ʵ�ʽڵ���ȡ�� ����
	 */
	public T get(Object key) {
		if (circle.isEmpty())
			return null;
		//������
		long hash = hashFunction.hash((String) key);// node ��String����ʾ,���node�ڹ�ϣ���е�hashCode
		if (!circle.containsKey(hash)) {// ����ӳ������̨����������ڻ�֮��,����Ҫ��˳ʱ�뷽��Ѱ�һ���
			SortedMap<Long, T> tailMap = circle.tailMap(hash);
			hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
		}
		//��������
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