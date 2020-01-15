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
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Hello world!
 */
public class API_dataSelect {
    private static String sqlFilename = "Test.sql";
    private static String sqlFilepath = "C:\\";
    private static Properties apiService = new Properties();


    public static void main(String[] args) throws IOException, InterruptedException {
        //Properties apiService = new Properties();
        apiService.load(new FileInputStream("src/resources/ApiService.properties"));

        String serviceId = apiService.getProperty("serviceId");
        String pageIndex = apiService.getProperty("pageIndex");

        String result = DoPost(serviceId, pageIndex);
        System.out.println(result);
        //String status = result.substring(result.indexOf("\"code"),result.indexOf("\"data")-1);
        //String pages = result.substring(result.indexOf("\"pages")+8,result.indexOf("\"count")-1);
        //String count = result.substring(result.indexOf("\"count")+8,result.indexOf("\"data\":[{")-1);
        String data = result.substring(result.indexOf("[{"), result.indexOf("]}") + 1);
        System.out.println(data);

        int startIndex = 1;
        int endIndex = 2;//total = 196914
        //sqlFilename = "Insert" + args[0] + "-" + args[1] + ".sql";
        //sqlFilepath = System.getProperty("user.dir") + "\\";//获取class当前路径
        //startIndex = Integer.parseInt(args[0]);
        //endIndex = Integer.parseInt(args[1]);
        for (int i = startIndex; i <= endIndex; i++) {
            System.out.println(new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
            result = DoPost(serviceId, String.valueOf(i));
            data = result.substring(result.indexOf("\"data\":[{"), result.indexOf("]}") + 1);

            System.out.println("第" + i + "页数据开始插入，总共196914页数据");
            DataInsert(data);

        }
        System.out.println("java程序结束");

    }

    ///datansert文本处理，sql文件执行
    private static void DataInsert(String data) throws IOException {

        data = data.replace("\"data\":[{", "(");//替换所有
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
        String tableName = apiService.getProperty("dbName") + "." + apiService.getProperty("tableName");
        String sqlStr = "INSERT INTO " + tableName + "(" + sqlColumnNamestr + ") VALUES" + data + ";";
        System.out.println(sqlStr);
        //File file =new File(sqlFilepath + sqlFilename);
        //Writer out =new FileWriter(file);
        //out.write(data);
        //out.close();

        //File sqlfile =new File(sqlFilepath + sqlFilename);
        //ExecSqlFile(sqlfile);

    }


    ///Post参数配置
    public static String DoPost(String serviceId, String pageIndex) {
        String result = null;
        try {
            Map<String, String> map = new HashMap<String, String>();
            String url = apiService.getProperty("url");
//          String serviceId = "ff8080816e4f7cf3016e4fe199ea122a";
            String appId = apiService.getProperty("appId");
            String ak = apiService.getProperty("ak");
            String sk = apiService.getProperty("sk");
            String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
//          String pageIndex = "1";
            String pageSize = "500";

            map.put("serviceId", serviceId);
            map.put("ak", ak);
            map.put("appId", appId);
            map.put("timestamp", timestamp);
            map.put("pageIndex", pageIndex);
            map.put("pageSize", pageSize);
//          所有参数需要在签名之前存在map中
            String sign = SignGeneration.generationSign(map, sk);
            System.out.println(sign);
            System.out.println(timestamp);
            map.put("sign", sign);
            result = sendHttpPost(url, map);
//            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;

    }

    /**
     * @return json格式字符串
     * @throws IOException
     * @功能描述：装配post请求参数，向接口发送post请求
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

    /**
     * @return
     * @throws
     * @功能描述：连接数据库，从特定路径执行sql文件
     */
    public static void ExecSqlFile(File sqlfile) {
        try {
            String dbHost = apiService.getProperty("dbHost");               // 数据库地址
            String dbName = apiService.getProperty("dbName");               // 数据库名称
            String userName = apiService.getProperty("userName");           // 登录名称
            String userPassword = apiService.getProperty("userPassword");   // 登录密码
            String dbPort = apiService.getProperty("dbPort");               // 数据库端口号
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName +
                    "?useUnicode=true&characterEncoding=utf-8&port=" + dbPort + "&autoReconnect=true";
            Connection conn = DriverManager.getConnection(url, userName, userPassword);

            ScriptRunner runner = new ScriptRunner(conn);
            Resources.setCharset(Charset.forName("UTF-8")); //设置字符集,不然中文乱码插入错误
            runner.setLogWriter(null);//设置是否输出日志
            runner.runScript(new FileReader(sqlfile));// 绝对路径读取
            runner.closeConnection();
            conn.close();
            System.out.println("sql脚本执行完毕");

        } catch (Exception e) {
            System.out.println("sql脚本执行发生异常");
            e.printStackTrace();
        }
    }


}
