package client;

import task.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.AbstractListModel;

/**
 *  @author lpm
 *聊天室客户端数据处理程序
 *和服务器类似，本类具有两个线程，分别处理发送信息到服务器以及接收服务器信息
 *两个任务。不同的是，本类只需要处理一个连接，也就是与服务器的Socket连接。
 */
public class ClientModel {
	
	protected Socket socket;	 			  //与Server连接的Soket
	protected String name;					//用户名
	protected AbstractListModel listModel; //用户名列表数据模型
	//发送信息队列和接收信息队列，被用来作为发送和接收信息的入口
	private BlockingQueue sendQueue,receiveQueue;
	private Vector<String> nameList; //用户名列表
	private Map<String,InetSocketAddress> name_address_map;//用户名与地址的对应表
	
	/**
	 * Method ClientModel
	 *
	 *
	 */
	public ClientModel(String ip,int port)
			throws IOException,UnknownHostException {
		
		socket=new Socket(ip,port);
		sendQueue=new LinkedBlockingQueue();
		receiveQueue=new LinkedBlockingQueue();
		nameList=new Vector<String>();
		name_address_map=new HashMap<String,InetSocketAddress>();
	}

	/**
	 *检验用户名是否有效
	 */
	
	public boolean validate(String name) throws IOException {
		
		DataOutputStream dos=new DataOutputStream(socket.getOutputStream());
		DataInputStream dis=new DataInputStream(socket.getInputStream());
		dos.writeUTF(name);
		if(dis.readBoolean()){
			this.name=name;
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 *取得用户名
	 */
	public String getName(){
		return name;
	}
	
	/**
	 *通过网络从服务器取得所有在线用户名列表
	 *通过ObjectInputStream包装套接字输入流，读取服务器在线用户。
	 *首先读取在线用户数，再依次读取用户名和网络地址。
	 */
	public Vector<String> getNames() throws Exception{
		Vector<String> v=new Vector<String>();
		ObjectInputStream dis=new ObjectInputStream(socket.getInputStream());
		int size=dis.readInt();
		for(int i=0;i<size;i++){
			String _name=dis.readUTF();
			InetSocketAddress address=(InetSocketAddress)dis.readObject();
			nameList.add(_name);
			name_address_map.put(_name,address);
			v.add(_name);
		}
		v.add(0,v.remove(v.size()-1));
		return v;
	}
	
	//接收和发送消息线程开始运行
	public void start(){
		try{
			new Receiver(socket,receiveQueue).start();
			new Sender(socket,sendQueue).start();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	//取得当前所使用的端口
	public int getLocalPort(){
		return socket.getLocalPort();
	}
	
	/**
	 *取得用户名对应的网络地址
	 */
	public SocketAddress getAddress(String name){
		return name_address_map.get(name);
	}
	/**
	 *取得客户端接收的信息。
	 *
	 */
	public Information getMessage(){
		Information message=null;
		try{
			Object object=receiveQueue.take();
			if(object instanceof Information){
				message=(Information)object;
				if(message.type==message.ENTER){
					if(!nameList.contains(message.source)){
						nameList.add(message.source);
						name_address_map.put(message.source,(InetSocketAddress)message.content);
					}
				}else if(message.type==message.EXIT){
					nameList.remove(message.source);
					name_address_map.remove(message.source);
				}
			}else if(object instanceof Socket){
				System.out.println("与服务器失去连接...");
				message=new Information(Information.EXIT,Setting.SERVER,null);
			}	
		}catch(InterruptedException e){
			e.printStackTrace();
		}
		return message;
	}
	
	/**
	 *加入消息，同时线程将消息自动发送给服务器
	 */
	public boolean putMessage(DefaultStyledDocument doc){
		Information info=new Information(Information.MESSAGE,name,doc);
		try{
			sendQueue.put(info);
			return true;
		}catch(InterruptedException e){
			e.printStackTrace();
			return false;
		}
	}
	
	public AbstractListModel getListModel(){
		if(listModel==null){
			listModel=new AbstractListModel(){
				public int getSize(){
					return nameList.size();
				}
				
				public Object getElementAt(int index){
					return nameList.get(index);
				}
			};
		}
		return listModel;
	}
}
