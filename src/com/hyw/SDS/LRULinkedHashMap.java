package com.hyw.SDS;

import java.util.LinkedHashMap;
import java.util.Map;

public class LRULinkedHashMap<K,V> extends LinkedHashMap<K,V>{  
    //���建�������  
    private int capacity;  
    private static final long serialVersionUID = 1L;  
    //�������Ĺ�����     
    LRULinkedHashMap(int capacity){  
        //����LinkedHashMap�Ĺ��������������²���  
        super(16,0.75f,true);  
        //����ָ���Ļ����������  
        this.capacity=capacity;  
    }  
    //ʵ��LRU�Ĺؼ����������map�����Ԫ�ظ��������˻��������������ɾ������Ķ���Ԫ��  
    @Override  
    public boolean removeEldestEntry(Map.Entry<K, V> eldest){ 
    	System.out.println("��ǰ�ڴ��С��"+size());
        System.out.println("��ɾ��K-V��ֵ�ԣ�"+eldest.getKey() + "-" + eldest.getValue());    
        return size()>capacity;  
    }    
//  
}  