package com.example.chatsocket.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URLConnection;

public class ImageUtil {

    public static String getMimeType(byte[] imageInByte) {
        ByteArrayInputStream bis = new ByteArrayInputStream(imageInByte);
        InputStream is = new BufferedInputStream(bis);
        String mimeType = "";
        try {
            mimeType = URLConnection.guessContentTypeFromStream(is);
        } catch (Exception e) {
        }
        return mimeType;
    }

    // convert from bitmap to byte array
    public static byte[] getBytesFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);

        // get the base 64 string
//        String imgString = Base64.encodeToString(getBytesFromBitmap(someImg),
//                Base64.NO_WRAP);
        return stream.toByteArray();
    }

    public static Bitmap getBitmapFromByte(byte[] byteImage) {
        return BitmapFactory.decodeByteArray(byteImage, 0, byteImage.length);
    }

    public static String base64Encode(byte[] bytes)
    {
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

}
