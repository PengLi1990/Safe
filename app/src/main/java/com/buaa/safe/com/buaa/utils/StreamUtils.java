package com.buaa.safe.com.buaa.utils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Created by Administrator on 2016/3/22.
 */
public class StreamUtils {
    public static String stream2String(InputStream inputStream) throws Exception{
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int len = 0;
        byte[] buff = new byte[1024];
        while ((len = inputStream.read(buff)) != -1){
            out.write(buff,0,len);
        }

        inputStream.close();
        out.close();

        return out.toString();
    }
}
