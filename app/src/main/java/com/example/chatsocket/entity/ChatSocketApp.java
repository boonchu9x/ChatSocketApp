package com.example.chatsocket.entity;

import android.annotation.SuppressLint;
import android.app.Application;

import com.example.chatsocket.utils.Constant;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;

@SuppressLint("Registered")
public class ChatSocketApp extends Application {

    private Socket mSocket;
    {
        try {
            mSocket = IO.socket(Constant.BASE_URL_SERVER);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Socket getSocket(){
        return mSocket;
    }
}
