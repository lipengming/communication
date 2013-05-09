package task;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import javax.swing.text.Element;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.Container;
import java.awt.event.ActionListener;

/**
 *属性设置类
 *包含一些程序属性字段和类方法
 */
public class Setting {
	
	/**
	 *UI 界面主色调Color
	 */
	public static java.awt.Color color1=new java.awt.Color(115,186,255);
	
	/**
	 *备选色调Color
	 */
	public static java.awt.Color color2=new java.awt.Color(115,255,186);
	
	/**
	 *备选色调Color
	 */
	public static java.awt.Color color3=new java.awt.Color(160,188,242); 
	/**
	 *设定消息来源为服务器的 source 字符
	 */
	public static final String SERVER="server";
	
	/**
	 *设定为程序急用，用户不能作为用户名使用的字符串
	 */
	protected static String[] invalidName={
		"","true","false","yes","no","server","client","enter","exit","to"
	};
	
	/**
	 *设定为在用户名中不能出现的非法字符
	 */
	protected static char[] invalidChar={
		'\\','/','.',':','(',')','[',']','-',
	};
	
	/**
	 *用来装载invalidName 的容器
	 */
	protected static Set<String> invalidNameSet=new HashSet<String>();
	
	static{
		for(int i=0;i<invalidName.length;i++)
			invalidNameSet.add(invalidName[i]);
	}
	
	public static void main(String[] args){
		System.out.println(isValidName("   "));
	}
	
	/**
	 * Method isInvalidName
	 * 测定一个字符串用户名在系统中是否被认为是合法用户名
	 *
	 * @param name 被测试的用户名
	 *
	 * @return true if the name if valid
	 *
	 */
	public static boolean isValidName(String name) {
		for(int i=0;i<invalidChar.length;i++){
			if(name.indexOf(invalidChar[i])!=-1)
				return false;
		}
		return !invalidNameSet.contains(name.trim());	
	}
	
	/**
	 * Method getAllElements
	 * 取得Document中指定一个Element的所有子Element，Element的结构是一个树形结构
	 * 遍历时按深度遍历次序
	 * 加入到指定List中。
	 * 该方法是用来取得Document的内部属性结构。
	 * @param list 盛放取得的Element序列的有序容器
	 * @param Element 被遍历的父Element
	 */
	public static void getAllElements(List<Element> list,Element root){
		if(root.isLeaf()){
			list.add(root);
		}else{
			for(int i=0;i<root.getElementCount();i++)
				getAllElements(list,root.getElement(i));
		}
	}
	
	/**
	 * Method createButton
	 * 按给定的参数构造一个JButton组件
	 * @param text button组件显示的text
	 * @param mn 指定的Mnemonic 其精度就小于等于char的精度
	 * @param command button组件的actionCommand
	 * @param stroke button组件对应的快捷键，其有效应用范围是WHEN_IN_FOCUSED_WINDOW
	 * @param Container button组件被加入的容器
	 * @param als 组件被注册的ActionListener
	 */
	public static JButton createButton(String text,int mn,String command,
			KeyStroke stroke,Container c,ActionListener als){
		JButton button=new JButton(text);
		button.setMnemonic(mn);
		button.setActionCommand(command);
		button.addActionListener(als);
		button.registerKeyboardAction(als,command,stroke,JComponent.WHEN_IN_FOCUSED_WINDOW);
		button.setFocusable(false);
		c.add(button);
		return button;
	}	
}
