package com.gravo.gravoadmin;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class Product {

    // --- FIX: Add a specific field for the ID ---
    // This will automatically hold the document ID when you read from Firestore.
    @DocumentId
    private String productId;

    // --- FIX: Removed @DocumentId from 'name' ---
    // Now this field will be saved into the document body as a normal string.
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

    private Map<String, String> specifications;

    // Required empty public constructor for Firestore
    public Product() {}

    // --- GETTERS AND SETTERS ---

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

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

    // Using 'isIs_new' follows standard Java naming for a boolean field named 'is_new'
    // This ensures Firestore maps it correctly.
    public boolean isIs_new() { return is_new; }
    public void setIs_new(boolean is_new) { this.is_new = is_new; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Map<String, String> getSpecifications() { return specifications; }
    public void setSpecifications(Map<String, String> specifications) { this.specifications = specifications; }
}