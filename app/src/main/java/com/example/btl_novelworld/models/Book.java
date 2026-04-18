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
    private long viewsCount; // Tổng lượt xem

    // --- HAI TRƯỜNG QUAN TRỌNG CẦN THÊM ---
    private long viewsWeek;  // Lượt xem trong tuần
    private long viewsMonth; // Lượt xem trong tháng
    // --------------------------------------

    private long likesCount;
    private double rating;
    private long totalChapters;
    private Timestamp updatedAt;

    // Giữ lại nếu bạn vẫn muốn dùng để hiển thị số thứ tự cứng,
    // nhưng thường sẽ dùng index của List sau khi sort theo views.
    private int rankByWeek;
    private int rankByMonth;

    private String categoryNamesDisplay;

    public Book() {
        // Constructor trống là bắt buộc cho Firebase
    }

    // Getter và Setter cho các trường mới
    public long getViewsWeek() {
        return viewsWeek;
    }

    public void setViewsWeek(long viewsWeek) {
        this.viewsWeek = viewsWeek;
    }

    public long getViewsMonth() {
        return viewsMonth;
    }

    public void setViewsMonth(long viewsMonth) {
        this.viewsMonth = viewsMonth;
    }

    // Các Getter/Setter cũ
    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getViewsCount() { return viewsCount; }
    public void setViewsCount(long viewsCount) { this.viewsCount = viewsCount; }

    public long getLikesCount() { return likesCount; }
    public void setLikesCount(long likesCount) { this.likesCount = likesCount; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public long getTotalChapters() { return totalChapters; }
    public void setTotalChapters(long totalChapters) { this.totalChapters = totalChapters; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public int getRankByWeek() { return rankByWeek; }
    public void setRankByWeek(int rankByWeek) { this.rankByWeek = rankByWeek; }

    public int getRankByMonth() { return rankByMonth; }
    public void setRankByMonth(int rankByMonth) { this.rankByMonth = rankByMonth; }

    public String getCategoryNamesDisplay() { return categoryNamesDisplay; }
    public void setCategoryNamesDisplay(String categoryNamesDisplay) { this.categoryNamesDisplay = categoryNamesDisplay; }
}