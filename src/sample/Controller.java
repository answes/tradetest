package sample;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import sample.bean.Result;

import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class Controller implements Initializable {
    @FXML
    private Label name;
    @FXML
    private Button chooseFile;
    @FXML
    private Button start;
    @FXML
    private TextArea log;
    @FXML
    private TextField ip;
    @FXML
    private TextField market;
    @FXML
    private TextField price;
    @FXML
    private TextField number;
    @FXML
    private TextField forNum;
    @FXML
    private HBox zaraBox;
    @FXML
    private Button chooseZara;
    @FXML
    private ComboBox<String> comboBox;
    @FXML
    private ComboBox<String> type;

    FileChooser fileChooser;
    String url;
    String marketId;
    int tradetype = 1 ;
    int tradePrice;
    int tradeNumber;
    int loginS=0;
    int orderS=0;


    List<String> userId  = new ArrayList<>();
    List<String> cookies = new ArrayList<>();
    List zaraIds = new ArrayList<>();
    private File file;
    private File zaraFile;

    public Controller() {
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        comboBox.getItems().addAll(FXCollections.observableArrayList("用户id登陆","用户账号密码登陆"));
        comboBox.setValue("用户id登陆");
        type.getItems().addAll(FXCollections.observableArrayList("买入","卖出"));
        type.setValue("买入");
        zaraBox.setVisible(false);
        initFileChoose();
        initZaraFile();

        Platform.runLater(()->log.selectRange(6, 9));
    }

    private void initZaraFile() {
        chooseZara.setOnAction(event ->{
                    cookies.clear();
                    zaraFile = fileChooser.showOpenDialog(new Stage());
                    if(null != zaraFile)
                        rideZaraFile();
                }
        );
    }

    private void rideZaraFile() {
        zaraIds.clear();
        BufferedReader br = null;
        try
        {
            br = new BufferedReader(new FileReader(zaraFile));
        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        String line = "";
        try {
            while ((line = br.readLine()) != null)  //读取到的内容给line变量
            {
                zaraIds.add(line);
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
//        user.forEach((key,value)->log.appendText(key+"-----"+value+"\r\n"));
        zaraIds.forEach(c->log.appendText("---原单id = "+c+"---\r\n"));
    }

    private void initFileChoose() {
        fileChooser = new FileChooser();
        fileChooser.setTitle("选择文件");
        configureFileChooser(fileChooser);
        chooseFile.setOnAction(event -> {
            file = fileChooser.showOpenDialog(new Stage());
            name.setText(file.getAbsolutePath());
            if(null != file)
                rideFile();
        });
        start.setOnAction(event -> {
            if(null == file){
                Alert information = new Alert(Alert.AlertType.INFORMATION,"请先选择需要登陆的用户文件");
                information.setTitle("错误");         //设置标题，不设置默认标题为本地语言的information
                information.setHeaderText("Information");    //设置头标题，默认标题为本地语言的information
                information.show();
                return;
            }
            if(tradetype == 2 && null == zaraFile ){
                Alert information = new Alert(Alert.AlertType.INFORMATION,"请先选择对应的用户的原单id文件");
                information.setTitle("错误");         //设置标题，不设置默认标题为本地语言的information
                information.setHeaderText("Information");    //设置头标题，默认标题为本地语言的information
                information.show();
                return;
            }
            logins();
        });
        comboBox.getSelectionModel().selectedItemProperty().addListener((observable,oldValue,newValue)->
                System.out.println(newValue)
        );
        type.getSelectionModel().selectedItemProperty().addListener((observable,oldValue,newValue)-> {
            zaraBox.setVisible(false);
            if(newValue.equals("卖出")){
                tradetype = 2;
                zaraBox.setVisible(true);
            }
        });
    }

    /**
     * 批量登陆获取cookie
     */
    private void logins(){
        url =  ip.getText();
        marketId = market.getText();
        tradePrice = Integer.parseInt(price.getText());
        tradeNumber= Integer.parseInt(number.getText());
///     user.forEach((key,value)-> loginForPassword(rul,key,value))
        if(null == cookies || cookies.size()== 0){
            userId.forEach(c-> loginForId(c));
        }
        log.appendText("---共登陆= "+cookies.size()+"个-----成功="+loginS+"个，失败="+String.valueOf(cookies.size()-loginS)+"\r\n");

        for(int i=0;i<Integer.parseInt(forNum.getText());i++){
            for(int j=0;j<cookies.size();j++){
                trade(cookies.get(j),tradetype == 1?0:Integer.parseInt((String)zaraIds.get(j)));
            }
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//
//                }
//            }).start();

        }
        log.appendText("---共下单= "+cookies.size()+"个-----成功="+orderS+"个，失败="+String.valueOf(cookies.size()-orderS)+"\r\n");
        log.appendText("---------------项目执行结束---------------");
    }

    private void loginForId(String id) {
        try{
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost request = new HttpPost("http://"+url+"/platform/user/loginById.m?id="+id);

            HttpResponse response =httpClient.execute(request);
            if(response.getStatusLine().getStatusCode() == 200){
                loginS++;
            }
            InputStreamReader inputStreamReader = new InputStreamReader(response.getEntity().getContent());
            BufferedReader buffered = new BufferedReader(inputStreamReader);
            StringBuffer sb = new StringBuffer();
            String string = buffered.readLine();
            while (string != null) {
                sb.append(string);
                string = buffered.readLine();
            }
            System.out.println("登陆结果-------"+sb.toString());
            Gson gson = new Gson();
            Result cookie =  gson.fromJson(sb.toString(),Result.class);
            log.appendText("登陆结果-------"+ cookie.getResultDesc()+"\r\n");
            cookies.add("usessionId="+cookie.getResultDesc());
        }catch (Exception e){

        }
    }

    private void trade(String cookie,int zaraId) {
        Map<String, String> haeder = new HashMap<>();
        Map<String, String> qurey = new HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");

        haeder.put("Cookie", cookie);
        String  param = String.format("marketId="+marketId+"&price=%d&zaraId=%d&num=%d&type=%d&tpassword=123456",new Random().nextInt(tradePrice)+1,zaraId,new Random().nextInt(tradeNumber)+1,tradetype);
        try {
            System.out.println("下单 --- "+dateFormat.format(new Date()));
            HttpResponse response = HttpUtils.doPost("http://"+url+"/trade/order/serverCreateOrder.o?" + param, "", "", haeder, qurey, param);
            if(response.getStatusLine().getStatusCode() == 200){
                orderS++;
            }
            System.out.println("下单结果 ---- "+dateFormat.format(new Date()));
            InputStreamReader inputStreamReader = new InputStreamReader(response.getEntity().getContent());
            BufferedReader buffered = new BufferedReader(inputStreamReader);
            StringBuffer sb = new StringBuffer();
            String string = buffered.readLine();
            while (string != null) {
                sb.append(string);
                string = buffered.readLine();
            }
            log.appendText("---下单结果 = "+sb.toString()+"\r\n");
            System.out.println("下单结果-------"+sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//
//    private void loginForPassword(String url,String username,String password) {
//        Map<String,String> map = new HashMap<>();
//        map.put("mobile",username);
//        map.put("password",password);
//        try{
//        HttpResponse response = HttpUtils.doPost("http://"+url+"/platform/user/loginById.m",map);
//        InputStreamReader inputStreamReader = new InputStreamReader(response.getEntity().getContent());
//        BufferedReader buffered = new BufferedReader(inputStreamReader);
//        StringBuffer sb = new StringBuffer();
//        String string = buffered.readLine();
//        response.getAllHeaders();
//        while (string != null) {
//            sb.append(string);
//            string = buffered.readLine();
//        }
//        }catch (Exception e){
//
//        }
//    }


    /**
     * 获取到账号
     */
    private void rideFile() {
        BufferedReader br = null;
        try
        {
            br = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        String line = "";
        try {
            while ((line = br.readLine()) != null)  //读取到的内容给line变量
            {
//                String[] s = line.split(",");
//                user.put(s[0],s[1]);
                userId.add(line);
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
//        user.forEach((key,value)->log.appendText(key+"-----"+value+"\r\n"));
        userId.forEach(c->log.appendText("---用户id = "+c+"---\r\n"));
    }

    /**
     * 过滤文件
     * @param fileChooser
     */
    private static void configureFileChooser(FileChooser fileChooser) {
        fileChooser.setTitle("View Pictures");
        fileChooser.setInitialDirectory(
                new File(System.getProperty("user.home"))
        );
        fileChooser.getExtensionFilters().addAll(
//                new FileChooser.ExtensionFilter("All Images", "*.*"),
//                new FileChooser.ExtensionFilter("JPG", "*.jpg"),
                new FileChooser.ExtensionFilter("CSV", "*.csv")
        );
    }

}
