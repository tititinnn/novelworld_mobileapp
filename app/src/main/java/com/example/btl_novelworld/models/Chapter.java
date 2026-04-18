package com.example.btl_novelworld.models;

import com.google.firebase.Timestamp; // Nhớ import thư viện này

public class Chapter {
    private String chapterId;
    private long chapterNumber;
    private String chapterTitle;
    private String content;

    // BIẾN VỪA ĐƯỢC KHÔI PHỤC
    private Timestamp publishedDate;

    // BẮT BUỘC: Constructor rỗng để Firebase tự động nạp dữ liệu
    public Chapter() {
    }

    // --- CÁC HÀM GETTER VÀ SETTER ---

    public String getChapterId() {
        return chapterId;
    }

    public void setChapterId(String chapterId) {
        this.chapterId = chapterId;
    }

    public long getChapterNumber() {
        return chapterNumber;
    }

    public void setChapterNumber(long chapterNumber) {
        this.chapterNumber = chapterNumber;
    }

    public String getChapterTitle() {
        return chapterTitle;
    }

    public void setChapterTitle(String chapterTitle) {
        this.chapterTitle = chapterTitle;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    // GETTER & SETTER CHO NGÀY ĐĂNG
    public Timestamp getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(Timestamp publishedDate) {
        this.publishedDate = publishedDate;
    }
}