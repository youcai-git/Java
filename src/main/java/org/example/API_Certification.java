package org.example;

import com.isoftstone.sign.SignGeneration;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Hello world!
 */
public class API_Certification {
    private static String sqlFilename;
    private static String sqlFilepath;

    public static void main(String[] args) throws IOException, InterruptedException {
        //电子证照信息获取服务
        String serviceId = "ff8080816c1d7ee4016c1e3564730a22";
        String appId = "FEC7EFF711B94B8283B2101FCA79F64F";
        //set column of table
        Map<String, String> columnArgs = new HashMap<String, String>();
        columnArgs.put("CertificateHolderCode", "422201197902027739");

        columnArgs.put("AccountId", "eb68fa5e287b48368c9ba5b503fa735f");
        columnArgs.put("UseFor", "测试");
        String result = DoPost(serviceId, appId, columnArgs);
        String Status = result.substring(result.indexOf("\"code"), result.indexOf("\"data") - 1);
        String total = result.substring(result.indexOf("\\\"total") + 10, result.indexOf("\\\"dataList\\\":[{") - 1);
        String dataList = result.substring(result.indexOf("\\\"dataList\\\":[{"), result.indexOf("]}") + 1);
        System.out.println(Status);
        System.out.println(total);
        System.out.println(dataList);
        DatanIsert(dataList);
        //int count_index = Integer.parseInt(pages);
        //sqlFilename = "Insert" + args[0] + "-" + args[1] + ".sql";
        //sqlFilepath = System.getProperty("user.dir") + "\\";//获取class当前路径


    }

    /**
     * @return
     * @throws Exception
     * @功能描述： 拼接sql语句写到文件中，执行sql文件
     */
    private static void DatanIsert(String data) throws IOException {

        data = data.replace("\\", "");
        data = data.replace("\"dataList\":[{", "(");//替换所有
        data = data.replace("},{", "),\r\n(");
        data = data.replace("}]", ")");

        //System.out.println(data.substring(1,data.indexOf("),")));
        List<String> columnNameList = new ArrayList<>();
        for (String columnNameStr : data.substring(1, data.indexOf("),")).split(",")) {
            int columnNameIndex = columnNameStr.indexOf(":");
            //int columnValueIndex = data.indexOf(",");
            columnNameList.add(columnNameStr.substring(0, columnNameIndex));
        }

        //data = data.replace( "\"JYFW\":" ,  "");
        String sqlColumnNamestr = "";
        for (String columnName : columnNameList) {
            data = data.replace(columnName + ":", "");
            sqlColumnNamestr = sqlColumnNamestr + columnName + ",";
        }
        sqlColumnNamestr = sqlColumnNamestr.replace("\"", "");
        sqlColumnNamestr = sqlColumnNamestr.replace(",)", ")");
        //System.out.println(data);
//
        for (String str : columnNameList) {
            data = data.replace(str + ":", "");
        }

        //sql语句拼接
        String tableName = "data_service.yjt_api_GSZTDJJCXX";
        String sqlStr = "INSERT INTO " + tableName + "(" + sqlColumnNamestr + ") VALUES" + data + ";";
        System.out.println(sqlStr);

        //File file =new File(sqlFilepath + sqlFilename);
        //Writer out =new FileWriter(file);
        //out.write(data);
        //out.close();

        //File sqlfile =new File(sqlFilepath + sqlFilename);
        //execsql(sqlfile);

    }


    /**
     * @return response
     * @throws Exception
     * @功能描述： 设置post接口必要参数，用post方法请求接口
     */
    public static String DoPost(String serviceId, String appId, Map<String, String> columnArgs) {
        String result = null;
        try {
            Map<String, String> map = new HashMap<String, String>();
            String url = "http://data.hb.cegn.cn/irsp/openApi/pushData/v1";
            String ak = "eb68fa5e287b48368c9ba5b503fa735f";
            String sk = "76987a64cedc4455";
            String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

            map.put("serviceId", serviceId);
            map.put("ak", ak);
            map.put("appId", appId);
            map.put("timestamp", timestamp);

            //获取字段参数值
            for (String key : columnArgs.keySet()) {
                map.put(key, columnArgs.get(key));
            }


//          所有参数需要在签名之前存在map中
            String sign = SignGeneration.generationSign(map, sk);
//          System.out.println(sign);
            map.put("sign", sign);
            result = sendHttpPost(url, map);
//            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;

    }

    /**
     * @return response
     * @throws Exception
     * @功能描述： Post方法请求接口
     */
    private static String sendHttpPost(String url, Map<String, String> map) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);
        //httpPost.addHeader("Content-Type", "application/json");
        //装配post请求参数
        List<NameValuePair> list = new ArrayList<NameValuePair>();
        for (String key : map.keySet()) {
            list.add(new BasicNameValuePair(key, String.valueOf(map.get(key))));
        }
        httpPost.setEntity(new UrlEncodedFormEntity(list, "utf-8"));
        CloseableHttpResponse response = httpClient.execute(httpPost);
//		System.out.println(response.getStatusLine().getStatusCode() + "\n");
        HttpEntity entity = response.getEntity();
        String responseContent = EntityUtils.toString(entity, "UTF-8");
//		System.out.println(responseContent);
        response.close();
        httpClient.close();
        return responseContent;
    }


    ///数据导入mysql库
    /**
     *
     */
    private static String dbHost = "10.0.178.6";               // 数据库地址
    private static String dbName = "data_service";             // 数据库名称
    private static String userName = "root";                 // 登录名称
    private static String userPassword = "1qaz@WSX";         // 登录密码
    private static String dbPort = "3306";                     // 数据库端口号

    public static void execsql(File sqlfile) {
        try {
            Connection conn = getMySqlConnection();
            ScriptRunner runner = new ScriptRunner(conn);
            Resources.setCharset(Charset.forName("UTF-8")); //设置字符集,不然中文乱码插入错误
            runner.setLogWriter(null);//设置是否输出日志
            // 绝对路径读取
            Reader read = new FileReader(sqlfile);
            // 从class目录下直接读取
//            Reader read = Resources.getResourceAsReader("test.sql");
            runner.runScript(read);
            runner.closeConnection();
            conn.close();
            System.out.println("sql脚本执行完毕");

        } catch (Exception e) {
            System.out.println("sql脚本执行发生异常");
            e.printStackTrace();
        }
    }

    /**
     * @return
     * @throws Exception
     * @功能描述： 获取数据库连接
     */
    public static Connection getMySqlConnection() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        String url = "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName + "?useUnicode=true&characterEncoding=utf-8&port=" + dbPort + "&autoReconnect=true";
        return DriverManager.getConnection(url, userName, userPassword);
    }


}
