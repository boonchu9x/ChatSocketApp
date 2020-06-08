package com.example.chatsocket.views;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.chatsocket.R;
import com.example.chatsocket.entity.ChatSocketApp;
import com.example.chatsocket.entity.Message;
import com.example.chatsocket.entity.MessageAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.rv_message)
    RecyclerView rvMessage;
    @BindView(R.id.ln_show_image)
    LinearLayout lnShowImage;
    @BindView(R.id.img_show_image)
    ImageView imgShowImage;
    @BindView(R.id.card_clear_image)
    CardView cardClearImage;
    @BindView(R.id.edt_message)
    EditText edtMessage;
    @BindView(R.id.ln_send_image)
    LinearLayout lnSendImage;
    @BindView(R.id.ln_send_message)
    LinearLayout lnSendMessage;

    private Socket mSocket;
    private String mUserName = "";
    private MessageAdapter mAdapter;
    private List<Message> listMessage = new ArrayList<>();
    private boolean mTyping = false;
    private Handler mTypingHandler = new Handler();
    private Boolean isConnected = true;
    private static final int TYPING_TIMER_LENGTH = 600;

    private String count;
    private String online = "online";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        getData();
        initViews();

        ChatSocketApp app = (ChatSocketApp) getApplication();
        mSocket = app.getSocket();
        mSocket.emit("client-joined-chat", mUserName);
        mSocket.on(Socket.EVENT_CONNECT, onConnect);
        mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.on("client-joined-chat", onUserJoined);
        mSocket.on("client-send-chat", onNewMessage);
        mSocket.on("client-send-typing", onTyping);
        mSocket.on("client-stop-typing", onStopTyping);
        mSocket.on("user-left", onUserLeft);

        eventViews();
    }


    private void getData() {
        Intent intent = getIntent();
        mUserName = intent.getStringExtra("username");
    }

    private void initViews() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        if (toolbar != null) toolbar.setNavigationOnClickListener(view -> {
            onBackPressed();
        });

        edtMessage.requestFocus();

        mAdapter = new MessageAdapter(this, listMessage, new MessageAdapter.OnClickItem() {
            @Override
            public void onClick() {
                Toast.makeText(MainActivity.this, "Show Image", Toast.LENGTH_SHORT).show();
            }
        });
        rvMessage.setAdapter(mAdapter);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @OnClick({R.id.ln_send_message, R.id.ln_send_image})
    void eventSend(View view) {
        if (view.getId() == R.id.ln_send_message) sendMessage();
        if (view.getId() == R.id.ln_send_image) {
        }
    }

    private void eventViews() {
        edtMessage.setOnEditorActionListener((v, id, event) -> {
            if (id == R.id.edt_message || id == EditorInfo.IME_NULL) {
                sendMessage();
                return true;
            }
            return false;
        });
        edtMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (null == mUserName) return;
                if (!mSocket.connected()) return;

                if (!mTyping) {
                    mTyping = true;
                    mSocket.emit("client-send-typing");
                }

                mTypingHandler.removeCallbacks(onTypingTimeout);
                mTypingHandler.postDelayed(onTypingTimeout, TYPING_TIMER_LENGTH);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });


    }

    private void addLog(String message) {
        listMessage.add(new Message.Builder(Message.TYPE_LOG)
                .message(message).build());
        mAdapter.notifyItemInserted(listMessage.size() - 1);
        scrollToBottom();
    }

    private String countUserOnline(int user) {
        return "<font color='#62CB00'>⦿ </font>" + "<font>" + getResources().getQuantityString(R.plurals.user_online, user, user) + "</font>";
    }

    private void addMessage(String username, String message, int type) {

        listMessage.add(new Message.Builder(type)
                .username(username).message(message).build());
        mAdapter.notifyItemInserted(listMessage.size() - 1);
        scrollToBottom();
    }

    private void addTyping(String username) {
        listMessage.add(new Message.Builder(Message.TYPE_ACTION)
                .username(username).build());
        mAdapter.notifyItemInserted(listMessage.size() - 1);
        scrollToBottom();
    }

    private void removeTyping(String username) {
        for (int i = listMessage.size() - 1; i >= 0; i--) {
            Message message = listMessage.get(i);
            if (message.getType() == Message.TYPE_ACTION && message.getUsername().equals(username)) {
                listMessage.remove(i);
                mAdapter.notifyItemRemoved(i);
            }

        }
    }

    private void sendMessage() {
        if (null == mUserName) return;
        if (!mSocket.connected()) return;
        mTyping = false;
        String message = edtMessage.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            edtMessage.requestFocus();
            return;
        }
        edtMessage.setText("");
        addMessage(mUserName, message, 0);

        // perform the sending message attempt.
        mSocket.emit("client-send-chat", message);
    }


    private void scrollToBottom() {
        rvMessage.scrollToPosition(mAdapter.getItemCount() - 1);
    }

    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!isConnected) {
                        if (null != mUserName)
                            mSocket.emit("client-joined-chat", mUserName);
                        isConnected = true;
                    }
                }
            });
        }
    };

    private Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isConnected) {
                        isConnected = false;
                        Toast.makeText(MainActivity.this, R.string.disconnect, Toast.LENGTH_LONG).show();
                    }

                }
            });
        }
    };

    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this,
                            R.string.error_connect, Toast.LENGTH_LONG).show();
                }
            });
        }
    };

    private Emitter.Listener onUserJoined = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    int numUsers;
                    try {
                        username = data.getString("username");
                        numUsers = data.getInt("number");
                    } catch (JSONException e) {
                        return;
                    }
                    addLog(getResources().getString(R.string.message_user_joined, username));
                    toolbar.setSubtitle(Html.fromHtml(countUserOnline(numUsers)));
                }
            });
        }
    };

    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(() -> {
                JSONObject data = (JSONObject) args[0];
                String username;
                String message;
                try {
                    username = data.getString("username");
                    message = data.getString("message");
                } catch (JSONException e) {
                    return;
                }
                removeTyping(username);
                addMessage(username, message, 1);
            });
        }
    };

    private Emitter.Listener onTyping = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(() -> {
                JSONObject data = (JSONObject) args[0];
                String username;
                try {
                    username = data.getString("username");
                } catch (JSONException e) {
                    return;
                }
                int pos = listMessage.size() - 1;
               /* if (pos > -1 && mAdapter.getItemViewType(pos) == Message.TYPE_ACTION) {
                    removeTyping(username);
                }*/
                if (pos > -1 && mAdapter.getItemViewType(pos) != Message.TYPE_ACTION)
                    addTyping(username);

            });
        }
    };

    private Emitter.Listener onStopTyping = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(() -> {
                JSONObject data = (JSONObject) args[0];
                String username;
                try {
                    username = data.getString("username");
                } catch (JSONException e) {
                    return;
                }
                removeTyping(username);
            });
        }
    };

    private Emitter.Listener onUserLeft = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(() -> {
                JSONObject data = (JSONObject) args[0];
                String username;
                int numUsers;
                try {
                    username = data.getString("usernameleft");
                    numUsers = data.getInt("number");
                } catch (JSONException e) {
                    return;
                }

                addLog(getResources().getString(R.string.message_user_left, username));
                toolbar.setSubtitle(Html.fromHtml(countUserOnline(numUsers)));
                removeTyping(username);
            });
        }
    };

    private Runnable onTypingTimeout = new Runnable() {
        @Override
        public void run() {
            if (!mTyping) return;

            mTyping = false;
            mSocket.emit("client-stop-typing");
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSocket.disconnect();
        mSocket.off(Socket.EVENT_CONNECT, onConnect);
        mSocket.off(Socket.EVENT_DISCONNECT, onDisconnect);
        mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.off(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.off("server-send-user-joined", onUserJoined);
        mSocket.off("server-send-chat", onNewMessage);
        mSocket.off("server-send-typing", onTyping);
        mSocket.off("server-send-stop-typing", onStopTyping);
        mSocket.off("server-send-user-left", onUserLeft);

    }
}
