package client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UTFDataFormatException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Date;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import task.Information;
import task.Setting;
import task.VoiceChat;
/**
 * 
 * @author lpm
 *
 */
public abstract class ClientUI extends JFrame 
		implements Runnable, ActionListener, Observer{
	
	private ClientModel model;
	private UDPClientModel udpModel;
	private UDPClient udpClient;
	private Vector<String> names;
	private JList nameList;
	private JTextPane sendArea;
	private JTextPane receiveArea;
	protected java.text.SimpleDateFormat format;
	protected String newline=System.getProperty("line.separator");
	protected String name;
	protected SimpleAttributeSet sourceAttribute;
	protected SimpleAttributeSet serverAttribute;

	/**
	 * Method ClientUI
	 *
	 *
	 */
	public ClientUI(ClientModel mod) {
		// TODO: 在这添加你的代码
		model=mod;
		try{
			udpModel=new UDPClientModel(model.getLocalPort());
			udpModel.addObserver(this);
		}catch(IOException e){
			e.printStackTrace();
		}
		try{
			names=model.getNames();
		}catch(Exception e){
			e.printStackTrace();
		}
		name=model.getName();
		format=new java.text.SimpleDateFormat("HH:mm:ss");
		
		nameList=new JList(names);
		sendArea=new JTextPane();
		receiveArea=new JTextPane();
		nameList.setCellRenderer(new CellRenderer());

		layoutUI();
		
		addUDPListenning();
		createAttributeSets();
		new Thread(this).start();
		addWindowFocusListener(new WindowAdapter(){
			public void windowGainedFocus(WindowEvent e){
				sendArea.requestFocusInWindow();
			}
		});
	}

	private void layoutUI(){
		//設定用户列表大小
		nameList.setFixedCellWidth(140);
		nameList.setFixedCellHeight(20);
		
		receiveArea.setEditable(false);
		// 加入滚动栏
		JScrollPane scrollPane1=new JScrollPane(nameList);
		JScrollPane scrollPane2=new JScrollPane(sendArea);
		JScrollPane scrollPane3=new JScrollPane(receiveArea);
		scrollPane1.setBorder(BorderFactory.createTitledBorder("用户列表"));
		scrollPane1.setOpaque(false);
		scrollPane2.setOpaque(false);
		scrollPane3.setOpaque(false);
		
		
		JPanel work_pane=new JPanel(new BorderLayout()),
			   button_pane=new JPanel(new FlowLayout(FlowLayout.RIGHT));
		work_pane.setOpaque(false);
		button_pane.setOpaque(false);
		//button_pane.add(new JLabel("双击用户列表进入单聊界面"));
		//设定sendButton的快捷键为ctrl+Enter
		KeyStroke stroke=KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,ActionEvent.CTRL_MASK,true);
		Setting.createButton("退出(E)",'E',"exit",null,button_pane,this);
		Setting.createButton("发送(S)",'S',"send",stroke,button_pane,this);
		work_pane.add(new EditToolBar(sendArea),BorderLayout.NORTH);
		work_pane.add(scrollPane2);
		work_pane.add(button_pane,BorderLayout.SOUTH);
		
		setLayout(new BorderLayout());
		JSplitPane sp1=new JSplitPane(JSplitPane.VERTICAL_SPLIT,true,
										scrollPane3,work_pane);
		sp1.setResizeWeight(0.75);
		sp1.setPreferredSize(new Dimension(350,400));
		
		sp1.setOpaque(false);

		sp1.setDividerSize(1);
		sp1.setBorder(BorderFactory.createEmptyBorder(18,10,0,0));
		Container contentPane=getContentPane();
		
		contentPane.add(sp1);
		contentPane.add(scrollPane1,BorderLayout.EAST);
		contentPane.setBackground(Setting.color1);
		pack();
	}

	private void addUDPListenning(){
		//双击鼠标，进入单聊界面
		nameList.addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount()==2){
					String remoteName=(String)nameList.getSelectedValue();
					if(remoteName.equals(name)){
						JOptionPane.showMessageDialog(ClientUI.this,
							"您不能和自己交谈！");
					}else{
						if(udpClient==null){
							udpClient=new UDPClient(udpModel,name);
							udpClient.addObserver(ClientUI.this);
						}
						udpClient.setRemoteSymbol(remoteName,model.getAddress(remoteName));
						udpClient.showIn(ClientUI.this);
					}
				}
			}
		});
	}
	
	private void createAttributeSets(){
		sourceAttribute=new SimpleAttributeSet();
		serverAttribute=new SimpleAttributeSet();
		StyleConstants.setForeground(sourceAttribute,Color.blue);
		StyleConstants.setForeground(serverAttribute,new Color(0,128,64));
	}
	/**
	 * Method run
	 *
	 *
	 */
	public void run(){
		while(true){
			Information info=model.getMessage();
			if(info==null){
				continue;
			}else if(info.type==Information.ENTER){
				//一个客户段加入
				if(!names.contains(info.source)){
					String serverMessage=format.format(new Date())+"\t"+info.source+" 进来了..."+newline;
					try{
						insertMessage(serverMessage,serverAttribute);
					}catch(BadLocationException e){
						System.err.println(e.getMessage());
					}
					
					names.add(info.source);
					nameList.updateUI();
				}
			}else if(info.type==Information.EXIT){
				//一个客户端离开
				if(info.source==Setting.SERVER){
					doWhenStop();
					break;
				}else{
					String serverMessage=format.format(new Date())+"\t"+info.source+" 离开了..."+newline;
					try{
						insertMessage(serverMessage,serverAttribute);
					}catch(BadLocationException e){
						System.err.println(e.getMessage());
					}
					names.remove(info.source);
					nameList.updateUI();
				}					
			}else if(info.type==Information.MESSAGE){
				try{
					//系统端发送消息
					if(info.source.equals(Setting.SERVER)){
						insertMessage(format.format(new Date())+newline+
							"[系统消息]  "+info.content+newline,serverAttribute);
					}else{
						String source=info.source+"  ("+format.format(new Date())+")"+newline;
						insertMessage(source,sourceAttribute);
						insertMessage((StyledDocument)info.content);
					}
				}catch(BadLocationException e){
					System.err.println(e.getMessage());
				}
			}
			
		}
	}

	/**
	 * Method actionPerformed
	 *添加消息控制命令
	 *
	 * @param e
	 *
	 */
	public void actionPerformed(ActionEvent e) {
		// TODO: 在这添加你的代码
		String command=e.getActionCommand();
		if(command.equals("exit")){
			exit();
		}else if(command.equals("send")){
			DefaultStyledDocument doc=(DefaultStyledDocument)sendArea.getStyledDocument();
			if(doc.getLength()==0){
				JOptionPane.showMessageDialog(this,"请不要发送空信息！");
			}else{
				model.putMessage(doc);
				sendArea.setDocument(sendArea.getEditorKit().createDefaultDocument());
			}
		}
	}
	/*
	 * 在接收框中显示信息
	 */
	
	protected void insertMessage(String message,SimpleAttributeSet attset)
			throws BadLocationException {
		Document docs=receiveArea.getDocument();
		docs.insertString(docs.getLength(),message,attset);
		receiveArea.setCaretPosition(docs.getLength());
	}
	protected void insertMessage(StyledDocument doc)
			throws BadLocationException {
		
		StyledDocument receive_doc=receiveArea.getStyledDocument();
		int base=receive_doc.getLength();
		String text=doc.getText(0,doc.getLength())+newline;

		receive_doc.insertString(base,text,null);
		LinkedList<Element> list=new LinkedList<Element>();
		for(Element e:doc.getRootElements()){
			Setting.getAllElements(list,e);
		}
		for(Element e:list){
			int offset=base+e.getStartOffset(),
				length=e.getEndOffset()-e.getStartOffset();
			receive_doc.setCharacterAttributes(offset,length,e.getAttributes(),false);
		}
		receiveArea.setCaretPosition(receive_doc.getLength());
	}
	
	protected void exit(){
		int option=JOptionPane.showConfirmDialog(this,"程序正连接到服务器上，您确定退出吗？",
			"请您选择",JOptionPane.YES_NO_OPTION);
		if(option==JOptionPane.YES_OPTION)
			System.exit(0);	
	}
	                
	protected abstract void doWhenStop();

	/**
	 * Method update
	 *
	 *
	 * @param o
	 * @param arg
	 *
	 */
	public void update(Observable o, Object object) {
		if(o==udpModel){
			if(object instanceof Information){
				final Information info=(Information)object;
				if (info.type==Information.FILE) {
					int response;
					response = JOptionPane.showConfirmDialog(null, "确认接收文件？", "*文件接受请求消息*",
							JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
					if (response==JOptionPane.YES_OPTION) {
						Thread thread = new Thread(){
							public void run(){	
								String savePath="E:\\"+(String) info.content;
								
							      try {	     
								      // 通过Socket连接文件服务器
								      Socket server=new Socket(InetAddress.getLocalHost(),3108);								      
								      //创建网络接受流接受服务器文件数据
								      InputStream netIn=server.getInputStream();
								      
								      InputStream in=new DataInputStream(new BufferedInputStream(netIn));
								      System.out.println(savePath);
								    //使用本地文件系统接受网络数据并存为新文件
								      File file=new File(savePath);
								      file.createNewFile();
								      RandomAccessFile raf=new RandomAccessFile(file,"rw");
								      
								      //创建缓冲区缓冲网络数据
								      byte[] buf=new byte[2048];
								      int num=in.read(buf);
								     
								      while(num!=(-1)){//是否读完所有数据
								         raf.write(buf,0,num);//将数据写往文件
								         raf.skipBytes(num);//顺序写文件字节
								         num=in.read(buf);//继续从网络中读取文件
								      }
								      JOptionPane.showMessageDialog(null, "！文件接受完成啦！", "消息", 1);
								      in.close();
								      raf.close();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}catch(Exception exception){
									exception.printStackTrace();
								}
							   
							}
						};
						thread.start();
					}else if(response==JOptionPane.NO_OPTION) {
						System.out.println("文件传送连接失败！");
					}
				
				}else if(info.type ==Information.VOICE){
					int response = JOptionPane.showConfirmDialog(null,
							   "确定接受对方语音请求？",
							   "语音请求",
							   JOptionPane.YES_NO_OPTION,
							   JOptionPane.QUESTION_MESSAGE);
					if (response==JOptionPane.YES_OPTION) {	
						Thread thread = new Thread(){
							@Override
							public void run() {	
								String remoteName=(String)nameList.getSelectedValue();
								String remoteStr=model.getAddress(remoteName).toString();
								int a = remoteStr.indexOf(":");
								String ip = remoteStr.substring(1, a);
								System.out.println(remoteStr+"============"+ip);
								VoiceChat vc = new VoiceChat(ip);
								
							}
						};
						thread.start();
					}else if(response==JOptionPane.NO_OPTION){
						System.out.println("语音连接失败！");
					}
				}else{
					try{
						String source=info.source+"  ("+format.format(new Date())+")  悄悄对你说:"+"\n";
						insertMessage(source,sourceAttribute);
						insertMessage((StyledDocument)info.content);
					}catch(BadLocationException e){
						System.err.println(e.getMessage());
					}
				}
				
			}
		}else if(o==udpClient){
			Information info=(Information)object;
			try{
				String source=format.format(new Date())+"\t你  悄悄对"+info.source+"说"+newline;
				insertMessage(source,sourceAttribute);
				insertMessage((StyledDocument)info.content);
			}catch(BadLocationException e){
				System.err.println(e.getMessage());
			}
		}
		
	}
	//用于设置list相关属性
	protected class CellRenderer extends DefaultListCellRenderer{
		@Override
		public Component getListCellRendererComponent(JList list,
													Object value,
													int index,
													boolean isSelected,
													boolean cellHasFocus){
			super.getListCellRendererComponent(list,value,index,isSelected,cellHasFocus);
			if(value.equals(name))
				setForeground(Color.red);
			return this;												
		}
	}
}
