package com.example.btl_novelworld.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

public class Comment {
    private String uid;
    private String userName;
    private String content;
    private Timestamp timestamp;

    // Constructor trống bắt buộc để Firebase.toObject hoạt động
    public Comment() {
    }

    public Comment(String uid, String userName, String content, Timestamp timestamp) {
        this.uid = uid;
        this.userName = userName;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    @PropertyName("userName") // Đảm bảo khớp với key trong Firestore
    public String getUserName() {
        return userName;
    }

    @PropertyName("userName")
    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}