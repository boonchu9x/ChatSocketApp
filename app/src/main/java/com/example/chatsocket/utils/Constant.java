package com.example.chatsocket.utils;

public class Constant {
    public static final String BASE_URL_SERVER = "http://192.168.1.37:3000/";

    public static String getTime(){
       return android.text.format.DateFormat.format("hh:mm a EEEE, dd MMMM", new java.util.Date()).toString();
    }
}
