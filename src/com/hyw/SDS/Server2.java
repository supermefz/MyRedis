package com.hyw.SDS;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.w3c.dom.Document;

public class Server2 {
	
	boolean started = false;
	
	Runtime rt= Runtime.getRuntime();
	Process p;
	  static File f = new File("serverdb2.xml");
	 //static Map<String, String> mapdb2 = new HashMap<>();
	  static Map<String,String> mapdb2=new LRULinkedHashMap<String,String>(4);
	public static void main(String[] args) {
		
	
		// TODO 自动生成的方法存根
		Server2 server2 = new Server2();
		try {
			server2.childprocess();
			server2.initServer(8002);
			server2.listen();
		} catch (IOException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}

	}
	public Server2() {
		// TODO 自动生成的构造函数存根
		if ((f.length()!=0)&&f.exists()) {
			
		try {
			SAXReader reader = new SAXReader();
	
			org.dom4j.Document doc=  reader.read(f);
			
			Element root = doc.getRootElement();
			
			Iterator<Element> ite= root.elementIterator();
			while(ite.hasNext()){
				Element tmpe = ite.next();
				if (tmpe.attributeValue("name").equals("kv")) {
					mapdb2.put(tmpe.elementText("key"), tmpe.elementText("value"));
					
					
				}
			}
			
			System.out.println("已经初始化内存数据库");
			System.out.println("数据库内容为：");
			Iterator<String> keyset = mapdb2.keySet().iterator();
			while(keyset.hasNext()){
				String key= keyset.next();
			//	System.out.println(key+":"+mapdb2.get(key));
			}
			
		} catch (DocumentException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}
		}
	}
	private Selector selector;
	

	/**
	 * 获得一个ServerSocket通道，并对该通道做一些初始化的工作
	 * 
	 * @param port
	 *            绑定的端口号
	 * @throws IOException
	 */
	public void initServer(int port) throws IOException {


//		if(f.exists() && f.length() == 0) {  
//		    System.out.println("文件为空！");  
//		}   else {

		
		
		// 获得一个ServerSocket通道
		ServerSocketChannel channel = ServerSocketChannel.open();
		// 设置通道为非阻塞
		channel.configureBlocking(false);
		// 将该通道对应的ServerSocket绑定到port端口
		channel.socket().bind(new InetSocketAddress(port));

		// 获得一个通道管理器
		this.selector = Selector.open();

		// 将通道管理器和该通道绑定，并为该通道注册SelectionKey.OP_ACCEPT事件,注册该事件后，
		// 当该事件到达时，selector.select()会返回，如果该事件没到达selector.select()会一直阻塞。

		/*
		 * 当向Selector注册Channel时，registor()方法会返回一个SelectorKey对象。
		 * 这个对象包含了一些属性。
		 */
		SelectionKey key = channel.register(selector, SelectionKey.OP_ACCEPT);

	}

	/**
	 * 采用轮询的方式监听selector上是否有需要处理的事件，如果有，则进行处理
	 * 
	 * @throws IOException
	 */
	public void listen() throws IOException {
		System.out.println("服务端启动成功！");
		// 轮询访问selector
		while (true) {
			// 当注册的事件到达时，方法返回；否则,该方法会一直阻塞
			selector.select();
			// 获得selector中选中的项的迭代器，选中的项为注册的事件
			Iterator ite = this.selector.selectedKeys().iterator();
			while (ite.hasNext()) {
				SelectionKey key = (SelectionKey) ite.next();
				// 删除已选的key,以防重复处理
				ite.remove();
				// 客户端请求连接事件
				if (key.isAcceptable()) {
					ServerSocketChannel server = (ServerSocketChannel) key.channel();
					// 获得和客户端连接的通道
					SocketChannel channel = server.accept();
					// 设置成非阻塞
					channel.configureBlocking(false);

					// 在和客户端连接成功之后，给通道设置读的权限。
					channel.register(this.selector, SelectionKey.OP_READ);
					
					
				} else if (key.isReadable()) {
					// 获得了可读的事件
					read(key);
				}

			}
		}
	}

	/**
	 * 处理读取客户端发来的信息 的事件
	 * 
	 * @param key
	 * @throws IOException
	 */
	public void read(SelectionKey key) {
		// 服务器可读取消息:得到事件发生的Socket通道
		SocketChannel channel = (SocketChannel) key.channel();
		// 创建读取的缓冲区
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		try {
			channel.read(buffer);
			byte[] data = buffer.array();
			String msg = new String(data).trim();
	//		System.out.println("服务端收到信息：" + msg);
			// 插入执行命令后的返回结果

			String getC2Mstring = msg;
			String[] arg = strsplit(getC2Mstring); // 得到分割后的数据
		
			ByteBuffer outBuffer = ByteBuffer.wrap(methods(arg, mapdb2).getBytes());
			channel.write(outBuffer);// 将消息回送到通道里
			channel.close();
			
			
		} catch (IOException e) {
			// TODO 自动生成的 catch 块
			key.cancel();
			try {
				channel.socket().close();
				channel.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}

	}

	// 将得到的数据流进行分片处理---根据空格进行分片
	public static String[] strsplit(String str) {
		String[] arg = str.split(" ");
		return arg;
	}

	// 方法比较函数
	public static String methods(String[] arg, Map<String, String> map) {
		String str = "0";
		if (arg[0].equals("set")) {
			str = set(arg, map);
			return str;
		} else if (arg[0].equals("setnx")) {
			str = setnx(arg, map);
			System.out.println(str);
			return str;
//		} else if (arg[0].equals("xx:setex")) {
//			str = setex(arg, map);
//			return str;
		} else if (arg[0].equals("setrange")) {
			str = setrange(arg, map);
			System.out.println(str);
			return str;
		} else if (arg[0].equals("mset")) {
			str = mset(arg, map);
			System.out.println(str);
			return str;
		} else if (arg[0].equals("msetnx")) {
			str = msetnx(arg, map);
			System.out.println(str);
			return str;
		} else if (arg[0].equals("get")) {
			str = get(arg, map);
			System.out.println(str);
			return str;
		} else if (arg[0].equals("getset")) {
			str = getset(arg, map);
			System.out.println(str);
			return str;
		} else if (arg[0].equals("getrange")) {
			str = getrange(arg, map);
			System.out.println(str);
			return str;
		} else if (arg[0].equals("mget")) {
			str = mget(arg, map);
			System.out.println(str);
			return str;
//		}else if (arg[0].equals("bgsave")) {
//			  childprocess();
//			return "1";
		}
		else if (arg[0].equals("sync")) {
			try {
				str = sync();
			} catch (IOException e) {
				// TODO 自动生成的 catch 块
				e.printStackTrace();
			}
			
			return str;
		}
		 else {
			System.out.println("无此函数！");
			return "无此函数！";
		}

	}

	// 判断是否为数字
	public static boolean isnum(String str) {
		return str.matches("^[-+]?(([0-9]+)([.]([0-9]+))?|([.]([0-9]+))?)$");
	}

	// set方法的实现(3个参数)----添加一对K-V键值对
	public static String set(String[] arg, Map<String, String> map) {
		String str = "0";
		if (arg.length != 3) {
			System.out.println("set参数输入错误！");
		} else {
			map.put(arg[1], arg[2]);
			str = "1";
		}
		return str;
	}

	// setnx方法的实现(3个参数)----查看并设置key,如果key存在，设置失败返回0；否则返回1；
	public static String setnx(String[] arg, Map<String, String> map) {
		String str = "0";
		if (arg.length != 3) {
			System.out.println("setnx参数输入错误！");
		} else if (!map.containsKey(arg[1])) {
			map.put(arg[1], arg[2]);
			str = "1";
		}
		return str; // 失败返回0，成功返回1
	}

	// setex方法的实现(4个参数)----设置在期限内有效的键值对----实现不完整，会出现阻塞现象
//	public static String setex(String[] arg, Map<String, String> map) {
//		String str = "0";
//		if (arg.length != 4) {
//			System.out.println("setex参数输入错误！");
//		} else if (isnum(arg[2])) {
//			long t1 = System.currentTimeMillis();
//			map.put(arg[1], arg[3]);
//			while (true) {
//				long t2 = System.currentTimeMillis();
//				if (t2 - t1 > Integer.parseInt(arg[2]) * 1000)
//					break;
//			}
//			map.remove(arg[1]);
//			str = "1";
//		} else {
//			System.out.println("时间输入错误！");
//		}
//		return str;
//	}

	// setrange方法的实现(4个参数)----替换第arg[2]个字符串之后的字符
		public static String setrange(String[] arg, Map<String, String> map) {
			String rnum = new String();
			// 判断参数
			if (arg.length != 4) {
				System.out.println("setrange参数输入错误！");
				return "setrange参数输入错误！";
			}
			// 判断第三个参数
			else if (isnum(arg[2])) {
				int num = 0;
				// 得到合并后字符串总长度
				int tempnum = Integer.parseInt(arg[2]) + arg[3].length();
				char[] temp = new char[tempnum];
				char[] add = new char[arg[3].length()];
				char[] str = new char[arg[1].length()];
				// 如果待添加的key存在
				if (map.containsKey(arg[1])) {
					str = map.get(arg[1]).toCharArray();
					add = arg[3].toCharArray();
					for (int i = 0; i < Integer.parseInt(arg[2]); i++) {
						temp[i] = str[num++];
					}
					num = 0;
					for (int i = Integer.parseInt(arg[2]); i < tempnum; i++) {
						temp[i] = add[num++];
					}
					String rstr = String.valueOf(temp);
					map.put(arg[1], rstr);
					rnum = String.valueOf(rstr.length());
				}
				// 如果待添加的key不存在
				else {
					add = arg[3].toCharArray();
					for (int i = 0; i < Integer.parseInt(arg[2]); i++) {
						temp[i] = ' ';
					}
					for (int i = Integer.parseInt(arg[2]); i < tempnum; i++) {
						temp[i] = add[num++];
					}
					String rstr = String.valueOf(temp);
					map.put(arg[1], rstr);
					rnum = String.valueOf(rstr.length());
				}
			}
			// 参数输入错误
			else {
				System.out.println("第三个参数输入错误！");
				return "setrange参数输入错误!";
			}
			return rnum; // 返回修改后的value的长度
		}
		
	// mset方法的实现----同时添加多对K-V键值对
	public static String mset(String[] arg, Map<String, String> map) {
		String str = "0";
		if ((arg.length - 1) % 2 != 0) {
			System.out.println("mset参数输入错误！");
		} else {
			for (int i = 1; i <= arg.length - 1; i = i + 2) {
				map.put(arg[i], arg[i + 1]);
			}
			str = "OK";
		}
		return str;
	}

	// msetnx方法的实现----同时输入多个不存在的key,如果一个失败，则全部失败返回0，否则返回1
	public static String msetnx(String[] arg, Map<String, String> map) {
		String str = "0";
		if ((arg.length - 1) % 2 != 0) {
			System.out.println("msetnx参数输入错误！");
		} else {
			for (int i = 1; i <= arg.length - 1; i = i + 2) {
				if (map.containsKey(arg[i])) {
					return str;
				}
			}
			for (int i = 1; i <= arg.length - 1; i = i + 2) {
				map.put(arg[i], arg[i + 1]);
			}
			str = "1";
		}
		return str;
	}

	// get方法的实现----返回key所对应的value
	public static String get(String[] arg, Map<String, String> map) {
		String value = "error!";
		if (arg.length != 2) {
			System.out.println("get参数输入错误！");
		} else {
			if(map.containsKey(arg[1])){
				value = map.get(arg[1]);
			}else{
				value = "不存在该key-value!";
			}
		}
		return value;
	}

	// getset方法的实现----返回原key的value并设置新value
	public static String getset(String[] arg, Map<String, String> map) {
		String value = "error!";
		if (arg.length != 3) {
			System.out.println("getset参数输入错误！");
		} else {
			value = map.get(arg[1]);
			System.out.println("value = "+value);
			map.put(arg[1], arg[2]);
		}
		return value;
	}

	// getrange方法的实现----返回range范围内的value值
		public static String getrange(String[] arg, Map<String, String> map) {
			String str = "";
			// 参数判断
			if (arg.length != 4) {
				System.out.println("getrange参数输入错误！");
				str = "getrange参数输入错误！";
			} else {
				if (map.containsKey(arg[1])) {
					char[] temp = new char[arg[1].length()];
					char[] tmp = new char[Integer.parseInt(arg[3]) - Integer.parseInt(arg[2]) + 1];
					int num = 0;
					temp = map.get(arg[1]).toCharArray();
					for (int i = Integer.parseInt(arg[2]); i <= Integer.parseInt(arg[3]); i++) {
						tmp[num++] = temp[i];
					}
					str = String.valueOf(tmp);
				} else {
					str = "不存在这个K-V键值对！";
				}
			}
			return str;
		}

	// mget方法的实现----一次返回多个key
	public static String mget(String[] arg, Map<String, String> map) {
		String[] mstr = new String[arg.length - 1];
		for (int i = 1; i < arg.length; i++) {
			mstr[i - 1] = map.get(arg[i]);
		}
		return String.join("\n", mstr);
	}
	//创建一个子线程来处理持久化
	public  void childprocess() {
		
		
		new Thread(new RDB2()).start();
//			rt = Runtime.getRuntime();//创建子进程
//			String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";//得到java安装目录的地址字符串
//
//			String cp = "\"" + System.getProperty("java.class.path");//得到Java类的路径字符串
//
//			cp += File.pathSeparator + ClassLoader.getSystemResource("").getPath() + "\"";//从用来加载类的搜索路径中查找具有指定名称的资源,让后再取得关键路径
//
//			String cmd = java + " -cp " + cp + " com.hyw.SDS.RDB";//得到命令字符串
//			Process p = rt.exec(cmd);
		
	}
	// 主从同步
		private static String sync() throws IOException {
			String str = null;
			// 定义一个file对象，用来初始化FileReader
			File file = new File("serverdb2.xml");
			// 定义一个fileReader对象，用来初始化BufferedReader
			FileReader reader = new FileReader(file);
			// new一个BufferedReader对象，将文件内容读取到缓存
			BufferedReader bReader = new BufferedReader(reader);
			// 定义一个字符串缓存，将字符串存放缓存中
			StringBuilder sb = new StringBuilder();

			// 逐行读取文件内容，不读取换行符和末尾的空格
			while ((str = bReader.readLine()) != null) {
				// 将读取的字符串添加换行符后累加存放在缓存中
				sb.append(str);
		//		System.out.println(str);
			}
			
			
			bReader.close();
			String rstr = sb.toString();
			return rstr;

		}
	
}
