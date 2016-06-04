package com.zkjinshi.company;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.beetle.bauhinia.GroupMessageActivity;
import com.beetle.bauhinia.PeerMessageActivity;
import com.beetle.bauhinia.activity.BaseActivity;
import com.beetle.bauhinia.api.IMHttpAPI;
import com.beetle.bauhinia.db.Conversation;
import com.beetle.bauhinia.db.ConversationIterator;
import com.beetle.bauhinia.db.CustomerMessageDB;
import com.beetle.bauhinia.db.GroupMessageDB;
import com.beetle.bauhinia.db.ICustomerMessage;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.IMessage.GroupNotification;
import com.beetle.bauhinia.db.MessageIterator;
import com.beetle.bauhinia.db.PeerMessageDB;
import com.beetle.bauhinia.tools.Notification;
import com.beetle.bauhinia.tools.NotificationCenter;
import com.beetle.im.GroupMessageObserver;
import com.beetle.im.IMMessage;
import com.beetle.im.IMService;
import com.beetle.im.IMServiceObserver;
import com.beetle.im.LoginPoint;
import com.beetle.im.LoginPointObserver;
import com.beetle.im.PeerMessageObserver;
import com.beetle.im.SystemMessageObserver;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;


public class MessageListActivity extends BaseActivity implements IMServiceObserver, LoginPointObserver,
        PeerMessageObserver, GroupMessageObserver, SystemMessageObserver, AdapterView.OnItemClickListener,
         NotificationCenter.NotificationCenterObserver {

    private static final String TAG = "MessageListActivity";

    private List<Conversation> conversations;
    private ListView lv;
    protected long currentUID = 0;


    private static final long APPID = 7;
    private static final long KEFU_ID = 55;


    private BaseAdapter adapter;
    class ConversationAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return conversations.size();
        }
        @Override
        public Object getItem(int position) {
            return conversations.get(position);
        }
        @Override
        public long getItemId(int position) {
            return position;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ConversationView view = null;
            if (convertView == null) {
                view = new ConversationView(MessageListActivity.this);
            } else {
                view = (ConversationView)convertView;
            }
            Conversation c = conversations.get(position);
            view.setConversation(c);;
            return view;
        }
    }

    // 初始化组件
    private void initWidget() {
        Toolbar toolbar = (Toolbar)findViewById(R.id.support_toolbar);
        setSupportActionBar(toolbar);

        lv = (ListView) findViewById(R.id.list);
        adapter = new ConversationAdapter();
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "main activity create...");

        setContentView(R.layout.activity_conversation);

        Intent intent = getIntent();

        currentUID = intent.getLongExtra("current_uid", 0);
        if (currentUID == 0) {
            Log.e(TAG, "current uid is 0");
            return;
        }

        IMService im =  IMService.getInstance();
        im.addObserver(this);
        im.addLoginPointObserver(this);
        im.addPeerObserver(this);
        im.addGroupObserver(this);
        im.addSystemObserver(this);

        loadConversations();
        initWidget();

        NotificationCenter nc = NotificationCenter.defaultCenter();
        nc.addObserver(this, PeerMessageActivity.SEND_MESSAGE_NAME);
        nc.addObserver(this, PeerMessageActivity.CLEAR_MESSAGES);
        nc.addObserver(this, GroupMessageActivity.SEND_MESSAGE_NAME);
        nc.addObserver(this, GroupMessageActivity.CLEAR_MESSAGES);

        //createGroup();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        IMService im =  IMService.getInstance();
        im.removeObserver(this);
        im.removeLoginPointObserver(this);
        im.removePeerObserver(this);
        im.removeGroupObserver(this);
        im.removeSystemObserver(this);
        NotificationCenter nc = NotificationCenter.defaultCenter();
        nc.removeObserver(this);
        Log.i(TAG, "message list activity destroyed");
    }


    public  String messageContentToString(IMessage.MessageContent content) {
        if (content instanceof IMessage.Text) {
            return ((IMessage.Text) content).text;
        } else if (content instanceof IMessage.Image) {
            return "一张图片";
        } else if (content instanceof IMessage.Audio) {
            return "一段语音";
        } else if (content instanceof IMessage.GroupNotification) {
            return ((GroupNotification) content).description;
        } else if (content instanceof IMessage.Location) {
            return "一个地理位置";
        } else {
            return content.getRaw();
        }
    }

    void updateConversationDetail(Conversation conv) {
        String detail = messageContentToString(conv.message.content);
        conv.setDetail(detail);
    }

    void updatePeerConversationName(Conversation conv) {
        User u = getUser(conv.cid);
        if (TextUtils.isEmpty(u.name)) {
            conv.setName(u.identifier);
            final Conversation fconv = conv;
            asyncGetUser(conv.cid, new GetUserCallback() {
                @Override
                public void onUser(User u) {
                    fconv.setName(u.name);
                    fconv.setAvatar(u.avatarURL);
                }
            });
        } else {
            conv.setName(u.name);
        }
        conv.setAvatar(u.avatarURL);
    }

    void updateGroupConversationName(Conversation conv) {
        Group g = getGroup(conv.cid);
        if (TextUtils.isEmpty(g.name)) {
            conv.setName(g.identifier);
            final Conversation fconv = conv;
            asyncGetGroup(conv.cid, new GetGroupCallback() {
                @Override
                public void onGroup(Group g) {
                    fconv.setName(g.name);
                    fconv.setAvatar(g.avatarURL);
                }
            });
        } else {
            conv.setName(g.name);
        }
        conv.setAvatar(g.avatarURL);
    }

    void loadConversations() {
        conversations = new ArrayList<Conversation>();
        ConversationIterator iter = PeerMessageDB.getInstance().newConversationIterator();
        while (true) {
            Conversation conv = iter.next();
            if (conv == null) {
                break;
            }
            if (conv.message == null) {
                continue;
            }
            updatePeerConversationName(conv);
            updateConversationDetail(conv);
            conversations.add(conv);
        }

        iter = GroupMessageDB.getInstance().newConversationIterator();
        while (true) {
            Conversation conv = iter.next();
            if (conv == null) {
                break;
            }
            if (conv.message == null) {
                continue;
            }
            updateGroupConversationName(conv);
            updateNotificationDesc(conv);
            updateConversationDetail(conv);
            conversations.add(conv);
        }

        MessageIterator messageIterator  = CustomerMessageDB.getInstance().newMessageIterator(KEFU_ID);
        ICustomerMessage msg = null;
        while (messageIterator != null) {
            msg = (ICustomerMessage) messageIterator.next();
            if (msg == null) {
                break;
            }

            if (msg.content.getType() != IMessage.MessageType.MESSAGE_ATTACHMENT) {
                break;
            }
        }

        Comparator<Conversation> cmp = new Comparator<Conversation>() {
            public int compare(Conversation c1, Conversation c2) {
                if (c1.message.timestamp > c2.message.timestamp) {
                    return -1;
                } else if (c1.message.timestamp == c2.message.timestamp) {
                    return 0;
                } else {
                    return 1;
                }

            }
        };
        Collections.sort(conversations, cmp);
    }

    public static class User {
        public long uid;
        public String name;
        public String avatarURL;

        //name为nil时，界面显示identifier字段
        public String identifier;
    }

    public static class Group {
        public long gid;
        public String name;
        public String avatarURL;

        //name为nil时，界面显示identifier字段
        public String identifier;
    }



    public interface GetUserCallback {
        void onUser(User u);
    }

    public interface GetGroupCallback {
        void onGroup(Group g);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        Conversation conv = conversations.get(position);
        Log.i(TAG, "conv:" + conv.getName());

        if (conv.type == Conversation.CONVERSATION_PEER) {
            onPeerClick(conv.cid);
        } else if (conv.type == Conversation.CONVERSATION_GROUP){
            onGroupClick(conv.cid);
        }
    }

    @Override
    public void onConnectState(IMService.ConnectState state) {

    }
    @Override
    public void onLoginPoint(LoginPoint lp) {

    }
    @Override
    public void onPeerInputting(long uid) {

    }
    @Override
    public void onPeerMessage(IMMessage msg) {
        Log.i(TAG, "on peer message");
        IMessage imsg = new IMessage();
        imsg.timestamp = now();
        imsg.msgLocalID = msg.msgLocalID;
        imsg.sender = msg.sender;
        imsg.receiver = msg.receiver;
        imsg.setContent(msg.content);

        long cid = 0;
        if (msg.sender == this.currentUID) {
            cid = msg.receiver;
        } else {
            cid = msg.sender;
        }

        int pos = findConversationPosition(cid, Conversation.CONVERSATION_PEER);
        Conversation conversation = null;
        if (pos == -1) {
            conversation = newPeerConversation(cid);
        } else {
            conversation = conversations.get(pos);
        }

        conversation.message = imsg;
        updateConversationDetail(conversation);

        if (pos == -1) {
            conversations.add(0, conversation);
            adapter.notifyDataSetChanged();
        } else if (pos > 0) {
            conversations.remove(pos);
            conversations.add(0, conversation);
            adapter.notifyDataSetChanged();
        } else {
            //pos == 0
        }
    }

    public Conversation findConversation(long cid, int type) {
        for (int i = 0; i < conversations.size(); i++) {
            Conversation conv = conversations.get(i);
            if (conv.cid == cid && conv.type == type) {
                return conv;
            }
        }
        return null;
    }

    public int findConversationPosition(long cid, int type) {
        for (int i = 0; i < conversations.size(); i++) {
            Conversation conv = conversations.get(i);
            if (conv.cid == cid && conv.type == type) {
                return i;
            }
        }
        return -1;
    }

    public Conversation newPeerConversation(long cid) {
        Conversation conversation = new Conversation();
        conversation.type = Conversation.CONVERSATION_PEER;
        conversation.cid = cid;

        updatePeerConversationName(conversation);
        return conversation;
    }

    public Conversation newGroupConversation(long cid) {
        Conversation conversation = new Conversation();
        conversation.type = Conversation.CONVERSATION_GROUP;
        conversation.cid = cid;
        updateGroupConversationName(conversation);
        return conversation;
    }

    public static int now() {
        Date date = new Date();
        long t = date.getTime();
        return (int)(t/1000);
    }
    @Override
    public void onPeerMessageACK(int msgLocalID, long uid) {
        Log.i(TAG, "message ack on main");
    }

    @Override
    public void onPeerMessageFailure(int msgLocalID, long uid) {
    }

    @Override
    public void onGroupMessage(IMMessage msg) {
        Log.i(TAG, "on group message");
        IMessage imsg = new IMessage();
        imsg.timestamp = msg.timestamp;
        imsg.msgLocalID = msg.msgLocalID;
        imsg.sender = msg.sender;
        imsg.receiver = msg.receiver;
        imsg.setContent(msg.content);

        int pos = findConversationPosition(msg.receiver, Conversation.CONVERSATION_GROUP);
        Conversation conversation = null;
        if (pos == -1) {
            conversation = newGroupConversation(msg.receiver);
        } else {
            conversation = conversations.get(pos);
        }

        conversation.message = imsg;
        updateConversationDetail(conversation);

        if (pos == -1) {
            conversations.add(0, conversation);
            adapter.notifyDataSetChanged();
        } else if (pos > 0) {
            conversations.remove(pos);
            conversations.add(0, conversation);
            adapter.notifyDataSetChanged();
        } else {
            //pos == 0
        }
    }
    @Override
    public void onGroupMessageACK(int msgLocalID, long uid) {

    }

    @Override
    public void onGroupMessageFailure(int msgLocalID, long uid) {

    }

    @Override
    public void onGroupNotification(String text) {
        GroupNotification groupNotification = IMessage.newGroupNotification(text);
        IMessage imsg = new IMessage();
        imsg.sender = 0;
        imsg.receiver = groupNotification.groupID;
        imsg.timestamp = groupNotification.timestamp;
        imsg.setContent(groupNotification);
        int pos = findConversationPosition(groupNotification.groupID, Conversation.CONVERSATION_GROUP);
        Conversation conv = null;
        if (pos == -1) {
            conv = newGroupConversation(groupNotification.groupID);
        } else {
            conv = conversations.get(pos);
        }
        conv.message = imsg;
        updateNotificationDesc(conv);
        updateConversationDetail(conv);
        if (pos == -1) {
            conversations.add(0, conv);
            adapter.notifyDataSetChanged();
        } else if (pos > 0) {
            //swap with 0
            conversations.remove(pos);
            conversations.add(0, conv);
            adapter.notifyDataSetChanged();
        } else {
            //pos == 0
        }
    }

    private void updateNotificationDesc(Conversation conv) {
        final IMessage imsg = conv.message;
        if (imsg == null || imsg.content.getType() != IMessage.MessageType.MESSAGE_GROUP_NOTIFICATION) {
            return;
        }
        long currentUID = this.currentUID;
        GroupNotification notification = (GroupNotification)imsg.content;
        if (notification.notificationType == GroupNotification.NOTIFICATION_GROUP_CREATED) {
            if (notification.master == currentUID) {
                notification.description = String.format("您创建了\"%s\"群组", notification.groupName);
            } else {
                notification.description = String.format("您加入了\"%s\"群组", notification.groupName);
            }
        } else if (notification.notificationType == GroupNotification.NOTIFICATION_GROUP_DISBAND) {
            notification.description = "群组已解散";
        } else if (notification.notificationType == GroupNotification.NOTIFICATION_GROUP_MEMBER_ADDED) {
            User u = getUser(notification.member);
            if (TextUtils.isEmpty(u.name)) {
                notification.description = String.format("\"%s\"加入群", u.identifier);
                final GroupNotification fnotification = notification;
                final Conversation fconv = conv;
                asyncGetUser(notification.member, new GetUserCallback() {
                    @Override
                    public void onUser(User u) {
                        fnotification.description = String.format("\"%s\"加入群", u.name);
                        if (fconv.message == imsg) {
                            fconv.setDetail(fnotification.description);
                        }
                    }
                });
            } else {
                notification.description = String.format("\"%s\"加入群", u.name);
            }
        } else if (notification.notificationType == GroupNotification.NOTIFICATION_GROUP_MEMBER_LEAVED) {
            User u = getUser(notification.member);
            if (TextUtils.isEmpty(u.name)) {
                notification.description = String.format("\"%s\"离开群", u.identifier);
                final GroupNotification fnotification = notification;
                final Conversation fconv = conv;
                asyncGetUser(notification.member, new GetUserCallback() {
                    @Override
                    public void onUser(User u) {
                        fnotification.description = String.format("\"%s\"离开群", u.name);
                        if (fconv.message == imsg) {
                            fconv.setDetail(fnotification.description);
                        }
                    }
                });
            } else {
                notification.description = String.format("\"%s\"离开群", u.name);
            }
        }
    }

    @Override
    public void onNotification(Notification notification) {
        if (notification.name.equals(PeerMessageActivity.SEND_MESSAGE_NAME)) {
            IMessage imsg = (IMessage) notification.obj;

            int pos = findConversationPosition(imsg.receiver, Conversation.CONVERSATION_PEER);
            Conversation conversation = null;
            if (pos == -1) {
                conversation = newPeerConversation(imsg.receiver);
            } else {
                conversation = conversations.get(pos);
            }

            conversation.message = imsg;
            updateConversationDetail(conversation);

            if (pos == -1) {
                conversations.add(0, conversation);
                adapter.notifyDataSetChanged();
            } else if (pos > 0){
                conversations.remove(pos);
                conversations.add(0, conversation);
                adapter.notifyDataSetChanged();
            } else {
                //pos == 0
            }

        } else if (notification.name.equals(PeerMessageActivity.CLEAR_MESSAGES)) {
            Long peerUID = (Long)notification.obj;
            Conversation conversation = findConversation(peerUID, Conversation.CONVERSATION_PEER);
            if (conversation != null) {
                conversations.remove(conversation);
                adapter.notifyDataSetChanged();
            }
        } else if (notification.name.equals(GroupMessageActivity.SEND_MESSAGE_NAME)) {
            IMessage imsg = (IMessage) notification.obj;
            int pos = findConversationPosition(imsg.receiver, Conversation.CONVERSATION_GROUP);
            Conversation conversation = null;
            if (pos == -1) {
                conversation = newGroupConversation(imsg.receiver);
            } else {
                conversation = conversations.get(pos);
            }

            conversation.message = imsg;
            updateConversationDetail(conversation);

            if (pos == -1) {
                conversations.add(0, conversation);
                adapter.notifyDataSetChanged();
            } else if (pos > 0){
                conversations.remove(pos);
                conversations.add(0, conversation);
                adapter.notifyDataSetChanged();
            } else {
                //pos == 0
            }

        }  else if (notification.name.equals(GroupMessageActivity.CLEAR_MESSAGES)) {
            Long groupID = (Long)notification.obj;
            Conversation conversation = findConversation(groupID, Conversation.CONVERSATION_GROUP);
            if (conversation != null) {
                conversations.remove(conversation);
                adapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onSystemMessage(String sm) {
        Log.i(TAG, "system message:" + sm);
    }

    public boolean canBack() {
        return false;
    }





    protected User getUser(long uid) {
        User u = new User();
        u.uid = uid;
        u.name = null;
        u.avatarURL = "";
        u.identifier = String.format("%d", uid);
        return u;
    }


    protected void asyncGetUser(long uid, GetUserCallback cb) {
        final long fuid = uid;
        final GetUserCallback fcb = cb;
        new AsyncTask<Void, Integer, User>() {
            @Override
            protected User doInBackground(Void... urls) {
                User u = new User();
                u.uid = fuid;
                u.name = String.format("%d", fuid);
                u.avatarURL = "";
                u.identifier = String.format("%d", fuid);
                return u;
            }
            @Override
            protected void onPostExecute(User result) {
                fcb.onUser(result);
            }
        }.execute();
    }


    protected Group getGroup(long gid) {
        Group g = new Group();
        g.gid = gid;
        g.name = null;
        g.avatarURL = "";
        g.identifier = String.format("%d", gid);
        return g;
    }


    protected void asyncGetGroup(long gid, GetGroupCallback cb) {
        final long fgid = gid;
        final GetGroupCallback fcb = cb;
        new AsyncTask<Void, Integer, Group>() {
            @Override
            protected Group doInBackground(Void... urls) {
                Group g = new Group();
                g.gid = fgid;
                g.name = String.format("%d", fgid);
                g.avatarURL = "";
                g.identifier = String.format("%d", fgid);
                return g;
            }
            @Override
            protected void onPostExecute(Group result) {
                fcb.onGroup(result);
            }
        }.execute();
    }


    protected void onPeerClick(long uid) {
        User u = getUser(uid);

        Intent intent = new Intent(this, PeerMessageActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("peer_uid", uid);
        if (TextUtils.isEmpty(u.name)) {
            intent.putExtra("peer_name", u.identifier);
        } else {
            intent.putExtra("peer_name", u.name);
        }
        intent.putExtra("current_uid", this.currentUID);
        startActivity(intent);
    }


    protected void onGroupClick(long gid) {
        Intent intent = new Intent(this, AppGroupMessageActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("group_id", gid);
        intent.putExtra("group_name", "企业群");
        intent.putExtra("current_uid", this.currentUID);
        startActivity(intent);
    }

    /**
     * 创建企业群
     *
     */
    private void createGroup(){
        try{
            AsyncHttpClient client = new AsyncHttpClient();
            client.setTimeout(3000);
            client.addHeader("Content-Type","application/json; charset=UTF-8");
            client.addHeader("Authorization", "Bearer " + IMHttpAPI.accessToken);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("master",123);
            jsonObject.put("name","企业群");
            JSONArray jsonArray = new JSONArray();
            jsonArray.put(123);
            jsonArray.put(456);
            jsonObject.put("members",jsonArray);
            StringEntity stringEntity = new StringEntity(jsonObject.toString(),"UTF-8");
            String url = "http://api.gobelieve.io/groups";
            client.post(this,url, stringEntity, "application/json", new JsonHttpResponseHandler(){
                public void onStart(){
                    super.onStart();
                }

                public void onFinish(){
                    super.onFinish();
                }

                public void onSuccess(int statusCode, Header[] headers, JSONObject response){
                    super.onSuccess(statusCode,headers,response);
                    try {
                        int groupId  = response.getInt("group_id");
                        Log.i("info","groupId"+groupId);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse){
                    super.onFailure(statusCode,headers,throwable,errorResponse);
                }
            });
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
