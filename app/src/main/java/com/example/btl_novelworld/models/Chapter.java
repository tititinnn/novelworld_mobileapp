package com.example.btl_novelworld.models;

import com.google.firebase.Timestamp;

public class Chapter {
    private String chapterId;
    private long chapterNumber;
    private String chapterTitle;
    private String content;
    private Timestamp publishedDate;

    public Chapter() {
    }

    public String getChapterId() {
        return chapterId;
    }

    public void setChapterId(String chapterId) {
        this.chapterId = chapterId;
    }

    public long getChapterNumber() {
        return chapterNumber;
    }

    public String getChapterTitle() {
        return chapterTitle;
    }

    public String getContent() {
        return content;
    }

    public Timestamp getPublishedDate() {
        return publishedDate;
    }
}