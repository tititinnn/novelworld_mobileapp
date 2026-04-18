package com.example.btl_novelworld.models;

public class Category {
    private String categoryId; // Sẽ lấy từ Document ID
    private String name;
    private int order;

    public Category() {}

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public String getName() { return name; }
    public int getOrder() { return order; }
}