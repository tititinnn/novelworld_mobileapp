package com.example.btl_novelworld.models;

import com.google.firebase.Timestamp;

public class LibraryItem {
    private String bookId;
    private String type; // history, favorite, saved
    private String lastReadChapterId;
    private Timestamp timestamp;

    public LibraryItem() {
    }

    public String getBookId() {
        return bookId;
    }

    public String getType() {
        return type;
    }

    public String getLastReadChapterId() {
        return lastReadChapterId;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }
}