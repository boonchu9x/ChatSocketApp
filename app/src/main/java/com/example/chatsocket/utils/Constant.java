package com.example.chatsocket.utils;

public class Constant {
    public static final String BASE_URL_SERVER = "https://chat-socket-realtime.herokuapp.com/";

    public static String getTime(){
       return android.text.format.DateFormat.format("hh:mm a EEEE, dd MMMM", new java.util.Date()).toString();
    }
}
