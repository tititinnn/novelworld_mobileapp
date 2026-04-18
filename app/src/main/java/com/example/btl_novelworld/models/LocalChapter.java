package com.example.btl_novelworld.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "offline_chapters")
public class LocalChapter {
    @PrimaryKey
    @NonNull
    public String chapterId;
    public String bookId; // Giúp phân biệt chương này của truyện nào
    public long chapterNumber;
    public String chapterTitle;
    public String content;

    // Constructor
    public LocalChapter(@NonNull String chapterId, String bookId, long chapterNumber, String chapterTitle, String content) {
        this.chapterId = chapterId;
        this.bookId = bookId;
        this.chapterNumber = chapterNumber;
        this.chapterTitle = chapterTitle;
        this.content = content;
    }
}