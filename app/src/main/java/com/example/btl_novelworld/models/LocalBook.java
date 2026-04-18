package com.example.btl_novelworld.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "offline_books")
public class LocalBook {
    @PrimaryKey
    @NonNull
    public String bookId;
    public String title;
    public String author;
    public String coverUrl;
    public String categoryNames;
    public int totalChapters; // THÊM DÒNG NÀY

    // Nhớ cập nhật lại Constructor
    public LocalBook(@NonNull String bookId, String title, String author, String coverUrl, String categoryNames, int totalChapters) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.coverUrl = coverUrl;
        this.categoryNames = categoryNames;
        this.totalChapters = totalChapters; // GÁN GIÁ TRỊ NÀY
    }
}