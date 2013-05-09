package server;

import task.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;
import java.util.concurrent.*;
import javax.swing.text.*;
import javax.swing.table.AbstractTableModel;

/**@author lpm
 * 聊天室服务器数据模型 
 * 本类设计了一个监听一个端口的聊天室服务器处理程序，
 * 可以同时接受 多个客户连接，可以将任一客户传送的信息返还给所有客户。
 * 本类使用多线程处理数据，线程之间的数据传输使用了BlockingQueue工具。
 * 对于每一个客户连接使用了两个线程分别处理接收信息和发送信息。
 * 这两个线程的实现分别中类SendServicer和ReceiveSerivicer中。
 * 当服务器发现有一个客户连接发生时，取得一个Socket与其对话，同时建立一个与其对应的
 * BlockingQueue,将这个对应的Map.Entry加入一个Map中。当服务器有信息要
 * 传送给客户端时，将这个信息放入对应的BlockingQueue中就可以直接唤醒 SendSevicer阻塞的线程执行传输任务。
 * 
 */
public class ServerModel implements Runnable {

	protected Logger logger = Logger.getLogger("server.model");
	protected boolean running;
	protected int port;// 端口
	protected int mostConnect = 50;// 最大连接数，默认为50
	protected AbstractTableModel userTableModel;//用户列表模型 table类型
	private ServerSocket server;
	private BlockingQueue queue;// 接收信息队列
	/**
	 * 套接字与对应的传送队列
	 */
	private List<Socket> socketList;
	private Map<Socket, BlockingQueue> socket_queue_map;//套接字与对应的消息队列位置
	private Map<Socket, String> socket_name_map;// 套接字与对应的用户名
	private Map<Socket, Date> socket_date_map;

	/**
	 * Method main
	 * 
	 * 
	 * @param args
	 * 
	 */
	public static void main(String[] args) {
		
		new ServerModel();
	}

	/**
	 * Method ServerModel
	 */
	public ServerModel() {
		// 设置容器，放置相应属性值
		queue = new LinkedBlockingQueue();
		socketList = new LinkedList<Socket>();
		socket_queue_map = new HashMap<Socket, BlockingQueue>();
		socket_name_map = new HashMap<Socket, String>();
		socket_date_map = new HashMap<Socket, Date>();
	}

	/**
	 * 开始运行服务器 @ param port 监听端口
	 */
	public void startOn(int port) throws IOException {
		this.port = port;
		server = new ServerSocket(port);
		logger.info("starting server on port:" + port);
		running = true;
		new Thread(this).start();
		transact();
	}

	/**
	 * 关闭服务器监听
	 */
	public void stop() throws IOException {
		server.close();
		running = false;
	}

	/**
	 * 返回服务器当前状态 @ return 如果正在监听返回true
	 */
	public boolean isRunning() {
		return running;
	}

	/**
	 * 取得连接用户数量
	 * 
	 */
	public int getConnectNumber() {
		return socketList.size();
	}

	/**
	 * 设置最大连接数量
	 */
	public void setMostConnect(int mostConnect) {
		this.mostConnect = mostConnect;
	}

	/**
	 * AbstractTableModel,
	 * 取得一个关于用户列表的数据模型 return JTable的数据模型，实作ABstractTableModel抽象类
	 */
	public AbstractTableModel getUserTableModel() {
		if (userTableModel == null) {
			userTableModel = new AbstractTableModel() {
				protected String[] columnNames = { "用户名", "ip", "端口", "登录时间" };

				public int getRowCount() {
					return socketList.size();
				}

				public int getColumnCount() {
					return columnNames.length;
				}

				public String getColumnName(int column) {
					return columnNames[column];
				}

				public Object getValueAt(int row, int column) {
					switch (column) {
					case 0:
						return socket_name_map.get(socketList.get(row));
					case 1:
						return socketList.get(row).getInetAddress();
					case 2:
						return socketList.get(row).getPort();
					case 3:
						return socket_date_map.get(socketList.get(row));
					default:
						return null;
					}
				}

				public boolean isCellEditable(int row, int column) {
					return false;
				}
			};
		}
		return userTableModel;
	}

	/**
	 * 断开索引指向的在线用户连接 @ param index 用户索引，具体点就是SocketList的索引，是按 连接时间排序的
	 */
	public void removeUser(int index) throws IOException {
		Socket socket = socketList.get(index);
		socket.close();
	}

	/**
	 * 发送服务器系统消息 这个接口可用来实现服务器对所有再线用户发送一条消息 @ param info 消息对象
	 */
	public void sendMessage(Information info) {
		try {
			queue.put(info);
			logger.info(info.content.toString());
		} catch (InterruptedException e) {
			logger.warning(e.getMessage());
		}

	}

	/**
	 * Method run 
	 * 程序主线程，服务器等待连接 取得一个用户连接的Socket之后，
	 * 先验证用户信息后加再将其入用户列表，
	 * 然后运行该Socket的Sender和Receiver线程。
	 */
	public void run() {
		while (!server.isClosed()) {
			try {
				Socket socket = server.accept();
				if (socket_queue_map.size() >= mostConnect) {
					socket.close();//如果连接数量大于知道那个数量，断开套接字
				} else if (testName(socket)) {
					logger.info(socket_name_map.get(socket)
							+ socket.getRemoteSocketAddress() + "\t已连接...");
					sendNames(socket);//发送用户列表给指定socket用户
					new Sender(socket, socket_queue_map.get(socket)).start();
					new Receiver(socket, queue).start();
				}
			} catch (IOException e) {
				logger.warning(e.getMessage());
			}
		}
	}

	/**
	 * 1、验证用户名。 当通过套接字传送过来的登录用户名与正在使用中的用户名有没有重复 
	 * 2、同时将验证结果通过套接字反馈给登录用户。
	 * 3、将验证通过的用户加入 用户列表。
	 * 
	 */
	private boolean testName(Socket socket) throws IOException {

		DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
		DataInputStream dis = new DataInputStream(socket.getInputStream());
		String name = dis.readUTF();				//通过套接字获取用户名
		Collection<String> names = socket_name_map.values();
		boolean valid = Setting.isValidName(name) && !names.contains(name);
		dos.writeBoolean(valid);
		if (valid) {
			Information info = new Information(Information.ENTER, name,
					socket.getRemoteSocketAddress());
			try {
				queue.put(info);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return false;
			}
			addUser(socket, name);
		} else {
			dos.close();
			dis.close();
			socket.close();
		}
		return valid;
	}

	/**
	 * 将用户信息加入用户列表，主要包含Socket,Name,Queue,Date等信息。
	 */
	protected void addUser(Socket socket, String name) {
		socketList.add(socket);
		socket_queue_map.put(socket, new SynchronousQueue());
		socket_name_map.put(socket, name);
		socket_date_map.put(socket, new Date());
		if (userTableModel != null)
			userTableModel.fireTableDataChanged();
	}

	/**
	 * 移除指定socket用户信息
	 */
	protected void removeUser(Socket socket) {
		socketList.remove(socket);
		socket_queue_map.remove(socket);
		socket_name_map.remove(socket);
		socket_date_map.remove(socket);
		if (userTableModel != null)
			userTableModel.fireTableDataChanged();
	}

	/**
	 * 发送用户列表给指定socket用户
	 * 
	 */
	private void sendNames(Socket socket) throws IOException {
		ObjectOutputStream dos = new ObjectOutputStream(socket.getOutputStream());

		int size = socketList.size();
		dos.writeInt(size);
		for (int i = 0; i < size; i++) {
			Socket s = socketList.get(i);
			String name = socket_name_map.get(s);
			dos.writeUTF(name);
			// 发送给指定的ip客户端
			dos.writeObject(s.getRemoteSocketAddress());
		}
	}

	/**
	 * 数据交换处理 这个过程是整个服务器程序处理的核心 通过queue统一接收来自所有用户以及服务器系统发送的信息，通过
	 * 类型甄别做出不同处理，再将处理结果转发给所有在线连接用户。
	 */
	private void transact() {
		new Thread(new Runnable() {
			public void run() {
				while (running) {
					Information info;
					try {
						Object object = queue.take();//从消息队列中取出对象，分别处理
						if (object instanceof Information) {
							// 正常传送的信息
							info = (Information) object;
							if (info.type == Information.MESSAGE) {
								if (info.content instanceof StyledDocument) {
									StyledDocument doc = (StyledDocument) info.content;
									try {
										logger.log(
												Level.INFO,
												doc.getText(0, doc.getLength()),
												info.source);
									} catch (BadLocationException be) {
										logger.warning(be.getMessage());
									}
								}
							}
						} else if (object instanceof Socket) {// 退出的socket
							Socket socket = (Socket) object;
							logger.info(socket.getRemoteSocketAddress()
									+ "\t退出...");
							String name = socket_name_map.get(socket);
							info = new Information(Information.EXIT, name, null);
							removeUser(socket);
						} else {
							continue;
						}

						// 遍历每个Socket，info发送给socket
						Iterator<Map.Entry<Socket, BlockingQueue>> i = socket_queue_map
								.entrySet().iterator();
						while (i.hasNext()) {
							Map.Entry<Socket, BlockingQueue> entry = i.next();
							entry.getValue().put(info);//把info推送到每个与服务器连接的socket
						}
						
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}
}
