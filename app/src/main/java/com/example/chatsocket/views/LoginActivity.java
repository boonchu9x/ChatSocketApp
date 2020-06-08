package com.example.chatsocket.views;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.example.chatsocket.R;
import com.example.chatsocket.database.PrefManager;
import com.example.chatsocket.entity.ChatSocketApp;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

import androidx.appcompat.app.AppCompatActivity;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class LoginActivity extends AppCompatActivity {

    @BindView(R.id.txt_layout_user_name)
    TextInputLayout txtLayoutUser;
    @BindView(R.id.txt_layout_password)
    TextInputLayout txtLayoutPass;
    @BindView(R.id.edt_user)
    TextInputEditText edtUser;
    @BindView(R.id.edt_pass)
    TextInputEditText edtPass;
    @BindView(R.id.btn_login)
    Button btnLogin;

    private String mUserName = "";
    private Socket mSocket;
    private PrefManager prefManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        //init socket
        init();
        prefManager = new PrefManager(this);
        if (!prefManager.isFirstLunch()){
            mUserName = prefManager.getUserName();
            mSocket.emit("login", mUserName);
            loginSuccess(mUserName);
        }
        mSocket.on("signin", login);

    }

    private void init(){
        ChatSocketApp app = (ChatSocketApp) getApplication();
        mSocket = app.getSocket();
        mSocket.connect();

    }

    @OnClick(R.id.btn_login)
    void login(View view){
        mUserName = Objects.requireNonNull(edtUser.getText()).toString().trim();
        String pass = Objects.requireNonNull(edtPass.getText()).toString().trim();
        if(TextUtils.isEmpty(mUserName))
        {
            YoYo.with(Techniques.Shake)
                    .duration(700)
                    .repeat(1)
                    .playOn(this.findViewById(R.id.txt_layout_user_name));
            return;
        }

        if(TextUtils.isEmpty(pass))
        {
            YoYo.with(Techniques.Shake)
                    .duration(700)
                    .repeat(1)
                    .playOn(this.findViewById(R.id.txt_layout_password));
            return;
        }

        mSocket.emit("client-register", mUserName);

    }

    private Socket.Listener login = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(() -> {

                JSONObject object = (JSONObject) args[0];
                try {
                    //result true/false;
                    boolean exits = object.getBoolean("register");

                    //kiểm tra đã tồn tại hay chưa
                    if (!exits) {
                        Toast.makeText(LoginActivity.this, getString(R.string.account_already_exists), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(LoginActivity.this, getString(R.string.sign_up_success), Toast.LENGTH_SHORT).show();


                        prefManager.setIsFirstLunch(false);
                        prefManager.saveUserName(mUserName);
                        loginSuccess(mUserName);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            });
        }
    };

    private void loginSuccess(String userName){
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("username", mUserName);
        startActivity(intent);
        overridePendingTransition(R.anim.right_in, R.anim.left_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
