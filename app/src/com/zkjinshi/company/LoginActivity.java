package com.zkjinshi.company;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.beetle.bauhinia.PeerMessageActivity;
import com.beetle.bauhinia.api.IMHttpAPI;
import com.beetle.bauhinia.api.body.PostDeviceToken;
import com.beetle.im.IMService;
import com.bigkoo.svprogresshud.SVProgressHUD;
import com.zkjinshi.company.utils.AuthUtil;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import java.io.InputStream;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * LoginActivity
 * Description: 登录页面,给用户指定消息发送方Id
 */
public class LoginActivity extends BaseActivity implements View.OnClickListener {

    private EditText mEtAccount;
    private EditText mEtTargetAccount;
    private Button btnLogin,btnGroupLogin;
    private SVProgressHUD progress;

    AsyncTask mLoginTask;
    @Override
    protected void onBaseCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_login);
        initData();
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        btnLogin = (Button) findViewById(R.id.btn_login);
        btnGroupLogin = (Button)findViewById(R.id.btn_group_login);
        mEtAccount = (EditText) findViewById(R.id.et_username);
        mEtTargetAccount = (EditText) findViewById(R.id.et_target_username);
        btnLogin.setOnClickListener(this);
        btnGroupLogin.setOnClickListener(this);
    }

    private void initData(){
        progress = new SVProgressHUD(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_login) {
            if (mEtAccount.getText().toString().length() <= 0) {
                Toast.makeText(this, "请设置您的用户id", Toast.LENGTH_SHORT).show();
                return;
            }
            final long senderId = Long.parseLong(mEtAccount.getText().toString());
            if (senderId <= 0) {
                Toast.makeText(this, "用户id不能为0或者-1", Toast.LENGTH_SHORT).show();
                return;
            }


            long receiver = 0;
            if (mEtTargetAccount.getText().toString().length() > 0) {
                receiver = Long.parseLong(mEtTargetAccount.getText().toString());
                if (receiver <= 0) {
                    receiver = 0;
                }
            }

            final long receiverId = receiver;

            if (mLoginTask != null) {
                return;
            }

            mLoginTask = new AsyncTask<Void, Integer, String>() {

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    progress.showWithStatus("加载中...");
                }

                @Override
                protected String doInBackground(Void... urls) {
                    return AuthUtil.doAuthGrant(senderId);
                }
                @Override
                protected void onPostExecute(String result) {
                    progress.dismiss();
                    mLoginTask = null;
                    if (result != null && result.length() > 0) {
                        //设置用户id,进入MainActivity
                        go2Chat(senderId, receiverId, result);
                    } else {
                        Toast.makeText(LoginActivity.this, "登陆失败", Toast.LENGTH_SHORT).show();
                    }
                }
            }.execute();

        } if (v.getId() == R.id.btn_group_login) {//群聊
            if (mEtAccount.getText().toString().length() <= 0) {
                Toast.makeText(this, "请设置您的用户id", Toast.LENGTH_SHORT).show();
                return;
            }
            final long senderId = Long.parseLong(mEtAccount.getText().toString());
            if (senderId <= 0) {
                Toast.makeText(this, "用户id不能为0或者-1", Toast.LENGTH_SHORT).show();
                return;
            }
            if (mLoginTask != null) {
                return;
            }

            mLoginTask = new AsyncTask<Void, Integer, String>() {

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    progress.showWithStatus("加载中...");
                }

                @Override
                protected String doInBackground(Void... urls) {
                    return AuthUtil.doAuthGrant(senderId);
                }
                @Override
                protected void onPostExecute(String result) {
                    progress.dismiss();
                    mLoginTask = null;
                    if (result != null && result.length() > 0) {
                        //设置用户id,进入MainActivity
                        go2GroupChat(senderId,211,result);
                    } else {
                        Toast.makeText(LoginActivity.this, "登陆失败", Toast.LENGTH_SHORT).show();
                    }
                }
            }.execute();
        }
    }

    private String login(long uid) {
        //调用app自身的登陆接口获取im服务必须的access token,之后可将token保存在本地供下次直接登录IM服务
        String URL = "http://demo.gobelieve.io";
        String uri = String.format("%s/auth/token", URL);
        try {
            HttpClient getClient = new DefaultHttpClient();
            HttpPost request = new HttpPost(uri);
            JSONObject json = new JSONObject();
            json.put("uid", uid);
            int PLATFORM_ANDROID = 2;
            String androidID = Settings.Secure.getString(this.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            json.put("platform_id", PLATFORM_ANDROID);
            json.put("device_id", androidID);
            StringEntity s = new StringEntity(json.toString());
            s.setContentEncoding((Header) new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
            request.setEntity(s);

            HttpResponse response = getClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK){
                System.out.println("login failure code is:"+statusCode);
                return null;
            }
            int len = (int)response.getEntity().getContentLength();
            byte[] buf = new byte[len];
            InputStream inStream = response.getEntity().getContent();
            int pos = 0;
            while (pos < len) {
                int n = inStream.read(buf, pos, len - pos);
                if (n == -1) {
                    break;
                }
                pos += n;
            }
            inStream.close();
            if (pos != len) {
                return null;
            }
            String txt = new String(buf, "UTF-8");
            JSONObject jsonObject = new JSONObject(txt);
            String accessToken = jsonObject.getString("token");
            return accessToken;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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
            'A',
            'B',
            'C',
            'D',
            'E',
            'F'
    };

    public final static String bin2Hex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            sb.append(HEX_DIGITS[(b[i] & 0xf0) >>> 4]);
            sb.append(HEX_DIGITS[b[i] & 0x0f]);
        }
        return sb.toString();
    }

    /**
     * 开始私聊或会话列表
     * @param sender 发送者用户ID
     * @param receiver 接受者用户ID
     * @param token
     */
    private void go2Chat(long sender, long receiver, String token) {
        IMService.getInstance().setToken(token);

        IMHttpAPI.setToken(token);
        IMService.getInstance().setToken(token);
        IMService.getInstance().setUID(sender);
        IMService.getInstance().start();

        IMDemoApplication app = (IMDemoApplication)getApplication();
        String deviceToken = app.getDeviceToken();
        if (token != null && deviceToken != null && deviceToken.length() > 0) {
            PostDeviceToken tokenBody = new PostDeviceToken();
            tokenBody.xgDeviceToken = deviceToken;
            IMHttpAPI.Singleton().bindDeviceToken(tokenBody)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Object>() {
                        @Override
                        public void call(Object obj) {
                            Log.i("im", "bind success");
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            Log.i("im", "bind fail");
                        }
                    });
        }

        if (receiver == 0) {
            Intent intent = new Intent(this, MessageListActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("current_uid", sender);
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, PeerMessageActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("peer_uid", receiver);
            intent.putExtra("peer_name", "企业聊");
            intent.putExtra("current_uid", sender);
            startActivity(intent);
        }
    }

    /**
     * 开始群聊
     * @param sender 用户ID
     * @param receiver 群ID
     * @param token
     */
    private void go2GroupChat(long sender, long receiver, String token) {
        IMService.getInstance().setToken(token);
        IMHttpAPI.setToken(token);
        IMService.getInstance().setToken(token);
        IMService.getInstance().setUID(sender);
        IMService.getInstance().start();
        Intent intent = new Intent(this, AppGroupMessageActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("group_id", receiver);
        intent.putExtra("group_name", "企业群");
        intent.putExtra("current_uid", sender);
        startActivity(intent);
    }

}
