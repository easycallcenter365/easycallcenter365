package com.telerobot.fs.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/***
 * 根据数据库表名称自动生成实体类
 * （自动从数据表字段中提取注释作为类字段的注释）
 ***/
public class EntityGenerator {
	
	private String packageName = "";
	private String authorName = "easycallcenter365";//作者名字
	private String tablename = "";//表名
	private String[] colnames; // 列名数组
	private String[] colTypes; //列名类型数组
	private int[] colSizes; //列名大小数组
	private boolean f_util = false; // 是否需要导入包java.util.*
	private boolean f_sql = false; // 是否需要导入包java.sql.*
    
    //数据库连接
	private  String URL ="";
	private  String NAME = "root";
	private  String PASS = "123456";
	private  String DbHost = "";
	private static final String DRIVER ="com.mysql.jdbc.Driver";

	Map<String,String> fieldInfoMap = new HashMap<String,String>();
	Map<String,String> fieldDefaultValueMap = new HashMap<String,String>();
    /** 
     * 获得某表中所有字段的注释 
     * @param tableName 
     * @return 
     * @throws Exception 
     */  
    public void getColumnCommentByTableName(String table) throws Exception {  
        Statement stmt = con.createStatement();  
            ResultSet rs = stmt.executeQuery("show full columns from " + table);  
            System.out.println("【"+table+"】");  
            while (rs.next()) {  
            	fieldInfoMap.put(rs.getString("Field"),  rs.getString("Comment"));  
            	fieldDefaultValueMap.put(rs.getString("Field"),  rs.getString("Default"));  
            }   
            rs.close();  
        stmt.close();  
    }  
  
    private  Connection con;

    public EntityGenerator(String user, String pass, String DbHost, String table, String savePath, String packagename) throws Exception{
		this.URL = "jdbc:mysql://"+ DbHost +":3306/ipcc?useUnicode=true&characterEncoding=UTF-8&zeroDateTimeBehavior=convertToNull&useSSL=false";
    	this.NAME = user;
    	this.PASS = pass;
    	this.tablename = table;
    	this.packageName = packagename;
		//查要生成实体类的表
    	String sql = "select * from " + tablename;
    	PreparedStatement pStemt = null;
    	try {
    		try {
				Class.forName(DRIVER);
			} catch (ClassNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
    		con = DriverManager.getConnection(URL,NAME,PASS);
    		getColumnCommentByTableName(tablename);
			pStemt = con.prepareStatement(sql);
			ResultSetMetaData rsmd = pStemt.getMetaData();
			int size = rsmd.getColumnCount();	//统计列
			colnames = new String[size];
			colTypes = new String[size];
			colSizes = new int[size];
			for (int i = 0; i < size; i++) {
				colnames[i] = rsmd.getColumnName(i + 1);
				colTypes[i] = rsmd.getColumnTypeName(i + 1);
				
				if(colTypes[i].equalsIgnoreCase("datetime")){
					f_util = true;
				}
				if(colTypes[i].equalsIgnoreCase("image") || colTypes[i].equalsIgnoreCase("text")){
					f_sql = true;
				}
				colSizes[i] = rsmd.getColumnDisplaySize(i + 1);
			}
			
			String content = parse(colnames,colTypes,colSizes);
			String outputPath = savePath + "\\"+ initcap(this.tablename) +"Entity.java";
			String contentService =  parseService(colnames,colTypes,colSizes);
			String outputPathService = savePath + "\\"+ initcap(this.tablename) +"Service.java";
			if(FileUtils.WriteFile(outputPath, content)) 
				System.out.println(outputPath + "创建成功");
			if(FileUtils.WriteFile(outputPathService, contentService)) 
				System.out.println(outputPathService + "创建成功");
		} catch (SQLException e) {
			e.printStackTrace();
		} finally{
			try {
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
    }
    
    
    private String parseService(String[] colnames, String[] colTypes, int[] colSizes) {
		StringBuffer sb = new StringBuffer();
		sb.append("package com.easycallcenter365.fs.service;\r\n");
		sb.append("import javax.annotation.Resource;\r\n");
		sb.append("import org.springframework.jdbc.core.JdbcTemplate;\r\n");
		sb.append("import org.springframework.stereotype.Service;\r\n");
		sb.append("import com.easycallcenter365.fs.config.utils.DateUtils;\r\n");
		sb.append("import com.easycallcenter365.fs.model.AcdEntity;\r\n");
		//注释部分
		sb.append("   /***\r\n");
		sb.append("    * "+tablename+"Service 增删改查\r\n");
		sb.append("    * "+new Date()+" "+this.authorName+"\r\n");
		sb.append("    ***/ \r\n");
		//实体部分
		sb.append("@Service \r\n");
		sb.append("public class " + initcap(tablename) + "Service {\r\n \r\n");
		sb.append("@Resource \r\n");
		sb.append("private JdbcTemplate jdbcTemplate; \r\n \r\n");
		processInsertMethod(sb);//属性
		processUpdateMethod(sb);//get set方法
		sb.append("}\r\n");
		
		return sb.toString();
	}
    
    private void processInsertMethod(StringBuffer sb) {
		String insertFields = "";
		String valueFields = "";
		String realValues = "";
		Set<String> fields = fieldInfoMap.keySet();
		for(String field : fields) {
			if(field.equalsIgnoreCase("Id")) continue;
			insertFields += "`" + field + "`,";
			valueFields  += "?,";
			realValues += "\r\n\t\t entity.get"+ initcap(field) +"(),";
		}
		insertFields = insertFields.substring(0,insertFields.length() - 1 );
		valueFields = valueFields.substring(0, valueFields.length() - 1 );
		realValues = realValues.substring(0, realValues.length() - 1);
		sb.append("\t public boolean save"+ initcap(tablename) +"("+ initcap(tablename) +"Entity entity) { \r\n");
		sb.append("\t\t String execSql = \"INSERT INTO `"+ tablename +"` ("+ insertFields +") VALUES ("+ valueFields +")\" ; \r\n");
		sb.append("\t\t int affectRow = jdbcTemplate.update(execSql, \r\n");
        sb.append("\t\t new Object[] { ");
        sb.append(realValues + "\r\n");
        sb.append("\t\t }); \r\n");
        sb.append("\t\t return affectRow == 1; \r\n");
        sb.append("\t }\r\n");
    }


	private void  processUpdateMethod(StringBuffer sb) {
		String insertFields = "";
		String realValues = "";
		Set<String> fields = fieldInfoMap.keySet();
		for(String field : fields) {
			if(field.equalsIgnoreCase("Id")) continue;
			insertFields += "`" + field + "` = ?,";
			realValues += "\r\n\t\t entity.get"+ initcap(field) +"(),";
		}
		insertFields = insertFields.substring(0,insertFields.length() - 1 );
 		sb.append("\t public boolean update"+ initcap(tablename) +"("+ initcap(tablename) +"Entity entity) { \r\n");
		sb.append("\t\t String execSql = \"UPDATE  `"+ tablename +"` Set  "+ insertFields +" Where Id=?  \" ; \r\n");
		sb.append("\t\t int affectRow = jdbcTemplate.update(execSql, \r\n");
        sb.append("\t\t new Object[] { ");
        sb.append(realValues + "\r\n");
        sb.append("\t\t entity.getId() \r\n");
        sb.append("\t\t }); \r\n");
        sb.append("\t\t return affectRow == 1; \r\n");
        sb.append("\t }\r\n");

	}

	/**
	 * 功能：生成实体类主体代码
	 * @param colnames
	 * @param colTypes
	 * @param colSizes
	 * @return
	 */
	private String parse(String[] colnames, String[] colTypes, int[] colSizes) {
		StringBuffer sb = new StringBuffer();
		
		sb.append("package " + this.packageName + ";\r\n\r\n");
		sb.append("import com.easycallcenter365.fs.config.utils.DateUtils;\r\n\r\n");
 
		//判断是否导入工具包
		if(f_util){
			sb.append("import java.util.Date;\r\n");
		}
		if(f_sql){
			sb.append("import java.sql.*;\r\n");
		}
		
		sb.append("\r\n");
		//注释部分
		sb.append("   /***\r\n");
		sb.append("    * "+tablename+" 实体类\r\n");
		sb.append("    * "+new Date()+" "+this.authorName+"\r\n");
		sb.append("    ***/ \r\n");
		//实体部分
		sb.append("public class " + initcap(tablename) + "Entity {\r\n\r\n");
		sb.append("\r\n\tpublic  " + initcap(tablename) + "Entity()  {}\r\n\r\n");
		processAllAttrs(sb);//属性
		processAllMethod(sb);//get set方法
		sb.append("}\r\n");
		
		return sb.toString();
	}
	 
	/**
	 * 功能：生成所有属性
	 * @param sb
	 */
	private void processAllAttrs(StringBuffer sb) {
		
		for (int i = 0; i < colnames.length; i++) {
			sb.append("\tprivate " + sqlType2JavaType(colTypes[i]) + " " + colnames[i] + " = "+ sqlTypeDefaultValue(colTypes[i], fieldDefaultValueMap.get(colnames[i])) +";\r\n");
		}
		
	}

	/**
	 * 功能：生成所有方法
	 * @param sb
	 */
	private void processAllMethod(StringBuffer sb) {
		for (int i = 0; i < colnames.length; i++) {
			if(!StringUtils.isNullOrEmpty(fieldInfoMap.get(colnames[i])))
			sb.append("\t/**  "+ fieldInfoMap.get(colnames[i]) +"  **/\r\n");
			sb.append("\tpublic void set" + initcap(colnames[i]) + "(" + sqlType2JavaType(colTypes[i]) + " " + 
					colnames[i] + "){\r\n");
			sb.append("\t\tthis." + colnames[i] + "=" + colnames[i] + ";\r\n");
			sb.append("\t}\r\n\r\n");
			if(!StringUtils.isNullOrEmpty(fieldInfoMap.get(colnames[i])))
			sb.append("\t/**  "+ fieldInfoMap.get(colnames[i]) +"  **/\r\n");
			sb.append("\tpublic " + sqlType2JavaType(colTypes[i]) + " get" + initcap(colnames[i]) + "(){\r\n");
			sb.append("\t\treturn " + colnames[i] + ";\r\n");
			sb.append("\t}\r\n\r\n");
		}
		
	}
	
	/**
	 * 功能：将输入字符串的首字母改成大写
	 * @param str
	 * @return
	 */
	private String initcap(String str) {
		
		char[] ch = str.toCharArray();
		if(ch[0] >= 'a' && ch[0] <= 'z'){
			ch[0] = (char)(ch[0] - 32);
		}
		
		return new String(ch);
	}
	
	  private Object sqlTypeDefaultValue(String sqlType, Object value) {
		
		if(sqlType.equalsIgnoreCase("bit")){
			return value;
		}else if(sqlType.equalsIgnoreCase("tinyint")){
			return value;
		}else if(sqlType.equalsIgnoreCase("smallint")){
			return value;
		}else if(sqlType.equalsIgnoreCase("int")){
			return value==null ? "0" : value;
		}else if(sqlType.equalsIgnoreCase("bigint")){
			return value;
		}else if(sqlType.equalsIgnoreCase("float")){
			return value;
		}else if(sqlType.equalsIgnoreCase("decimal") || sqlType.equalsIgnoreCase("numeric") 
				|| sqlType.equalsIgnoreCase("real") || sqlType.equalsIgnoreCase("money") 
				|| sqlType.equalsIgnoreCase("smallmoney")){
			return value;
		}else if(sqlType.equalsIgnoreCase("varchar") || sqlType.equalsIgnoreCase("char") 
				|| sqlType.equalsIgnoreCase("nvarchar") || sqlType.equalsIgnoreCase("nchar") 
				|| sqlType.equalsIgnoreCase("text")){
			 
			return "\""+ value +"\"";
		}else if(sqlType.equalsIgnoreCase("datetime")){
			return "DateUtils.parseDateTime(\""+ value +"\")";
		}else if(sqlType.equalsIgnoreCase("image")){
			return value;
		}
		
		return null;
	}

	/**
	 * 功能：获得列的数据类型
	 * @param sqlType
	 * @return
	 */
	private String sqlType2JavaType(String sqlType) {
		
		if(sqlType.equalsIgnoreCase("bit")){
			return "boolean";
		}else if(sqlType.equalsIgnoreCase("tinyint")){
			return "byte";
		}else if(sqlType.equalsIgnoreCase("smallint")){
			return "short";
		}else if(sqlType.equalsIgnoreCase("int")){
			return "int";
		}else if(sqlType.equalsIgnoreCase("bigint")){
			return "long";
		}else if(sqlType.equalsIgnoreCase("float")){
			return "float";
		}else if(sqlType.equalsIgnoreCase("decimal") || sqlType.equalsIgnoreCase("numeric") 
				|| sqlType.equalsIgnoreCase("real") || sqlType.equalsIgnoreCase("money") 
				|| sqlType.equalsIgnoreCase("smallmoney")){
			return "double";
		}else if(sqlType.equalsIgnoreCase("varchar") || sqlType.equalsIgnoreCase("char") 
				|| sqlType.equalsIgnoreCase("nvarchar") || sqlType.equalsIgnoreCase("nchar") 
				|| sqlType.equalsIgnoreCase("text")){
			return "String";
		}else if(sqlType.equalsIgnoreCase("datetime")){
			return "Date";
		}else if(sqlType.equalsIgnoreCase("image")){
			return "Blod";
		}
		
		return null;
	}
	
	/**
	 * main
	 * TODO
	 * @param args
	 * @throws Exception 
	 */
//	public static void main(String[] args) throws Exception {
//		
//		//new EntityGenerator("root", "123456","172.29.50.7","Acd", "C:\\", "com.easycallcenter365.fs.model");
//		new EntityGenerator("root", "123456","172.29.50.7","Acd", "C:\\", "com.easycallcenter365.fs.model");
//	}

}