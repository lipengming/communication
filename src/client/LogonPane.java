package client;

import javax.swing.*;
import java.awt.Container;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.EventListener;

/**
 *登录面板
 */
public class LogonPane extends JPanel implements ActionListener{
	
	private ClientModel client;
	private JTextField nameField;
	private JTextField ipField;
	private JTextField portField;
	private JTextField pswFiled;
	private JButton relatedButton=null;

	/**
	 * Method LogonPane
	 *
	 *
	 */
	public LogonPane() {
		// TODO: 在这添加你的代码
		super(new SpringLayout());
		ipField=addLabeledField(this,"服务器IP:",this);
		portField=addLabeledField(this,"服务器端口:",this);
		nameField=addLabeledField(this,"用户名:",this);
		pswFiled=addLabeledField(this, "密码:", this);
		
		ipField.setText("127.0.0.1");
		portField.setText("8001");
		task.SpringUtilities.makeCompactGrid(this,
                                        4, 2,			 //rows, cols
                                        10, 10,        //initX, initY
                                        6, 10);       //xPad, yPad
                   
    }                                    
		
	public String getIP(){
		return ipField.getText();
	}
	
	public int getPort() throws NumberFormatException{
		return Integer.parseInt(portField.getText());
	}
	
	public String getName(){
		return nameField.getText();
	}
	
	public String getpsw(){
		return pswFiled.getText();
		
	}
		
	public void setRelatedButton(JButton button){
		relatedButton=button;
	}

	protected static JTextField addLabeledField(Container c,String label,ActionListener als){
		JLabel l=new JLabel(label);
		c.add(l);
		JTextField field=new JTextField(15);
		field.addActionListener(als);
		l.setLabelFor(field);
		c.add(field);
		return field;
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
		if(source==ipField){
			portField.grabFocus();
			portField.selectAll();
		}else if(source==portField){
			nameField.grabFocus();
			nameField.selectAll();
		}else if(source==nameField){
			if(relatedButton!=null)
				relatedButton.doClick();
		}
	}
	
	
}
