package com.gravo.gravoadmin;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.List;
import java.util.Map; // <-- IMPORT THIS
import com.google.firebase.firestore.DocumentId;

public class Product {
    @DocumentId
    private String name;
    private String description;
    private String brand;
    private String categoryId;
    private String sellerId;
    private double price;
    private double costPrice;
    private long discountPercent;
    private long stockQuantity;
    private List<String> imageUrls;
    private List<String> tags_lowercase;
    private boolean is_new;
    @ServerTimestamp
    private Date createdAt;

    // --- NEW FIELD FOR SPECIFICATIONS ---
    private Map<String, String> specifications;

    // Required empty public constructor for Firestore
    public Product() {}

    // Getters and Setters for all fields...
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public double getCostPrice() { return costPrice; }
    public void setCostPrice(double costPrice) { this.costPrice = costPrice; }
    public long getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(long discountPercent) { this.discountPercent = discountPercent; }
    public long getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(long stockQuantity) { this.stockQuantity = stockQuantity; }
    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    public List<String> getTags_lowercase() { return tags_lowercase; }
    public void setTags_lowercase(List<String> tags_lowercase) { this.tags_lowercase = tags_lowercase; }
    public boolean isIs_new() { return is_new; }
    public void setIs_new(boolean is_new) { this.is_new = is_new; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    // --- GETTER AND SETTER FOR SPECIFICATIONS ---
    public Map<String, String> getSpecifications() { return specifications; }
    public void setSpecifications(Map<String, String> specifications) { this.specifications = specifications; }
}