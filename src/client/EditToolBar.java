package client;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;

import task.VoiceChat;
import static javax.swing.text.StyledEditorKit.*;
import java.awt.event.FocusListener;
import java.util.EventListener;


/**@author lpm
 *创建作用于指定文本属性编辑工具的工具栏
 */
public class EditToolBar extends JToolBar implements ActionListener, CaretListener, FocusListener{

	JTextPane editor;
	protected JFileChooser fileChooser;
	protected AbstractButton bold,italic,colors,pictures,underline;
	protected JComboBox fonts,sizes;
	private Container ancestor; 
	protected static String BOLD="加粗";
	protected static String ITALIC="倾斜";
	protected static String UNDERLINE="下划线";
	protected static String FONT="字体";
	protected static String SIZE="字号";
	protected static String PICTURE="插入图片";
	protected static String COLOR="字体颜色";
	
	/**
	 * Method EditToolBar
	 *
	 *
	 */
	public EditToolBar(JTextPane editor) {
		
		super(HORIZONTAL);
		this.editor=editor;
//		this.setOpaque(false);
		createAndAddItems();                              //GUI设置
		editor.addCaretListener(this);					//设置事件
//		editor.addFocusListener(this);
		fileChooser=new JFileChooser();
		fileChooser.addChoosableFileFilter(new PictureFilter());
	}
	
	protected void createAndAddItems(){
		//创建font属性
		fonts=new JComboBox(getAllFonts());
		fonts.setFocusable(false);
		fonts.setMaximumSize(new Dimension(120,20));
		fonts.setMinimumSize(new Dimension(120,20));
		fonts.setPreferredSize(new Dimension(120,20));
		fonts.addActionListener(this);
		fonts.setToolTipText(FONT);
		add(fonts);
		//创建font大小
		sizes=new JComboBox(getFontSizeArray());
		sizes.setFocusable(false);
		sizes.setMaximumSize(new Dimension(40,20));
		sizes.setMinimumSize(new Dimension(40,20));
		sizes.addActionListener(this);
		sizes.setToolTipText(SIZE);
		add(sizes);

		//创建粗体按钮
				Action action=new StyledAction("",new BoldAction(),editor);
				bold=new JToggleButton(action);
				bold.setIcon(createIcon("1.png"));
				bold.setFocusable(false);
				bold.setToolTipText(BOLD);
				add(bold);
				//创建斜体按钮
				action=new StyledAction("",new ItalicAction(),editor);
				italic=new JToggleButton(action);
				italic.setIcon(createIcon("back.png"));
				italic.setFocusable(false);
				italic.setToolTipText(ITALIC);
				add(italic);
				//创下划线按钮
				action=new StyledAction("",new UnderlineAction(),editor);
				underline=new JToggleButton(action);
				underline.setIcon(createIcon("1.png"));
				underline.setFocusable(false);
				underline.setToolTipText(UNDERLINE);
				add(underline);
				//创建颜色按钮
				colors=new JButton(createIcon("back.png"));
				colors.setFocusable(false);
				colors.addActionListener(this);
				colors.setToolTipText(COLOR);
				add(colors);
				//创建插入图片按钮	
				pictures=new JButton(createIcon("1.png"));
				pictures.setFocusable(false);
				pictures.addActionListener(this);
				pictures.setToolTipText(PICTURE);
				add(pictures);
				updateItems(editor.getInputAttributes());
				
	}
	//用于得到字体的类型
	protected String[] getAllFonts(){
		return GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
	}
	//用于得到字号的大小
	protected Integer[] getFontSizeArray(){
		int length=50;
		Integer[] array=new Integer[length];
		for(int i=0;i<length;i++){
			array[i]=6+i;
		}
		return array;
	}
	
	protected Icon createPictureIcon(File file)throws Exception{
		Icon icon=null;
		try{
			java.net.URL url=file.toURL();
			icon=new ImageIcon(url);
		}catch(OutOfMemoryError er){
			throw new Exception(er.getMessage());
		}
		if(icon.getIconWidth()<=0||icon.getIconHeight()<=0)
			throw new Exception("不能显示的文件，请选择正确的图片格式！");
		return icon;
	}
	
	protected static Icon createIcon(String path){
		java.net.URL url=EditToolBar.class.getResource(path);
		if(url!=null){
			return new ImageIcon(url);
		}else{
			System.err.println("couldn't find file:"+path);
			return null;
		}
	}

	/**
	 * Method actionPerformed
	 *
	 *
	 * @param e
	 *
	 */
	public void actionPerformed(ActionEvent e) {
		
		Object source=e.getSource();
		if(source==colors){
			Color initColor=StyleConstants.getForeground(editor.getInputAttributes());
			Color color=JColorChooser.showDialog(EditToolBar.this,
												"Chooser font color",
												initColor);
			if(color!=null){
				new StyledAction(null,new ForegroundAction("color",color),editor).actionPerformed(e);
			}	
		}else if(source==fonts){
			String fontName=(String)fonts.getSelectedItem();
			new StyledAction(null,new FontFamilyAction(fontName,fontName),editor).actionPerformed(e);
		}else if(source==sizes){
			Integer choicSize=(Integer)sizes.getSelectedItem();
			new StyledAction(null,new FontSizeAction(null,choicSize),editor).actionPerformed(e);
		}else if(source==pictures){
			int returnVal=fileChooser.showOpenDialog(getAncestor());
			if(returnVal==JFileChooser.APPROVE_OPTION){
				try{
					editor.insertIcon(createPictureIcon(fileChooser.getSelectedFile()));
					editor.insertComponent(new JLabel());
				}catch(Throwable ex){
					System.err.println(ex.getMessage());
					JOptionPane.showMessageDialog(getAncestor(),ex.getMessage());
				}
			}
		}
	}

	/**
	 * Method caretUpdate
	 *
	 *
	 * @param e
	 *
	 */
	public void caretUpdate(CaretEvent e) {
		
		AttributeSet atts=editor.getInputAttributes();
		int offset=editor.getCaretPosition();
//		System.out.println("offset"+offset);
		atts=editor.getStyledDocument().getCharacterElement(offset>0?offset-1:offset).getAttributes();
		updateItems(atts);
	}
	
	/**
	 *当光标移动时，依据当前光标文字属性更新工具栏中的项目属性
	 */
	protected void updateItems(AttributeSet atts){
		//暂时移除事件监听，是为了防止产生新的ActionEvent更改文字属性
		fonts.removeActionListener(this);
		sizes.removeActionListener(this);
		//依据AttributeSet设置项目属性
		bold.setSelected(StyleConstants.isBold(atts));
		italic.setSelected(StyleConstants.isItalic(atts));
		underline.setSelected(StyleConstants.isUnderline(atts));
		fonts.setSelectedItem(StyleConstants.getFontFamily(atts));
		sizes.setSelectedItem(StyleConstants.getFontSize(atts));
		//更新完毕，重新添加事件监听
		fonts.addActionListener(this);
		sizes.addActionListener(this);
	}
	
	/**
	 *获得最外层组件
	 */
	protected Container getAncestor(){
		if(ancestor==null){
			ancestor=getParent();
			while(ancestor.getParent()!=null)
				ancestor=ancestor.getParent();
		}
		return ancestor;
	}

	/**
	 * Method focusGained
	 * 当文本框获得焦点时，工具栏项目设为可用
	 *
	 * @param e
	 *
	 */
	public void focusGained(FocusEvent e) {
		
		bold.setEnabled(true);
		italic.setEnabled(true);
		underline.setEnabled(true);
		colors.setEnabled(true);
		fonts.setEnabled(true);
		sizes.setEnabled(true);
	}

	/**
	 * Method focusLost
	 * 当文本框失去焦点时，工具栏项目设为不可用
	 *
	 * @param e
	 *
	 */
	public void focusLost(FocusEvent e) {
		bold.setEnabled(false);
		italic.setEnabled(false);
		underline.setEnabled(false);
		colors.setEnabled(false);
		fonts.setEnabled(false);
		sizes.setEnabled(false);
	}
	
	/**
	 *内部类，继承自StyledTextAction抽象类
	 *作用于一个指定文字组件，设置文字属性。
	 *默认的StryledTextAction子类能够作用于当前程序所有的Text组件之中。
	 *实作本类的目的是限制其只作用于指定的组件。
	 *本类指定一个StyledTextAction子类acton作为参数，当本类的actionPerformed调用时
	 *首先检测ActionEvent所作用的文本组件，如果与指定的组件吻合，则执行
	 *字段action的actionPerformed，作出属性编辑。
	 *@param mn       the name of the action
	 *@param editor   the specified text component
	 *@param action   the action type
	 */
	class StyledAction extends StyledTextAction{
		protected StyledTextAction action;
		protected JEditorPane editor; 
		public StyledAction(String mn,StyledTextAction action,JEditorPane editor){
			super(mn);
			this.action=action;
			this.editor=editor;
		}
		
		public void actionPerformed(ActionEvent e){
			if(getEditor(e)==editor){
				action.actionPerformed(e);
			}
		}
	}
	
	/**
	 *内部类，文件过滤器
	 *这个文件过滤器是用于选择图片文件的文件对话框中的，在这里定义了
	 *"gif","jpg","png","jfif"四种可选格式。
	 *
	 */
	class PictureFilter extends javax.swing.filechooser.FileFilter{
		private java.util.List<String> list;
		private String description="图片文件"; 
	
		public PictureFilter() {
			list=new java.util.LinkedList<String>();
//			for(String ext:javax.imageio.ImageIO.getReaderFormatNames()){
//				addExt(ext);
//			}
			addExt("gif");
			addExt("jpg");
			addExt("png");
			addExt("jfif");
		}
		
		public void addExt(String ext){
			list.add(ext);
		}

		public boolean accept(File file) {
			if(file.isDirectory())
				return true;
			String fileName=file.getName();
			int index=fileName.lastIndexOf('.');
			if(index>0&&index<fileName.length()-1){
				String extension=fileName.substring(index+1);
				for(String ext:list){
					if(extension.equalsIgnoreCase(ext))
						return true;
				}
			}
			return false;	
		}
	
		public String getDescription(){
			StringBuffer buf=new StringBuffer(description);
			buf.append("(");
			for(String ext:list){
				buf.append("*."+ext+",");
			}
			buf.deleteCharAt(buf.length()-1);
			buf.append(")");
			return buf.toString();	
		}
	}
}
