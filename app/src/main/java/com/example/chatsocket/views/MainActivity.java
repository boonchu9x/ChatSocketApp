package com.example.chatsocket.views;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
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
import com.example.chatsocket.utils.ImageUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import pl.aprilapps.easyphotopicker.DefaultCallback;
import pl.aprilapps.easyphotopicker.EasyImage;
import pl.aprilapps.easyphotopicker.MediaFile;
import pl.aprilapps.easyphotopicker.MediaSource;

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


    private EasyImage easyImage;
    private byte[] byteImage = null;
    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 102;


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

        easyImage = new EasyImage.Builder(this)

                // Chooser only
                // Will appear as a system chooser title, DEFAULT empty string
                //.setChooserTitle("Pick media")
                // Will tell chooser that it should show documents or gallery apps
                //.setChooserType(ChooserType.CAMERA_AND_DOCUMENTS)  you can use this or the one below
                //.setChooserType(ChooserType.CAMERA_AND_GALLERY)

                // Setting to true will cause taken pictures to show up in the device gallery, DEFAULT false
                .setCopyImagesToPublicGalleryFolder(false)
                // Sets the name for images stored if setCopyImagesToPublicGalleryFolder = true
                .setFolderName("ChatAppSocket")
                // Allow multiple picking
                .allowMultiple(false)
                .build();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @OnClick({R.id.ln_send_message, R.id.ln_send_image, R.id.card_clear_image})
    void eventSend(View view) {
        if (view.getId() == R.id.ln_send_message) sendMessage();
        if (view.getId() == R.id.ln_send_image) actionGallery();
        if (view.getId() == R.id.card_clear_image) {
            lnShowImage.setVisibility(View.GONE);
            byteImage = null;
        }
    }

    private void actionGallery() {
        if (easyImage == null) return;
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
            }
        } else {
            easyImage.openGallery(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                easyImage.openGallery(this);
            } else {
                Toast.makeText(this, R.string.unable_top_open_gallery, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        easyImage.handleActivityResult(requestCode, resultCode, data, this, new DefaultCallback() {
            @Override
            public void onMediaFilesPicked(@NonNull MediaFile[] imageFiles, @NonNull MediaSource source) {
                File file = imageFiles[0].getFile();

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(file.getAbsolutePath(), options);
                int imageHeight = options.outHeight;
                int imageWidth = options.outWidth;

                String filePathColumn = file.getPath();
                Bitmap imageBitmap = BitmapFactory.decodeFile(filePathColumn);
                byteImage = ImageUtil.getBytesFromBitmap(imageBitmap);
                if (imageWidth > 150) {
                    float scale = imageWidth / 150;
                    Bitmap scaleBitmap = Bitmap.createScaledBitmap(imageBitmap, 150, (int) (imageHeight / scale), false);
                    imgShowImage.setImageBitmap(scaleBitmap);
                } else {
                    imgShowImage.setImageBitmap(imageBitmap);
                }
                lnShowImage.setVisibility(View.VISIBLE);
            }

            @Override
            public void onImagePickerError(@NonNull Throwable error, @NonNull MediaSource source) {
                //Some error handling
                error.printStackTrace();
            }

            @Override
            public void onCanceled(@NonNull MediaSource source) {
                //Not necessary to remove any files manually anymore
            }
        });
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

                if (!mTyping && count > 0) {
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

    private void addMessage(String username, String message, byte[] byteImage, int type) {

        listMessage.add(new Message.Builder(type)
                .username(username).byteImage(byteImage).message(message).build());
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
        if (TextUtils.isEmpty(message) && byteImage == null) {
            edtMessage.requestFocus();
            return;
        }



        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("message", message);
            jsonObject.put("images", byteImage);
            // perform the sending message attempt.
            //mSocket.emit("client-send-chat", message);
            mSocket.emit("client-send-chat", jsonObject);
            addMessage(mUserName, message, byteImage, 0);
            edtMessage.setText("");
            edtMessage.clearFocus();
            lnShowImage.setVisibility(View.GONE);
            byteImage = null;
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed send image", Toast.LENGTH_SHORT).show();
        }

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
                String username = "";
                String message = "";
                byte[] byteImage = null;
                try {
                    username = data.getString("username");
                    message = data.getString("message");
                    byteImage = Base64.decode(data.getString("send_img"),Base64.DEFAULT);
                    Toast.makeText(MainActivity.this, "" + byteImage.length, Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    return;
                }
                removeTyping(username);
                addMessage(username, message, byteImage, 1);
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
                if (pos > -1 && mAdapter.getItemViewType(pos) == Message.TYPE_ACTION) {
                    removeTyping(username);
                }
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
