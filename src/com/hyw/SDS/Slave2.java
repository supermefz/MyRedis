package com.hyw.SDS;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

public class Slave2 {
	// 需要拷贝的目的数据库
	File destfile = new File("slavedb2.xml");
	//Map<String, String> slavedb2 = new HashMap<>();
	static Map<String,String> slavedb2=new LRULinkedHashMap<String,String>(4);
	Map<String, Integer> timeout = new HashMap<>();

	boolean live = false; // 是否重置生命值
	int life = 20;
	boolean flag = true;
	private static SocketChannel socket;

	// 设置线程
	public void run() {
		// 每40秒自动同步一次
		new Thread(new MyThread()).start();
		// 判断键值对是否timeout
		new Thread(new time()).start();
	}

	// 设置线程----主从同步
	private class MyThread implements Runnable {

		@Override
		public void run() {
			// Auto-generated method stub
			while (flag) {
				// 发送同步请求命令
				// flag = false;
				try {
					initS2M("localhost", 8002);
				} catch (IOException e1) {
					// Auto-generated catch block
					e1.printStackTrace();
				}
				send("sync");
				receivedb();

				try {
					Thread.sleep(40 * 1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	}

	// 初始化从服务器
	private void initS2M(String ip, int port) throws IOException {
		// 获得一个Socket通道
		socket = SocketChannel.open();
		// Socket连接
		socket.connect(new InetSocketAddress(ip, port));
		// 通道设置为非阻塞
		socket.configureBlocking(false);

	}

	// 发送数据库同步信息给master
	private void send(String msg) {
		try {
			socket.write(ByteBuffer.wrap(msg.getBytes()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// 接受数据库同步信息并保存
	private void receivedb() {

		String msg = null;
		boolean getted = true;
		while (getted) {
			try {
				// 创建读取的缓冲区
				ByteBuffer buffer = ByteBuffer.allocate(1024);
				// 从该通道将字节序列读入给定的缓冲区。
				socket.read(buffer);
				// 缓存区以字节数组的方式复制给data
				byte[] data = buffer.array();
				// 转化成字符串，除去空格
				msg = new String(data).trim();

				if (!msg.isEmpty()) {
					// flag = true;
//					System.out.println("从服务端收到信息：" + msg);

					FileOutputStream outputStream = new FileOutputStream(destfile);
					// 有格式，有空格 易读
					OutputFormat format = OutputFormat.createPrettyPrint();
					// 无空格换行,节约空间
					// OutputFormat format=OutputFormat.createCompactFormat();

					format.setEncoding("UTF-8");
					XMLWriter writer = new XMLWriter(outputStream, format);

					writer.write(DocumentHelper.parseText(msg));
					writer.close();
					initmap(destfile);
					getted = false;
				}
			} catch (Exception e) {
				// Auto-generated catch block
				System.out.println("xml文件拷贝异常");
				e.printStackTrace();
				try {
					socket.socket().close();
					socket.close();
					break;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	// 设置线程----判断时候有过期K-V键值对
	private class time implements Runnable {
		@Override
		public void run() {
			// Auto-generated method stub
			while (true) {
				// 每5秒判断一次是否有timeout的K-V对
				for (Map.Entry<String, Integer> KV : timeout.entrySet()) {
					if (KV.getValue() == 0) {
						slavedb2.remove(KV.getKey());
						System.out.println("删除超时K-V:" + KV.getKey());
					}
				}
				// 等待5秒
				try {
					Thread.sleep(5 * 1000);
				} catch (InterruptedException e) {
					// generated catch block
					e.printStackTrace();
				}
				// 使每一个K-V键值对的生命周期减少5秒
				for (Map.Entry<String, Integer> KV : timeout.entrySet()) {
					int temp = KV.getValue() - 5;
					timeout.put(KV.getKey(), temp);
				}

			}
		}

	}

	// 读取文件到内存中----内存淘汰----timeout
	public void initmap(File f) {
		SAXReader r = new SAXReader();
		try {
			org.dom4j.Document doc = r.read(f);

			Element e = doc.getRootElement();
			// System.out.println("sdjf");
			Iterator<Element> e1 = e.elementIterator();// 根节点下的子节点集合
			// System.out.println(e1.hasNext());
			while (e1.hasNext()) {
				// System.out.println("sdf");
				Element tmpe = e1.next();
				if (tmpe.attributeValue("name").equals("kv")) {
					/**
					 * timeout----内存淘汰
					 */
					if (slavedb2.containsKey(tmpe.element("key").getText())) { // 如果内存中存在K-V键值对
						// K-V键值对没有改变
						if (slavedb2.get(tmpe.element("key").getText()).equals(tmpe.element("value").getText())) {
							// 标志位不变
							live = false;
						} else {
							// 标志位置1
							live = true;
							System.out.println(tmpe.element("key").getText() + "标志位置1！");
						}
					} else { // 内存中不存在K-V键值对
						// 将数据读入到内存里
						System.out.println(
								tmpe.element("key").getText() + "-" + tmpe.element("value").getText() + "读入内存！");
						slavedb2.put(tmpe.element("key").getText(), tmpe.element("value").getText());
						// 标志位置1
						live = true;
					}
					// 标志位为true
					if (live = true) {
						// 设置K-V生命周期为20秒
						System.out.println(
								tmpe.element("key").getText() + "-" + tmpe.element("value").getText() + "生命周期重置！");
						timeout.put(tmpe.element("key").getText(), life);
						live = false;
					}
				}
			}
		} catch (DocumentException e) {
			// 自动生成的 catch 块
			e.printStackTrace();
		}
	}

	/**
	 * 私有化一个Slave上的selector
	 */
	private Selector selector;

	// 设置selector端口，方便C2S快速读取数据
	public void initS2C(int port) throws IOException {
		// 新建一个socket
		ServerSocketChannel socket = ServerSocketChannel.open();
		// 给serversocketchannel绑定一个port
		socket.socket().bind(new InetSocketAddress(port));

		// 注册一个通道并且让通道出于可接受状态
		selector = selector.open();
		socket.configureBlocking(false);
		SelectionKey key = socket.register(selector, SelectionKey.OP_ACCEPT);

		System.out.println("Slaver监听通道启动完成！");
	}

	// Slave开始监听channel
	public void listen() throws IOException {
		System.out.println("服务端启动成功！");
		// 轮询访问selector
		while (true) {
			// 当注册的事件到达时，方法返回；否则,该方法会一直阻塞
			selector.select();
			// 获得selector中选中的项的迭代器，选中的项为注册的事件
			Iterator<SelectionKey> ite = this.selector.selectedKeys().iterator();
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
			System.out.println("slave服务端收到信息：" + msg);
			// 插入执行命令后的返回结果

			String getC2Mstring = msg;
			String[] arg = strsplit(getC2Mstring); // 得到分割后的数据

			// 如果使用到现有的K-V键值对，则将其生命周期重置
			if (slavedb2.containsKey(arg[1])) {
				timeout.put(arg[1], life);
				System.out.println("重置K-V生命值：" + arg[1]);
			}

			ByteBuffer outBuffer = ByteBuffer.wrap(methods(arg, slavedb2).getBytes());
			channel.write(outBuffer);// 将消息回送到通道里
			channel.close();

		} catch (IOException e) {
			// 自动生成的 catch 块
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
	public String methods(String[] arg, Map<String, String> map) {
		String str = "0";
		if (arg[0].equals("get")) {
			str = get(arg, map);
			System.out.println(str);
			return str;
		} else if (arg[0].equals("getrange")) {
			str = getrange(arg, map);
			System.out.println(str);
			return str;
		}
		return str;
	}

	// get方法的实现----返回key所对应的value
	public static String get(String[] arg, Map<String, String> map) {
		String value = "";
		if (arg.length != 2) {
			System.out.println("get参数输入错误！");
			value = "get参数输入错误！";
		} else {
			if (map.containsKey(arg[1])) {
				value = map.get(arg[1]);
			} else {
				value = "不存在该key-value!";
			}
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

	// 从服务器主方法
	public static void main(String[] args) {
		Slave2 Slave2 = new Slave2();
		try {
			// Slave的初始化，并且负责自动主从复制，40s完成一次
			Slave2.run();
			// 用于监听客户端访问
			Slave2.initS2C(8012);
			Slave2.listen();

		} catch (IOException e) {
			// 自动生成的 catch 块
			e.printStackTrace();
		}
	}

}
