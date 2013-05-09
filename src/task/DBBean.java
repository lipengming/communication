package task;

import java.sql.*;

public class DBBean {
	 //初始化相关信息
	 //请修改连接数据库的相关信息。。。。
	 //..............................................
	
	
     String DBDrive =  "com.mysql.jdbc.Driver"; 
     String DBUrl = "jdbc:mysql://localhost:3306/user_info"; 
     String DBUser = "root";
     String DBPassword="195891";
     ResultSet rs = null;
     Connection conn = null;
     Statement stmt = null;
     //初始化连接
     public boolean init(String driveName,String sqlUrl,String sqlUser,String sqlPass){
    	 DBDrive = driveName;
    	 DBUrl = sqlUrl;
    	 DBUser = sqlUser;
    	 DBPassword = sqlPass;
    	 return init();
    	 
     }
     public boolean init(){
    	 try{
    	     Class.forName(DBDrive);
    		 return true;
    	 }catch(Exception e){
    		 System.out.println("初始化失败！");
    		 e.printStackTrace();
    		 return false;
    	 }
     }
     //执行查询语句
     public ResultSet executeQuery(String sql){
    	 rs = null;
    	 try {
			conn = DriverManager.getConnection(DBUrl, DBUser, DBPassword);
			stmt = conn.createStatement();
			rs=stmt.executeQuery(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			System.out.println("执行查询出错。。。。");
			e.printStackTrace();
		}
    	 return rs;
     }
     //执行插入语句
     public boolean executeinsert(String sql){
    	 try {
			conn = DriverManager.getConnection(DBUrl, DBUser, DBPassword);
			stmt = conn.createStatement();
			stmt.execute(sql);
			return true;
		} catch (SQLException e) {
			System.out.println("执行更新语句出错。。。。");
			e.printStackTrace();
			return false;
		}			
    	
     }
       //执行更新语句
     public boolean executeUpdate(String sql){
    	 try {
 			conn = DriverManager.getConnection(DBUrl, DBUser, DBPassword);
 			stmt = conn.createStatement();
 			stmt.executeUpdate(sql);
 			return true;
 		} catch (SQLException e) {
 			// TODO Auto-generated catch block
 			System.out.println("执行更新语句出错。。。。");
 			e.printStackTrace();
 			return false;
 		}
 		
     }
       //执行关闭操作。。。。
     public boolean colse(){
    	 try{
    		 if(rs!=null){
    			 rs.close();
    		 }
    		 if(stmt!=null){
    		     stmt.close();
    		 }
    		 if(conn!=null){
    			 conn.close();
    		 }
    		 return true;
    	 }catch(SQLException e){
    		 System.out.println("关闭失败。。。。。");
    		 e.printStackTrace();
    		 return false;
    	 }
     }
}
