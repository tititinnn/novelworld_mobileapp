package com.example.btl_novelworld.models;

import com.google.firebase.Timestamp;
import java.util.List;

public class Book {
    private String bookId;
    private String title;
    private String author;
    private String coverUrl;
    private String description;
    private List<String> categories;
    private String status;
    private long viewsCount;
    private long likesCount;
    private double rating;
    private long totalChapters;
    private Timestamp updatedAt;

    private int rankByWeek;
    private int rankByMonth;
    private String categoryNamesDisplay;

    public Book() {
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getCategories() {
        return categories;
    }

    public String getStatus() {
        return status;
    }

    public long getViewsCount() {
        return viewsCount;
    }

    public long getLikesCount() {
        return likesCount;
    }

    public double getRating() {
        return rating;
    }

    public long getTotalChapters() {
        return totalChapters;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public int getRankByWeek(){return rankByWeek;}
    public int getRankByMonth(){return rankByMonth;}

    public String getCategoryNamesDisplay() { return categoryNamesDisplay; }
    public void setCategoryNamesDisplay(String categoryNamesDisplay) { this.categoryNamesDisplay = categoryNamesDisplay; }
}