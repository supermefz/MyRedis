package com.hyw.SDS;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectOutputStream.PutField;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.print.attribute.standard.OutputDeviceAssigned;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import org.dom4j.*;

public class RDB implements Runnable{
	 //static HashMap<String,String> mapdb = new HashMap<String,String>();
	int i = 0;

	public void run(){
		
//		mapdb.put("a", "1");
//		mapdb.put("b", "2");
//		mapdb.put("c", "3");
		while(true){
		try {
			DocumentBuilderFactory dbf= DocumentBuilderFactory.newInstance();
			
			DocumentBuilder db= dbf.newDocumentBuilder();
			
			Document doc = db.newDocument();
		
			Element maps = doc.createElement("maps");
			
			Set<Entry<String, String>> set = Server.mapdb.entrySet();
			Iterator<Entry<String, String>> iter= set.iterator();
			while (iter.hasNext()) {
				Map.Entry<String, String> entry = iter.next();
				Element map = doc.createElement("map"+i);
				map.setAttribute("name", "kv");
				Element key = doc.createElement("key");
				key.setTextContent(entry.getKey());
				Element value = doc.createElement("value");
				value.setTextContent(entry.getValue());
				map.appendChild(key);
				map.appendChild(value);
				maps.appendChild(map);
				i++;
			}	
			doc.appendChild(maps);
			TransformerFactory tf=TransformerFactory.newInstance();
			Transformer t= tf.newTransformer();
			t.setOutputProperty(OutputKeys.INDENT, "yes");  
			t.transform(new DOMSource(doc), new StreamResult(Server.f));
			Thread.sleep(1000*3);
//		System.out.println(Server.mapdb.size());
		} catch (ParserConfigurationException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		} catch (TransformerConfigurationException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}
		}
	}
}
