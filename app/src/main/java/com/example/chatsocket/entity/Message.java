package com.example.chatsocket.entity;

public class Message {

    public static final int TYPE_MESSAGE_ME = 0;
    public static final int TYPE_MESSAGE_YOU = 1;
    public static final int TYPE_LOG = 2;
    public static final int TYPE_ACTION = 3;

    private int mType;
    private String mMessage;
    private String mUsername;
    private byte[] mByteImage;

    private Message() {}

    public int getType() {
        return mType;
    };

    public String getMessage() {
        return mMessage;
    };

    public String getUsername() {
        return mUsername;
    };

    public byte[] getByteImage(){
        return mByteImage;
    }


    public static class Builder {
        private final int mType;
        private String mUsername;
        private String mMessage;
        private byte[] byteImage;

        public Builder(int type) {
            mType = type;
        }

        public Builder byteImage(byte[] byteImage) {
            this.byteImage = byteImage;
            return this;
        }

        public Builder username(String username) {
            mUsername = username;
            return this;
        }

        public Builder message(String message) {
            mMessage = message;
            return this;
        }

        public Message build() {
            Message message = new Message();
            message.mType = mType;
            message.mUsername = mUsername;
            message.mByteImage = byteImage;
            message.mMessage = mMessage;
            return message;
        }
    }
}
