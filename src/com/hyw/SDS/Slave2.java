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
	// ��Ҫ������Ŀ�����ݿ�
	File destfile = new File("slavedb2.xml");
	//Map<String, String> slavedb2 = new HashMap<>();
	static Map<String,String> slavedb2=new LRULinkedHashMap<String,String>(4);
	Map<String, Integer> timeout = new HashMap<>();

	boolean live = false; // �Ƿ���������ֵ
	int life = 20;
	boolean flag = true;
	private static SocketChannel socket;

	// �����߳�
	public void run() {
		// ÿ40���Զ�ͬ��һ��
		new Thread(new MyThread()).start();
		// �жϼ�ֵ���Ƿ�timeout
		new Thread(new time()).start();
	}

	// �����߳�----����ͬ��
	private class MyThread implements Runnable {

		@Override
		public void run() {
			// Auto-generated method stub
			while (flag) {
				// ����ͬ����������
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

	// ��ʼ���ӷ�����
	private void initS2M(String ip, int port) throws IOException {
		// ���һ��Socketͨ��
		socket = SocketChannel.open();
		// Socket����
		socket.connect(new InetSocketAddress(ip, port));
		// ͨ������Ϊ������
		socket.configureBlocking(false);

	}

	// �������ݿ�ͬ����Ϣ��master
	private void send(String msg) {
		try {
			socket.write(ByteBuffer.wrap(msg.getBytes()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// �������ݿ�ͬ����Ϣ������
	private void receivedb() {

		String msg = null;
		boolean getted = true;
		while (getted) {
			try {
				// ������ȡ�Ļ�����
				ByteBuffer buffer = ByteBuffer.allocate(1024);
				// �Ӹ�ͨ�����ֽ����ж�������Ļ�������
				socket.read(buffer);
				// ���������ֽ�����ķ�ʽ���Ƹ�data
				byte[] data = buffer.array();
				// ת�����ַ�������ȥ�ո�
				msg = new String(data).trim();

				if (!msg.isEmpty()) {
					// flag = true;
//					System.out.println("�ӷ�����յ���Ϣ��" + msg);

					FileOutputStream outputStream = new FileOutputStream(destfile);
					// �и�ʽ���пո� �׶�
					OutputFormat format = OutputFormat.createPrettyPrint();
					// �޿ո���,��Լ�ռ�
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
				System.out.println("xml�ļ������쳣");
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

	// �����߳�----�ж�ʱ���й���K-V��ֵ��
	private class time implements Runnable {
		@Override
		public void run() {
			// Auto-generated method stub
			while (true) {
				// ÿ5���ж�һ���Ƿ���timeout��K-V��
				for (Map.Entry<String, Integer> KV : timeout.entrySet()) {
					if (KV.getValue() == 0) {
						slavedb2.remove(KV.getKey());
						System.out.println("ɾ����ʱK-V:" + KV.getKey());
					}
				}
				// �ȴ�5��
				try {
					Thread.sleep(5 * 1000);
				} catch (InterruptedException e) {
					// generated catch block
					e.printStackTrace();
				}
				// ʹÿһ��K-V��ֵ�Ե��������ڼ���5��
				for (Map.Entry<String, Integer> KV : timeout.entrySet()) {
					int temp = KV.getValue() - 5;
					timeout.put(KV.getKey(), temp);
				}

			}
		}

	}

	// ��ȡ�ļ����ڴ���----�ڴ���̭----timeout
	public void initmap(File f) {
		SAXReader r = new SAXReader();
		try {
			org.dom4j.Document doc = r.read(f);

			Element e = doc.getRootElement();
			// System.out.println("sdjf");
			Iterator<Element> e1 = e.elementIterator();// ���ڵ��µ��ӽڵ㼯��
			// System.out.println(e1.hasNext());
			while (e1.hasNext()) {
				// System.out.println("sdf");
				Element tmpe = e1.next();
				if (tmpe.attributeValue("name").equals("kv")) {
					/**
					 * timeout----�ڴ���̭
					 */
					if (slavedb2.containsKey(tmpe.element("key").getText())) { // ����ڴ��д���K-V��ֵ��
						// K-V��ֵ��û�иı�
						if (slavedb2.get(tmpe.element("key").getText()).equals(tmpe.element("value").getText())) {
							// ��־λ����
							live = false;
						} else {
							// ��־λ��1
							live = true;
							System.out.println(tmpe.element("key").getText() + "��־λ��1��");
						}
					} else { // �ڴ��в�����K-V��ֵ��
						// �����ݶ��뵽�ڴ���
						System.out.println(
								tmpe.element("key").getText() + "-" + tmpe.element("value").getText() + "�����ڴ棡");
						slavedb2.put(tmpe.element("key").getText(), tmpe.element("value").getText());
						// ��־λ��1
						live = true;
					}
					// ��־λΪtrue
					if (live = true) {
						// ����K-V��������Ϊ20��
						System.out.println(
								tmpe.element("key").getText() + "-" + tmpe.element("value").getText() + "�����������ã�");
						timeout.put(tmpe.element("key").getText(), life);
						live = false;
					}
				}
			}
		} catch (DocumentException e) {
			// �Զ����ɵ� catch ��
			e.printStackTrace();
		}
	}

	/**
	 * ˽�л�һ��Slave�ϵ�selector
	 */
	private Selector selector;

	// ����selector�˿ڣ�����C2S���ٶ�ȡ����
	public void initS2C(int port) throws IOException {
		// �½�һ��socket
		ServerSocketChannel socket = ServerSocketChannel.open();
		// ��serversocketchannel��һ��port
		socket.socket().bind(new InetSocketAddress(port));

		// ע��һ��ͨ��������ͨ�����ڿɽ���״̬
		selector = selector.open();
		socket.configureBlocking(false);
		SelectionKey key = socket.register(selector, SelectionKey.OP_ACCEPT);

		System.out.println("Slaver����ͨ��������ɣ�");
	}

	// Slave��ʼ����channel
	public void listen() throws IOException {
		System.out.println("����������ɹ���");
		// ��ѯ����selector
		while (true) {
			// ��ע����¼�����ʱ���������أ�����,�÷�����һֱ����
			selector.select();
			// ���selector��ѡ�е���ĵ�������ѡ�е���Ϊע����¼�
			Iterator<SelectionKey> ite = this.selector.selectedKeys().iterator();
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
			System.out.println("slave������յ���Ϣ��" + msg);
			// ����ִ�������ķ��ؽ��

			String getC2Mstring = msg;
			String[] arg = strsplit(getC2Mstring); // �õ��ָ�������

			// ���ʹ�õ����е�K-V��ֵ�ԣ�����������������
			if (slavedb2.containsKey(arg[1])) {
				timeout.put(arg[1], life);
				System.out.println("����K-V����ֵ��" + arg[1]);
			}

			ByteBuffer outBuffer = ByteBuffer.wrap(methods(arg, slavedb2).getBytes());
			channel.write(outBuffer);// ����Ϣ���͵�ͨ����
			channel.close();

		} catch (IOException e) {
			// �Զ����ɵ� catch ��
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

	// get������ʵ��----����key����Ӧ��value
	public static String get(String[] arg, Map<String, String> map) {
		String value = "";
		if (arg.length != 2) {
			System.out.println("get�����������");
			value = "get�����������";
		} else {
			if (map.containsKey(arg[1])) {
				value = map.get(arg[1]);
			} else {
				value = "�����ڸ�key-value!";
			}
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

	// �ӷ�����������
	public static void main(String[] args) {
		Slave2 Slave2 = new Slave2();
		try {
			// Slave�ĳ�ʼ�������Ҹ����Զ����Ӹ��ƣ�40s���һ��
			Slave2.run();
			// ���ڼ����ͻ��˷���
			Slave2.initS2C(8012);
			Slave2.listen();

		} catch (IOException e) {
			// �Զ����ɵ� catch ��
			e.printStackTrace();
		}
	}

}
