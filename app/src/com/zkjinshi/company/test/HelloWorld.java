package com.zkjinshi.company.test;

import com.zkjinshi.company.utils.BASE64Encoder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HelloWorld {
    private static final int TIMEOUT_IN_MILLIONS = 5000;

    //private static final String API = "http://api.gobelieve.io";
    private static final String API = "http://dev.api.gobelieve.io";
    private static final long appID = 7;
    private static final String appSecret = "0WiCxAU1jh76SbgaaFC7qIaBPm2zkyM1";

    private static byte[] getMD5(byte[] data) {
        byte[] hash = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(data);
            hash = digest.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return hash;
    }

    private static String base64Encode(byte[] c) {

        final BASE64Encoder encoder = new BASE64Encoder();
        final String encodedText = encoder.encode(c);
        return encodedText;
    }

    public static String getBasicAuth(long appID, String appSecret) {
        byte[] md5 = getMD5(appSecret.getBytes());
        String strMD5 = bin2Hex(md5);

        String t = "" + appID + ":" + strMD5;
        System.out.println("auth:" + t); 
        // String t = "" + appID + ":";
        // byte[] id = t.getBytes();

        // byte[] c = new byte[id.length + md5.length];

        // System.arraycopy(id, 0, c, 0, id.length);
        // System.arraycopy(md5, 0, c, id.length, md5.length);

        final String encodedText = base64Encode(t.getBytes());
        return encodedText;
    }

/**
     * 向指定 URL 发送POST方法的请求
     *
     * @param url
     *            发送请求的 URL
     * @return 所代表远程资源的响应结果
     * @throws Exception
     */
    public static String doAuthGrant() {
        String url = String.format("%s/auth/grant", API);
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        try {
            URL realUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection)realUrl.openConnection();
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type",
                    "application/json");
            conn.setRequestProperty("charset", "utf-8");

            String basicAuth = getBasicAuth(appID, appSecret);
            conn.setRequestProperty("Authorization", "Basic " + basicAuth);

            conn.setUseCaches(false);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setReadTimeout(TIMEOUT_IN_MILLIONS);
            conn.setConnectTimeout(TIMEOUT_IN_MILLIONS);
            
            

            //todo json serialize
            String param = "{\"uid\":1}";

            // 获取URLConnection对象对应的输出流
            out = new PrintWriter(conn.getOutputStream());
            // 发送请求参数
            out.print(param);
            // flush输出流的缓冲
            out.flush();

            in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null)
            {
                result += line;
            }
            System.out.println("result:" + result);
        } catch (Exception e) {
            e.printStackTrace();
        } finally{
            try {
                if (out != null)
                {
                    out.close();
                }
                if (in != null)
                {
                    in.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return result;
    }


    private static final char HEX_DIGITS[] = {
            '0',
            '1',
            '2',
            '3',
            '4',
            '5',
            '6',
            '7',
            '8',
            '9',
            'a',
            'b',
            'c',
            'd',
            'e',
            'f'
    };

    public final static String bin2Hex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            sb.append(HEX_DIGITS[(b[i] & 0xf0) >>> 4]);
            sb.append(HEX_DIGITS[b[i] & 0x0f]);
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        doAuthGrant();
    }

}


