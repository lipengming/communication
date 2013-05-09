package server;

import task.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import java.util.*;
import java.util.logging.*;

/**
 * @author lpm
 * 聊天室服务器主程序 这是运行聊天室服务器的程序入口。
 */
public class Server extends JFrame implements ActionListener {

	protected Logger logger = Logger.getLogger("server");// 日志构造器
	private ServerModel model;// 服务器数据模型
	JTabbedPane tabbedPane;
	private JTextPane logArea;// 日志显示栏
	private JTable table;// 在线用户列表
	private JButton startButton, closeButton, exitButton;// 开始，退出，关闭按钮
	private JFormattedTextField portField, sizeField;

	/**
	 * Method main
	 * 
	 * 
	 * @param args
	 * 
	 */
	public static void main(String[] args) {
		createAndShowGUI();
	}

	/**
	 * ethod Server
	 * 
	 * 
	 */
	public Server() {
		super("聊天室服务器");
		// 构建服务器模型
		model = new ServerModel();
		// 创建界面
		buildUI();
		// 配置日志
		configureLogging();
		//添加窗体事件
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				exit();
			}
		});
	}

	/**
	 * 布局构造GUI
	 */
	private void buildUI() {
		Container contentPane = getContentPane();
		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.addTab("日志", createLogPane());
		tabbedPane.addTab("参数", createParamPane());
		tabbedPane.addTab("用户列表", createUserPane());
		tabbedPane.setSelectedIndex(1);
		tabbedPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		contentPane.add(tabbedPane, BorderLayout.CENTER);
		contentPane.add(createButtonPane(), BorderLayout.SOUTH);
	}

	/**
	 * 创建日志面板
	 */
	private Container createLogPane() {
		logArea = new JTextPane();
		logArea.setEditable(false);
		logArea.setBackground(Color.black);
		logArea.setForeground(Color.red);
		logArea.setPreferredSize(new Dimension(600, 400));
		return new JScrollPane(logArea,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	}

	/**
	 * 创建参数设置面板
	 */
	private Container createParamPane() {
		// 构造文本框
		portField = new JFormattedTextField(new Integer(8001));
		sizeField = new JFormattedTextField(new Integer(50));
		portField.setColumns(15);
		JPanel paramPane = new JPanel();
		JPanel c = new JPanel(new SpringLayout());
		c.setBorder(BorderFactory.createEmptyBorder(40, 10, 10, 10));
		addLabel(c, portField, "服务器端口");
		addLabel(c, sizeField, "最大连接数");
		// 使用SpringUtiles布局
		SpringUtilities.makeCompactGrid(c, 2, 2, 10, 10, 10, 10);
		paramPane.add(c);
		return paramPane;
	}

	/**
	 * 创建用户面板
	 */
	private Container createUserPane() {
		// 创建用户列表
		table = new JTable(model.getUserTableModel());
		JScrollPane tablePane = new JScrollPane(table);
		// 创建按钮，用于断开选择用户
		Action remove_action = new AbstractAction("断开所选用户") {
			public void actionPerformed(ActionEvent e) {
				int[] selectedRows = table.getSelectedRows();
				try {
					for (int index : selectedRows)
						model.removeUser(index);
				} catch (IOException ie) {
					ie.printStackTrace(System.err);
				}
			}
		};
		// 创建按钮，用于断开全部用户
		Action remove_all_action = new AbstractAction("断开所有用户") {
			public void actionPerformed(ActionEvent e) {
				int option = JOptionPane.showConfirmDialog(Server.this,
						"你真的要强行断开所用用户连接吗？", "请您选择", JOptionPane.YES_NO_OPTION);
				if (option == JOptionPane.YES_OPTION) {
					try {
						for (int i = 0; i < table.getRowCount(); i++) {
							model.removeUser(i);
						}
					} catch (IOException ie) {
						ie.printStackTrace(System.err);
					}
				}
			}
		};
		JButton remove_button = new JButton(remove_action);
		JButton remove_all_button = new JButton(remove_all_action);
		JPanel buttons = new JPanel();
		buttons.add(remove_button);
		buttons.add(remove_all_button);
		// 创建文本框，用于发送系统消息
		final JTextField textField = new JTextField();
		textField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Information info = new Information(Information.MESSAGE,
						Setting.SERVER, textField.getText());
				model.sendMessage(info);
				textField.setText("");
			}
		});
		JLabel label = new JLabel("发送系统消息");
		label.setLabelFor(textField);
		JPanel fieldPane = new JPanel(new BorderLayout());
		fieldPane.add(label, BorderLayout.WEST);
		fieldPane.add(textField);
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(tablePane);
		panel.add(buttons, BorderLayout.NORTH);
		panel.add(fieldPane, BorderLayout.SOUTH);
		return panel;
	}

	private Container createButtonPane() {
		// 构造按钮
		startButton = new JButton("启动");
		closeButton = new JButton("关闭");
		exitButton = new JButton("退出");
		startButton.addActionListener(this);
		closeButton.addActionListener(this);
		exitButton.addActionListener(this);
		closeButton.setEnabled(false);
		JPanel buttonPane = new JPanel();
		buttonPane.add(startButton);
		buttonPane.add(closeButton);
		buttonPane.add(exitButton);
		return buttonPane;
	}

	/**
	 * Method actionPerformed
	 * 
	 * 
	 * @param e
	 * 
	 */
	public void actionPerformed(ActionEvent e) {
		// TODO: 在这添加你的代码
		if (e.getSource() == startButton) {
			int port = (Integer) portField.getValue(), mostConnect = (Integer) sizeField
					.getValue();
			model.setMostConnect(mostConnect);
			try {
				model.startOn(port);
				startButton.setEnabled(false);
				closeButton.setEnabled(true);
				portField.setEditable(false);
				tabbedPane.setSelectedIndex(0);
			} catch (IOException ie) {
				logger.warning(ie.getMessage());
				JOptionPane.showMessageDialog(this, ie.getMessage());
			}
		} else if (e.getSource() == closeButton) {
			if (model.getConnectNumber() != 0) {
				JOptionPane.showMessageDialog(Server.this,
						"有用户正连接在服务器上，请先关闭用户连接！");
				tabbedPane.setSelectedIndex(2);
			} else {
				try {
					model.stop();
					startButton.setEnabled(true);
					closeButton.setEnabled(false);
					portField.setEditable(true);
					logger.info("server closed.");
					tabbedPane.setSelectedIndex(1);
				} catch (Exception ie) {
					ie.printStackTrace();
				}
			}

		} else if (e.getSource() == exitButton) {
			exit();
		}
	}

	public static void createAndShowGUI() {
		JFrame server = new Server();
		server.pack();
		server.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		server.show();
	}

	private static void addLabel(Container c, Component tc, String label) {
		JLabel l = new JLabel(label);
		l.setLabelFor(tc);
		c.add(l);
		c.add(tc);
	}

	private void exit() {
		if (model.isRunning()) {
			int option = JOptionPane.showConfirmDialog(this,
					"服务器正在运行中，您确定退出吗？", "请选择", JOptionPane.YES_NO_OPTION);
			if (option != JOptionPane.YES_OPTION)
				return;
		}
		logger.info("application is exited.");
		System.exit(0);
	}

	/**
	 * 配置日志
	 */
	protected void configureLogging() {
		logger.setUseParentHandlers(false);
		logger.addHandler(new TextPaneHandler(logArea));
		// 配置文件记录日志
		try {
			Handler fileHandle = new FileHandler("server%g.log", 1000000, 2,
					true);
			fileHandle.setFormatter(new InfoFormatter());
			logger.addHandler(fileHandle);
		} catch (IOException e) {
			logger.warning(e.getMessage());
		}
		logger.info("启动日志......");
	}
	/**
	 * 自定义日志处理器，用于将日志记录在一个JTextPane之中
	 */
	class TextPaneHandler extends Handler {
		protected JTextPane logArea;
		protected SimpleAttributeSet attributeSet;// 信息之下级别日志属性
		protected SimpleAttributeSet focusAttributeSet;// 错误及警告级别日志属性

		public TextPaneHandler(JTextPane textPane) {
			logArea = textPane;
			setFormatter(new InfoFormatter());
			attributeSet = new SimpleAttributeSet();
			focusAttributeSet = new SimpleAttributeSet();
			StyleConstants.setForeground(attributeSet, Color.RED);
			StyleConstants.setFontFamily(attributeSet, "宋体");
			StyleConstants.setFontSize(attributeSet, 14);

			StyleConstants.setForeground(focusAttributeSet, Color.LIGHT_GRAY);
			StyleConstants.setFontFamily(focusAttributeSet, "宋体");
			StyleConstants.setFontSize(focusAttributeSet, 14);

		}

		public void publish(LogRecord record) {
			String info = getFormatter().format(record);
			Document docs = logArea.getDocument();
			try {
				if (record.getLevel().intValue() > Level.INFO.intValue()) {
					docs.insertString(docs.getLength(), info, attributeSet);
				} else {
					docs.insertString(docs.getLength(), info, focusAttributeSet);
				}

				logArea.setCaretPosition(docs.getLength());
			} catch (BadLocationException e) {
				logger.warning(e.getMessage());
			}
		}

		public void close() {
		}

		public void flush() {
		}
	}

	/**
	 * 自定义日志处理格式，当日志级别为"info"时，按对话信息处理
	 */
	class InfoFormatter extends SimpleFormatter {
		String newline = System.getProperty("line.separator");
		java.text.DateFormat dateFormatter = new java.text.SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss ");

		public String format(LogRecord record) {
			if (record.getLevel() != Level.INFO) {
				return super.format(record);
			} else {
				Object[] params = record.getParameters();
				String param = "";
				if (params != null)
					param = params[0].toString();
				return dateFormatter.format(new Date()) + param + newline
						+ "信息：" + record.getMessage() + newline;
			}
		}
	}
}