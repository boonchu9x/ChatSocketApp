package com.example.chatsocket.database;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

public class PrefManager {

    private Context mContext;
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private static final String PREF_NAME = "PREF_NAME";
    private static final String IS_FIRST_LUNCH = "IS_FIRST_LUNCH";
    private static final String SAVE_USER_NAME = "SAVE_USER_NAME";


    @SuppressLint("CommitPrefEdits")
    public PrefManager(Context context) {
        int PRIVATE_MODE = 0;
        pref = context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
        this.mContext = context;
    }

    public void setIsFirstLunch(boolean isFirstLunch) {
        editor.putBoolean(IS_FIRST_LUNCH, isFirstLunch);
        editor.commit();
    }
    public boolean isFirstLunch() {
        return pref.getBoolean(IS_FIRST_LUNCH, true);
    }


    public void saveUserName(String userName) {
        editor.putString(SAVE_USER_NAME, userName);
        editor.commit();
    }

    public String getUserName(){
        return pref.getString(SAVE_USER_NAME, "");
    }

}
