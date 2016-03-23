package com.buaa.safe.com.buaa.activity;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

import com.buaa.safe.R;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class SplanshActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splansh);
        TextView tv = (TextView)findViewById(R.id.tv_version);
        tv.setText("版本号：" + getVersionName());
    }

    /*
    网络部分写在子线程
     */
    public void checkNewVersion(){
        new Thread(){
            @Override
            public void run() {
                try {
                    HttpURLConnection conn = (HttpURLConnection)new URL("").openConnection();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }

    public String getVersionName(){
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

    public int getVersionCode(){
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
}
