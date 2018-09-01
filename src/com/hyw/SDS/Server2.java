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
		
	
		// TODO �Զ����ɵķ������
		Server2 server2 = new Server2();
		try {
			server2.childprocess();
			server2.initServer(8002);
			server2.listen();
		} catch (IOException e) {
			// TODO �Զ����ɵ� catch ��
			e.printStackTrace();
		}

	}
	public Server2() {
		// TODO �Զ����ɵĹ��캯�����
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
			
			System.out.println("�Ѿ���ʼ���ڴ����ݿ�");
			System.out.println("���ݿ�����Ϊ��");
			Iterator<String> keyset = mapdb2.keySet().iterator();
			while(keyset.hasNext()){
				String key= keyset.next();
			//	System.out.println(key+":"+mapdb2.get(key));
			}
			
		} catch (DocumentException e) {
			// TODO �Զ����ɵ� catch ��
			e.printStackTrace();
		}
		}
	}
	private Selector selector;
	

	/**
	 * ���һ��ServerSocketͨ�������Ը�ͨ����һЩ��ʼ���Ĺ���
	 * 
	 * @param port
	 *            �󶨵Ķ˿ں�
	 * @throws IOException
	 */
	public void initServer(int port) throws IOException {


//		if(f.exists() && f.length() == 0) {  
//		    System.out.println("�ļ�Ϊ�գ�");  
//		}   else {

		
		
		// ���һ��ServerSocketͨ��
		ServerSocketChannel channel = ServerSocketChannel.open();
		// ����ͨ��Ϊ������
		channel.configureBlocking(false);
		// ����ͨ����Ӧ��ServerSocket�󶨵�port�˿�
		channel.socket().bind(new InetSocketAddress(port));

		// ���һ��ͨ��������
		this.selector = Selector.open();

		// ��ͨ���������͸�ͨ���󶨣���Ϊ��ͨ��ע��SelectionKey.OP_ACCEPT�¼�,ע����¼���
		// �����¼�����ʱ��selector.select()�᷵�أ�������¼�û����selector.select()��һֱ������

		/*
		 * ����Selectorע��Channelʱ��registor()�����᷵��һ��SelectorKey����
		 * ������������һЩ���ԡ�
		 */
		SelectionKey key = channel.register(selector, SelectionKey.OP_ACCEPT);

	}

	/**
	 * ������ѯ�ķ�ʽ����selector���Ƿ�����Ҫ������¼�������У�����д���
	 * 
	 * @throws IOException
	 */
	public void listen() throws IOException {
		System.out.println("����������ɹ���");
		// ��ѯ����selector
		while (true) {
			// ��ע����¼�����ʱ���������أ�����,�÷�����һֱ����
			selector.select();
			// ���selector��ѡ�е���ĵ�������ѡ�е���Ϊע����¼�
			Iterator ite = this.selector.selectedKeys().iterator();
			while (ite.hasNext()) {
				SelectionKey key = (SelectionKey) ite.next();
				// ɾ����ѡ��key,�Է��ظ�����
				ite.remove();
				// �ͻ������������¼�
				if (key.isAcceptable()) {
					ServerSocketChannel server = (ServerSocketChannel) key.channel();
					// ��úͿͻ������ӵ�ͨ��
					SocketChannel channel = server.accept();
					// ���óɷ�����
					channel.configureBlocking(false);

					// �ںͿͻ������ӳɹ�֮�󣬸�ͨ�����ö���Ȩ�ޡ�
					channel.register(this.selector, SelectionKey.OP_READ);
					
					
				} else if (key.isReadable()) {
					// ����˿ɶ����¼�
					read(key);
				}

			}
		}
	}

	/**
	 * �����ȡ�ͻ��˷�������Ϣ ���¼�
	 * 
	 * @param key
	 * @throws IOException
	 */
	public void read(SelectionKey key) {
		// �������ɶ�ȡ��Ϣ:�õ��¼�������Socketͨ��
		SocketChannel channel = (SocketChannel) key.channel();
		// ������ȡ�Ļ�����
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		try {
			channel.read(buffer);
			byte[] data = buffer.array();
			String msg = new String(data).trim();
	//		System.out.println("������յ���Ϣ��" + msg);
			// ����ִ�������ķ��ؽ��

			String getC2Mstring = msg;
			String[] arg = strsplit(getC2Mstring); // �õ��ָ�������
		
			ByteBuffer outBuffer = ByteBuffer.wrap(methods(arg, mapdb2).getBytes());
			channel.write(outBuffer);// ����Ϣ���͵�ͨ����
			channel.close();
			
			
		} catch (IOException e) {
			// TODO �Զ����ɵ� catch ��
			key.cancel();
			try {
				channel.socket().close();
				channel.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}

	}

	// ���õ������������з�Ƭ����---���ݿո���з�Ƭ
	public static String[] strsplit(String str) {
		String[] arg = str.split(" ");
		return arg;
	}

	// �����ȽϺ���
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
				// TODO �Զ����ɵ� catch ��
				e.printStackTrace();
			}
			
			return str;
		}
		 else {
			System.out.println("�޴˺�����");
			return "�޴˺�����";
		}

	}

	// �ж��Ƿ�Ϊ����
	public static boolean isnum(String str) {
		return str.matches("^[-+]?(([0-9]+)([.]([0-9]+))?|([.]([0-9]+))?)$");
	}

	// set������ʵ��(3������)----���һ��K-V��ֵ��
	public static String set(String[] arg, Map<String, String> map) {
		String str = "0";
		if (arg.length != 3) {
			System.out.println("set�����������");
		} else {
			map.put(arg[1], arg[2]);
			str = "1";
		}
		return str;
	}

	// setnx������ʵ��(3������)----�鿴������key,���key���ڣ�����ʧ�ܷ���0�����򷵻�1��
	public static String setnx(String[] arg, Map<String, String> map) {
		String str = "0";
		if (arg.length != 3) {
			System.out.println("setnx�����������");
		} else if (!map.containsKey(arg[1])) {
			map.put(arg[1], arg[2]);
			str = "1";
		}
		return str; // ʧ�ܷ���0���ɹ�����1
	}

	// setex������ʵ��(4������)----��������������Ч�ļ�ֵ��----ʵ�ֲ��������������������
//	public static String setex(String[] arg, Map<String, String> map) {
//		String str = "0";
//		if (arg.length != 4) {
//			System.out.println("setex�����������");
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
//			System.out.println("ʱ���������");
//		}
//		return str;
//	}

	// setrange������ʵ��(4������)----�滻��arg[2]���ַ���֮����ַ�
		public static String setrange(String[] arg, Map<String, String> map) {
			String rnum = new String();
			// �жϲ���
			if (arg.length != 4) {
				System.out.println("setrange�����������");
				return "setrange�����������";
			}
			// �жϵ���������
			else if (isnum(arg[2])) {
				int num = 0;
				// �õ��ϲ����ַ����ܳ���
				int tempnum = Integer.parseInt(arg[2]) + arg[3].length();
				char[] temp = new char[tempnum];
				char[] add = new char[arg[3].length()];
				char[] str = new char[arg[1].length()];
				// �������ӵ�key����
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
				// �������ӵ�key������
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
			// �����������
			else {
				System.out.println("�����������������");
				return "setrange�����������!";
			}
			return rnum; // �����޸ĺ��value�ĳ���
		}
		
	// mset������ʵ��----ͬʱ��Ӷ��K-V��ֵ��
	public static String mset(String[] arg, Map<String, String> map) {
		String str = "0";
		if ((arg.length - 1) % 2 != 0) {
			System.out.println("mset�����������");
		} else {
			for (int i = 1; i <= arg.length - 1; i = i + 2) {
				map.put(arg[i], arg[i + 1]);
			}
			str = "OK";
		}
		return str;
	}

	// msetnx������ʵ��----ͬʱ�����������ڵ�key,���һ��ʧ�ܣ���ȫ��ʧ�ܷ���0�����򷵻�1
	public static String msetnx(String[] arg, Map<String, String> map) {
		String str = "0";
		if ((arg.length - 1) % 2 != 0) {
			System.out.println("msetnx�����������");
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

	// get������ʵ��----����key����Ӧ��value
	public static String get(String[] arg, Map<String, String> map) {
		String value = "error!";
		if (arg.length != 2) {
			System.out.println("get�����������");
		} else {
			if(map.containsKey(arg[1])){
				value = map.get(arg[1]);
			}else{
				value = "�����ڸ�key-value!";
			}
		}
		return value;
	}

	// getset������ʵ��----����ԭkey��value��������value
	public static String getset(String[] arg, Map<String, String> map) {
		String value = "error!";
		if (arg.length != 3) {
			System.out.println("getset�����������");
		} else {
			value = map.get(arg[1]);
			System.out.println("value = "+value);
			map.put(arg[1], arg[2]);
		}
		return value;
	}

	// getrange������ʵ��----����range��Χ�ڵ�valueֵ
		public static String getrange(String[] arg, Map<String, String> map) {
			String str = "";
			// �����ж�
			if (arg.length != 4) {
				System.out.println("getrange�����������");
				str = "getrange�����������";
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
					str = "���������K-V��ֵ�ԣ�";
				}
			}
			return str;
		}

	// mget������ʵ��----һ�η��ض��key
	public static String mget(String[] arg, Map<String, String> map) {
		String[] mstr = new String[arg.length - 1];
		for (int i = 1; i < arg.length; i++) {
			mstr[i - 1] = map.get(arg[i]);
		}
		return String.join("\n", mstr);
	}
	//����һ�����߳�������־û�
	public  void childprocess() {
		
		
		new Thread(new RDB2()).start();
//			rt = Runtime.getRuntime();//�����ӽ���
//			String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";//�õ�java��װĿ¼�ĵ�ַ�ַ���
//
//			String cp = "\"" + System.getProperty("java.class.path");//�õ�Java���·���ַ���
//
//			cp += File.pathSeparator + ClassLoader.getSystemResource("").getPath() + "\"";//�����������������·���в��Ҿ���ָ�����Ƶ���Դ,�ú���ȡ�ùؼ�·��
//
//			String cmd = java + " -cp " + cp + " com.hyw.SDS.RDB";//�õ������ַ���
//			Process p = rt.exec(cmd);
		
	}
	// ����ͬ��
		private static String sync() throws IOException {
			String str = null;
			// ����һ��file����������ʼ��FileReader
			File file = new File("serverdb2.xml");
			// ����һ��fileReader����������ʼ��BufferedReader
			FileReader reader = new FileReader(file);
			// newһ��BufferedReader���󣬽��ļ����ݶ�ȡ������
			BufferedReader bReader = new BufferedReader(reader);
			// ����һ���ַ������棬���ַ�����Ż�����
			StringBuilder sb = new StringBuilder();

			// ���ж�ȡ�ļ����ݣ�����ȡ���з���ĩβ�Ŀո�
			while ((str = bReader.readLine()) != null) {
				// ����ȡ���ַ�����ӻ��з����ۼӴ���ڻ�����
				sb.append(str);
		//		System.out.println(str);
			}
			
			
			bReader.close();
			String rstr = sb.toString();
			return rstr;

		}
	
}
