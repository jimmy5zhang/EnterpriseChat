package com.zkjinshi.company.utils;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.zkjinshi.company.response.AuthResponse;
import com.zkjinshi.company.vo.AuthVo;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 授权工具类
 */
public class AuthUtil {

    private static final int TIMEOUT_IN_MILLIONS = 5000;
    private static final String API = "http://api.gobelieve.io";
    private static final long appID = 1857;
    private static final String appSecret = "E0PswOToWv2eFvRxz8LJJJTw7DCIOdPh";

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
     * @return 所代表远程资源的响应结果
     * @throws Exception
     */
    public static String doAuthGrant(long uid) {
        String url = String.format("%s/auth/grant", API);
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        String token = null;
        AuthResponse authResponse = null;
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
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("uid",uid);
            out = new PrintWriter(conn.getOutputStream());
            // 发送请求参数
            out.print(jsonObject.toString());
            // flush输出流的缓冲
            out.flush();
            in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null)
            {
                result += line;
            }
            if(!TextUtils.isEmpty(result)){
                authResponse = new Gson().fromJson(result,AuthResponse.class);
            }
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
        if(null != authResponse){
            AuthVo authVo = authResponse.getData();
            if(null != authVo){
                token = authVo.getToken();
                if(!TextUtils.isEmpty(token)){
                    return token;
                }
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

}
