package client;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Observable;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.text.DefaultStyledDocument;

import task.Information;
import task.Setting;
import task.VoiceChat;

/**@author lpm
 * UDP客户端，用于用户之间单独聊天 本类含有一个独立的JFrame,当发送完信息之后隐藏窗口。
 */
public class UDPClient extends Observable implements ActionListener {

	protected JFrame frame;
	protected UDPClientModel model;
	private JLabel label;
	private JTextPane editor;
	protected String name;
	protected String remoteName;
	protected SocketAddress remoteAddress;

	/**
	 * Method UDPClient
	 * 
	 * 
	 */
	public UDPClient(UDPClientModel model, String name) {
		this.model = model;
		this.name = name;
		label = new JLabel();

		frame = new JFrame();
		// 创建编辑框
		editor = new JTextPane();
		editor.setPreferredSize(new Dimension(350, 180));
		JScrollPane editorPane = new JScrollPane(editor);
		editorPane.setOpaque(false);
		// 创建按钮
		JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPane.setOpaque(false);
		KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
				ActionEvent.CTRL_MASK, true);
		Setting.createButton("语音(V)", 'C', "voice", stroke, buttonPane, this);
		Setting.createButton("文件(F)", 'C', "file", stroke, buttonPane, this);
		Setting.createButton("关闭(C)", 'C', "close", null, buttonPane, this);
		Setting.createButton("发送(S)", 'C', "send", stroke, buttonPane, this);

		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.setBackground(Setting.color1);
		contentPane.setBorder(BorderFactory.createEmptyBorder(15, 10, 10, 10));
		contentPane.add(new EditToolBar(editor), BorderLayout.NORTH);
		contentPane.add(editorPane, BorderLayout.CENTER);
		contentPane.add(buttonPane, BorderLayout.SOUTH);
		frame.setContentPane(contentPane);
		frame.pack();
	}

	public void setRemoteSymbol(String name, SocketAddress address) {
		remoteName = name;
		remoteAddress = address;
		frame.setTitle(remoteName + " - 发送消息");
	}

	// 按指定窗口的位置显示
	public void showIn(Component owner) {
		frame.setLocationRelativeTo(owner);
		frame.show();
	}

	// 发送信息过程
	protected boolean send() throws java.io.IOException {
		DefaultStyledDocument doc = (DefaultStyledDocument) editor
				.getStyledDocument();
		if (doc.getLength() == 0) {
			JOptionPane.showMessageDialog(frame, "请不要发送空消息!");
			return false;
		} else {
			Information info = new Information(Information.MESSAGE, name, doc);
			model.send(info, remoteAddress);
			setChanged();
			notifyObservers(new Information(Information.MESSAGE, remoteName,
					doc));
			editor.setDocument(editor.getEditorKit().createDefaultDocument());
			return true;
		}
	}

	/**
	 * Method actionPerformed
	 * 
	 * @param e
	 * 
	 */
	public void actionPerformed(ActionEvent e) {
		// TODO: 在这添加你的代码
		String command = e.getActionCommand();
		if (command.equals("close")) {
			frame.dispose();
		} else if (command.equals("send")) {
			try {
				if (send()) {
					frame.dispose();
				}
			} catch (Exception ie) {
				ie.printStackTrace();
			}

		} else if (command == "file") {
			JFileChooser jf = new JFileChooser(); // creat file
			// chooser
			jf.setPreferredSize(new Dimension(400, 300));
			int retnrnVal = jf.showOpenDialog(jf);
			final String myfile = jf.getSelectedFile().getPath();
			String filename = jf.getSelectedFile().getName();	
				Thread thread = new Thread() {
					public void run() {
						
						File file = new File(myfile);
						
						FileInputStream fos;
						try {
							fos = new FileInputStream(file);
							// 注意这里使用的是字节流，因为图像信息可以用二进制传输

							// 创建网络服务器接受客户请求
							ServerSocket ss = new ServerSocket(3108);
							Socket client = ss.accept();

							// 创建网络输出流并提供数据包装器
							OutputStream netOut = client.getOutputStream();
							OutputStream doc = new DataOutputStream(
									new BufferedOutputStream(netOut));

							// 创建文件读取缓冲区
							byte[] buf = new byte[2048];
							int num = fos.read(buf);
							while (num != (-1)) {// 是否读完文件
								doc.write(buf, 0, num);// 把文件数据写出网络缓冲区
								doc.flush();// 刷新缓冲区把数据写往客户端
								num = fos.read(buf);// 继续从文件中读取数据
							}
							fos.close();
							doc.close();

						} catch (FileNotFoundException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} catch (IOException exception) {
							// TODO Auto-generated catch block
							exception.printStackTrace();
						}
					}
				};
				thread.start();
				{	
					
					Information info = new Information(Information.FILE, name, filename);
					try {
						model.send(info, remoteAddress);
					} catch (IOException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
					setChanged();
					notifyObservers(new Information(Information.FILE, remoteName,filename));
				}
		}else if(command == "voice"){
			int response = JOptionPane.showConfirmDialog(null,
					   "确定发送语音请求？",
					   "语音请求",
					   JOptionPane.YES_NO_OPTION,
					   JOptionPane.QUESTION_MESSAGE);
			if (response==JOptionPane.YES_OPTION) {	
				
				Thread thread = new Thread(){
					@Override
					public void run() {
						String remotesString = remoteAddress.toString();
						int a = remotesString.indexOf(":");
						String ip = remotesString.substring(1, a);
						System.out.println(ip+"+++++++"+remoteAddress.toString());
						
						VoiceChat vChat = new VoiceChat(ip);								
					}
				};
				thread.start();
				{	
					Information info = new Information(Information.VOICE, name, "VOICE");
					try {
						model.send(info, remoteAddress);
					} catch (IOException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
					setChanged();
					notifyObservers(new Information(Information.VOICE, remoteName,"VOICE"));
				}
				//thread.start();
			}else if (response==JOptionPane.NO_OPTION) {
				this.frame.dispose();
			}	
		}
		
	}
}
