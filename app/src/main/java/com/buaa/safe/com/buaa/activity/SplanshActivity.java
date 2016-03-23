package com.buaa.safe.com.buaa.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;
import android.widget.Toast;

import com.buaa.safe.R;
import com.buaa.safe.com.buaa.utils.StreamUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class SplanshActivity extends Activity {
    private static final int CODE_UPDATE_DIALOG = 1;    //弹出对话框
    private static final int CODE_ENTER_HOME = 2;       //进入主界面
    private static final int CODE_URL_ERROR = 3;    //弹出对话框
    private static final int CODE_NETWORK_ERROR = 4;
    private static final int CODE_JSON_ERROR = 5;

    String mVersionName;
    int mVersionCode;
    String desc;
    String url;

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
           switch (msg.what){
               case CODE_UPDATE_DIALOG:
                   showUpdateDialog();
                   break;
               case CODE_ENTER_HOME:
                   enterHome();
                   break;
               case CODE_NETWORK_ERROR:
                   Toast.makeText(getApplicationContext(),"网络不给力",Toast.LENGTH_LONG).show();
                   enterHome();
                   break;
               case CODE_URL_ERROR:
                   Toast.makeText(getApplicationContext(),"网路地址错误",Toast.LENGTH_LONG).show();
                   enterHome();
                   break;
               case CODE_JSON_ERROR:
                   Toast.makeText(getApplicationContext(),"数据解析错误",Toast.LENGTH_LONG).show();
                   enterHome();
                   break;

           }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splansh);

        TextView tv = (TextView)findViewById(R.id.tv_version);
        tv.setText("版本号：" + getVersionName());

        checkNewVersion();
    }

    /*
    网络部分写在子线程
     */
    public void checkNewVersion(){
        new Thread() {
            @Override
            public void run() {
                Message msg = Message.obtain();
                long startTime = System.currentTimeMillis();
                try {
                    // 10.0.2.2是预留ip,供模拟器访问PC的服务器
                    HttpURLConnection conn = (HttpURLConnection) new URL(
                            "http://10.0.2.2:8080/safe/version.json")
                            .openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(2000);// 连接超时
                    conn.setReadTimeout(2000);// 读取超时,连接上了,服务器不给响应

                    conn.connect();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        InputStream in = conn.getInputStream();
                        String result = null;
                        try {
                            result = StreamUtils.stream2String(in);
                        } catch (Exception e) {
                            System.out.println("字节转字符出错");
                            e.printStackTrace();
                        }
                        // System.out.println("result:" + result);
                        // 解析json
                        JSONObject jo = new JSONObject(result);
                        mVersionName = jo.getString("versionName");
                        mVersionCode = jo.getInt("versionCode");
                        desc = jo.getString("des");
                        url = jo.getString("url");

                        if (getVersionCode() < mVersionCode) {
                            System.out.println("有更新");
                            // showUpdateDialog();
                            msg.what = CODE_UPDATE_DIALOG;
                        } else {
                            System.out.println("无更新");
                            msg.what = CODE_ENTER_HOME;
                        }
                    }

                } catch (MalformedURLException e) {
                    // url错误
                    e.printStackTrace();
                    msg.what = CODE_URL_ERROR;
                } catch (IOException e) {
                    // 网络异常
                    e.printStackTrace();
                    msg.what = CODE_NETWORK_ERROR;
                } catch (JSONException e) {
                    // json解析异常
                    e.printStackTrace();
                    msg.what = CODE_JSON_ERROR;
                } finally {
                    long endTime = System.currentTimeMillis();
                    long timeUsed = endTime - startTime;// 访问网络使用的时间

                    try {
                        if (timeUsed < 2000) {
                            Thread.sleep(2000 - timeUsed);// 强制等待一段时间, 凑够两秒钟
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    handler.sendMessage(msg);
                }
            }
        }.start();
    }

    private String getVersionName(){
        PackageManager pm = getPackageManager();

        try {
            PackageInfo packageInfo = pm.getPackageInfo(getPackageName(), 0);
            String versionName = packageInfo.versionName;
            return versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return "";
    }

    private int getVersionCode(){
        PackageManager pm = getPackageManager();
        try {
            PackageInfo packageInfo = pm.getPackageInfo(getPackageName(),0);
            int versionCode = packageInfo.versionCode;
            return versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private void showUpdateDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("发现新版本！");
        builder.setMessage(desc);
        builder.setPositiveButton("立即更新", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.setNegativeButton("下次再说", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                enterHome();
            }
        });

        builder.show();
    }

    private void enterHome(){
        Intent intent = new Intent();
        intent.setClass(this,HomeActivity.class);
        startActivity(intent);
    }
}
