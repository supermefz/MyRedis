package com.hyw.SDS;

public class SDS {
	int len=0;
	int free=0;
	String str= null;
	public SDS(int len, int free,String str) {
		// TODO 自动生成的构造函数存根
		 this.free = free;
		 this.str = str;
		 this.len = len;
	}
	//创建并返回一个只保存了空字符串 "" 的 sds
	public SDS(){
		 this.free = 0;
		 this.str = "";
		 this.len = 0;
	 }
	// 根据给定字符串 str ，创建一个包含同样字符串的 sds
	public SDS(String str){
		 this.free = 0;
		 this.str = str;
		 this.len = str.length();
	 }
	//释放给定的 sds的内存
	 public void sdsfree(SDS s){
		 if (s != null) {
			try {
				s.finalize();
			} catch (Throwable e) {
				// TODO 自动生成的 catch 块
				e.printStackTrace();
			}
		}
	 }
	 
	 public void sdsclear(SDS s){
		 
		  s.free = s.str.length();
		  s.str = "";
		  s.len = 0;
	 }
}
